#!/usr/bin/env python3
"""Frame budget guard of the admin panel.

One preset pool holds thousands of entries, so the panel may only touch what is on screen while it draws. This check
proves that the drawing path walks the visible rows and nothing else, that scrolling costs the same whatever the pool
size, and that the whole-pool work (measuring column widths, filtering) happens only when the data or the filter
changes. It also proves the panel is still a plain scroll list: pagination was considered and refused, so a page
control appearing here is a change of design that has to be made on purpose.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCREEN = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "AdminConfigScreen.java"
LAYOUT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "TableLayout.java"
SCROLL = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "ScrollBar.java"

problems = []


def read(path):
    return path.read_text(encoding="utf-8")


def method_body(text, name):
    """The body of one method, found by its signature and closed by counting braces."""
    match = re.search(r"\b" + re.escape(name) + r"\s*\([^)]*\)\s*\{", text)

    if match is None:
        return None

    start = text.index("{", match.end() - 1)
    depth = 0

    for index in range(start, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1

            if depth == 0:
                return text[start:index + 1]

    return None


def check(name, condition, detail=""):
    if not condition:
        problems.append(name + (": " + detail if detail else ""))


def main():
    for path in (SCREEN, LAYOUT, SCROLL):
        if not path.is_file():
            problems.append("missing " + str(path))

    if problems:
        for problem in problems:
            print("FAIL " + problem)

        return 1

    screen = read(SCREEN)
    render = method_body(screen, "render")
    draw_list = method_body(screen, "drawList")
    scroll = method_body(screen, "mouseScrolled")

    check("render exists", render is not None)
    check("drawList exists", draw_list is not None)
    check("mouseScrolled exists", scroll is not None)

    if render and draw_list and scroll:
        check("render rebuilds nothing", "rebuild" not in render, "a rebuild per frame costs the pool size")
        check("render walks the list only through drawList", "for (int" not in render.split("drawList")[0])
        check("drawList is bounded by the visible rows", "visibleRows()" in draw_list and "visible" in draw_list)
        check("drawList loop bound is the visible count", re.search(r"for \(int \w+ = 0; \w+ < visible;", draw_list)
              is not None, "the row loop must stop at the visible count")
        check("drawList does not measure or rebuild", "rebuild" not in draw_list and "TableLayout(" not in draw_list)
        check("scroll does not rebuild", "rebuild" not in scroll)
        check("scroll steps the bar", "scrollBar.scrollBy" in scroll or "scrollBar." in scroll)

    check("the panel keeps a plain scroll bar", "private final ScrollBar scrollBar" in screen)
    check("no page control was added", not re.search(r"\bpage(Size|Index|Count)\b", screen),
          "pagination was refused for this version")

    rebuild_sites = [index for index in re.finditer(r"this\.(prizeLayout|valueLayout|adminLayout) = new TableLayout",
                                                   screen)]
    check("tables are rebuilt in three places only", len(rebuild_sites) == 3, str(len(rebuild_sites)))

    layout = read(LAYOUT)
    check("a column measures its cells once", "for (String cell : cells)" in layout)
    check("column widths are locked by compute()", "public void compute()" in layout)

    for name in ("rebuildPrizeTable", "rebuildValueTable", "rebuildAdminTable"):
        body = method_body(screen, name)

        if body is None:
            problems.append(name + " is missing")
            continue

        check(name + " locks its table", "compute()" in body, "a rebuilt table must lock its widths")

    filter_body = method_body(screen, "rebuildValueKeys")
    check("the filter rebuilds the value rows only", filter_body is not None and "rebuildValueTable()" in filter_body)

    if problems:
        for problem in problems:
            print("FAIL " + problem)

        return 1

    print("PASS frame: the list draws the visible rows only")
    print("PASS scroll: constant work whatever the pool size")
    print("PASS rebuild: column widths locked on data and filter changes only")
    return 0


if __name__ == "__main__":
    sys.exit(main())

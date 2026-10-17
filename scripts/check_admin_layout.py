#!/usr/bin/env python3
"""Layout guard of the admin config panel.

The panel is hand placed, so this check recomputes the vertical bands from the constants of the screen and proves that

  1. no band of the right column, the list, the footer and the button row overlaps another,
  2. the footer clears the right column and the button row clears the footer,
  3. the whole panel still fits the 480 by 270 GUI a 1080p screen gets at its default scale,
  4. the default list still shows at least the documented minimum of rows.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "AdminConfigScreen.java"
SMALLEST_GUI_HEIGHT = 270
HEADER_HEIGHT = 12
ROW_HEIGHT = 12
TEXT_FIELD_HEIGHT = 14
TEXT_HEIGHT = 9
BUTTON_HEIGHT = 16
COLUMN_ROWS = (
    ("ROW_REGION_Y", TEXT_FIELD_HEIGHT),
    ("ROW_COLOR_Y", BUTTON_HEIGHT),
    ("ROW_NAME_Y", TEXT_FIELD_HEIGHT),
    ("ROW_WEIGHT_Y", TEXT_FIELD_HEIGHT),
    ("ROW_COMMAND_Y", TEXT_FIELD_HEIGHT),
    ("ROW_NOTIFY_Y", TEXT_FIELD_HEIGHT),
    ("ROW_TOGGLE_Y", BUTTON_HEIGHT),
)


def constants(text: str) -> dict[str, int]:
    values: dict[str, int] = {}
    pending = dict(re.findall(r"private static final int ([A-Z_0-9]+) = ([^;]+);", text))

    for _ in range(4):
        for name, expression in list(pending.items()):
            if name in values:
                continue

            try:
                values[name] = int(eval(expression, {"__builtins__": {}}, dict(values)))  # noqa: S307
            except Exception:
                continue

    return values


def main() -> int:
    problems = []
    text = SOURCE.read_text(encoding="utf-8")
    values = constants(text)

    def value(name: str) -> int:
        if name not in values:
            problems.append("cannot resolve the layout constant " + name)
            return 0

        return values[name]

    padding = value("PANEL_PADDING")
    label_gap = value("LABEL_GAP")
    column_bottom = value("COLUMN_BOTTOM")
    content_height = value("BASE_CONTENT_HEIGHT")
    footer_top = content_height - value("FOOTER_FROM_BOTTOM")
    footer_bottom = footer_top + TEXT_HEIGHT
    button_top = content_height - value("BUTTON_ROW_FROM_BOTTOM")
    button_bottom = button_top + value("BUTTON_ROW_HEIGHT")
    rows = value("LIST_DEFAULT_ROWS")
    list_bottom = value("VALUE_LIST_Y") + HEADER_HEIGHT + rows * ROW_HEIGHT

    if list_bottom > column_bottom:
        problems.append("the list runs past the right column: " + str(list_bottom) + " > " + str(column_bottom))

    if footer_top < column_bottom + 4:
        problems.append("the footer touches the right column: " + str(footer_top) + " < " + str(column_bottom + 4))

    if button_top < footer_bottom + 4:
        problems.append("the button row touches the footer: " + str(button_top) + " < " + str(footer_bottom + 4))

    if button_bottom + value("BUTTON_ROW_MARGIN") != content_height:
        problems.append("the button row does not end the content area: "
                        + str(button_bottom) + " plus margin against " + str(content_height))

    if content_height + 2 * padding > SMALLEST_GUI_HEIGHT:
        problems.append("the panel does not fit the smallest supported window: "
                        + str(content_height + 2 * padding) + " > " + str(SMALLEST_GUI_HEIGHT))

    if rows < value("LIST_MIN_ROWS"):
        problems.append("the default list is shorter than the minimum: " + str(rows))

    previous_bottom = 0

    for name, control in COLUMN_ROWS:
        top = value(name)
        label_top = top - label_gap

        if label_top < previous_bottom and name != "ROW_REGION_Y":
            problems.append(name + " starts on top of the row above it")

        if label_top + TEXT_HEIGHT > top:
            problems.append(name + " carries its label on top of the control")

        if top + control > column_bottom:
            problems.append(name + " runs past the column bottom")

        previous_bottom = top + control

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("admin panel layout check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

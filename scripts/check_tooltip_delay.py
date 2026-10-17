#!/usr/bin/env python3
"""Tooltip delay guard of Devil Bone Gambler.

Checks that every tooltip the mod draws itself goes through the shared delayed tooltip, so no screen of the mod shows
a tooltip the moment the pointer reaches a cell.

  1. the immediate tooltip drawing of the vanilla gui graphics appears in the shared delayed tooltip only,
  2. every screen that owns a delayed tooltip really draws through it.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
GUI_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui"
DELAYED = GUI_ROOT / "DelayedTooltip.java"
IMMEDIATE = re.compile(r'\bgraphics\.render(?:Component)?Tooltip\s*\(')
OWNER = re.compile(r'private\s+(?:final\s+)?DelayedTooltip\s+([A-Za-z0-9_]+)')
USES = re.compile(r'this\.([A-Za-z0-9_]+)\.render\(')


def main() -> int:
    problems = []

    if not DELAYED.is_file():
        print("missing the shared delayed tooltip: " + str(DELAYED))
        return 1

    delayed_text = DELAYED.read_text(encoding="utf-8")

    if not IMMEDIATE.search(delayed_text):
        problems.append("the shared delayed tooltip does not draw with the gui graphics")

    for path in sorted(GUI_ROOT.glob("*.java")):
        text = path.read_text(encoding="utf-8")

        if path != DELAYED and IMMEDIATE.search(text):
            problems.append("draws a tooltip without the shared delay: " + path.name)

        for owner in OWNER.findall(text):
            if not USES.search(text):
                problems.append("owns a delayed tooltip but never draws through it: " + path.name)
            break

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("tooltip delay check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

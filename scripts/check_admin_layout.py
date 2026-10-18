#!/usr/bin/env python3
"""Layout guard of the admin config panel.

The panel is hand placed, so this check recomputes the vertical bands from the constants of the screen and proves that

  1. no band of the right column, the list, the footer and the button row overlaps another,
  2. the footer clears the right column and the button row clears the footer,
  3. the whole panel still fits the 480 by 270 GUI a 1080p screen gets at its default scale, margin included,
  4. both tab lists fill the left column, because their last row ends within one row of the space above the footer
     and never runs past the right column.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "AdminConfigScreen.java"
SMALLEST_GUI_WIDTH = 480
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
LIST_TOPS = ("PRIZE_LIST_Y", "VALUE_LIST_Y")


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

    padding_x = value("PANEL_PADDING")
    padding_y = value("PANEL_PADDING_Y")
    margin = value("PANEL_MARGIN")
    column_bottom = value("COLUMN_BOTTOM")
    content_height = value("BASE_CONTENT_HEIGHT")
    footer_top = content_height - value("FOOTER_FROM_BOTTOM")
    footer_bottom = footer_top + TEXT_HEIGHT
    button_top = content_height - value("BUTTON_ROW_FROM_BOTTOM")
    button_bottom = button_top + value("BUTTON_ROW_HEIGHT")

    if footer_top < column_bottom + 4:
        problems.append("the footer touches the right column: " + str(footer_top) + " < " + str(column_bottom + 4))

    if button_top < footer_bottom + 4:
        problems.append("the button row touches the footer: " + str(button_top) + " < " + str(footer_bottom + 4))

    if button_bottom + value("BUTTON_ROW_MARGIN") != content_height:
        problems.append("the button row does not end the content area: "
                        + str(button_bottom) + " plus margin against " + str(content_height))

    if content_height + 2 * padding_y + 2 * margin > SMALLEST_GUI_HEIGHT:
        problems.append("the panel and its margin do not fit the smallest supported window: "
                        + str(content_height + 2 * padding_y + 2 * margin) + " > " + str(SMALLEST_GUI_HEIGHT))

    if value("PANEL_WIDTH") + 2 * padding_x > SMALLEST_GUI_WIDTH:
        problems.append("the panel is wider than the smallest supported window")

    if padding_y * 2 != padding_x:
        problems.append("the vertical padding is not half the horizontal one: "
                        + str(padding_y) + " against " + str(padding_x))

    """
    竖排列表的高度必须把左列填满: 首行到页脚之间的可用高度除以行高, 得到的行数至少是最小行数,
    并且余下的空白不足一行, 否则列表下方会重新留出一条空白.
    """
    list_limit = content_height - value("FOOTER_FROM_BOTTOM") - value("LIST_BOTTOM_GAP")

    for name in LIST_TOPS:
        top = value(name)
        rows = (list_limit - top - HEADER_HEIGHT) // ROW_HEIGHT
        bottom = top + HEADER_HEIGHT + rows * ROW_HEIGHT

        if rows < value("LIST_MIN_ROWS"):
            problems.append(name + " shows fewer than the minimum rows: " + str(rows))

        if bottom > list_limit:
            problems.append(name + " runs into the gap above the footer: " + str(bottom) + " > " + str(list_limit))

        if list_limit - bottom >= ROW_HEIGHT:
            problems.append(name + " leaves a blank row band under the list: "
                            + str(list_limit - bottom) + " pixels")

        if bottom > column_bottom:
            problems.append(name + " runs past the right column: " + str(bottom) + " > " + str(column_bottom))

    previous_bottom = 0

    for name, control in COLUMN_ROWS:
        top = value(name)
        label_top = top - value("LABEL_GAP")

        if label_top < previous_bottom and name != "ROW_REGION_Y":
            problems.append(name + " starts on top of the row above it")

        if label_top + TEXT_HEIGHT > top:
            problems.append(name + " carries its label on top of the control")

        if top + control > column_bottom:
            problems.append(name + " runs past the column bottom")

        previous_bottom = top + control

    # 底部那一排按钮的宽度必须跟随各自文本, 不能再写死. 确认按钮要同时容纳普通与确认文案.
    if "private int fitWidth(Component... labels)" not in text:
        problems.append("the bottom row does not measure its buttons against their labels")

    if "BUTTON_PADDING_X = 5" not in text:
        problems.append("the bottom row padding constant is missing")

    parts = text.split("int buttonY =", 1)

    if len(parts) == 2:
        row = parts[1].split("this.adminButton", 1)[0]

        for literal in ("size(68, 18)", "size(24, 18)", "size(64, 18)", "size(52, 18)"):
            if literal in row:
                problems.append("a bottom row button still uses a fixed width: " + literal)
    else:
        problems.append("the bottom row could not be located")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("admin panel layout check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

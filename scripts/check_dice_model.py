"""
Checks both dice item models: the cube geometry, the per face texture mapping and the pip count of every face.
Exit code 0 means every face maps onto the cell that carries its own number, the six faces cover 1 to 6 exactly
once, opposite faces sum to seven, the three faces the inventory looks at are 4, 5 and 6, each cell really shows
that many pips, and every face covers the whole height of its cell in texture space, so no face is cropped or
stretched.
"""

import json
import pathlib
import sys
from PIL import Image

FACE = 12
SHEET_WIDTH = FACE * 6
SLOTS = (2, 5, 8)

LAYOUTS = {
    1: ((1, 1),),
    2: ((0, 0), (2, 2)),
    3: ((0, 0), (1, 1), (2, 2)),
    4: ((0, 0), (2, 0), (0, 2), (2, 2)),
    5: ((0, 0), (2, 0), (1, 1), (0, 2), (2, 2)),
    6: ((0, 0), (2, 0), (0, 1), (2, 1), (0, 2), (2, 2)),
}

# 面 -> 该面应当显示的点数, 对面之和为 7; 物品栏的正视角同时看到 north, east 与 up, 这三面是 4, 5, 6
FACES = {
    "north": 6,
    "south": 1,
    "east": 5,
    "west": 2,
    "up": 4,
    "down": 3,
}
GUI_FACES = ("north", "east", "up")
GUI_NUMBERS = (4, 5, 6)
# 模型 uv 的单位是纹理空间: 0 到 16 覆盖整张贴图, 与贴图的实际像素高度无关
UV_MAX = 16.0
OPPOSITE = (("north", "south"), ("east", "west"), ("up", "down"))
MARGIN = 0.35


def occupied_slots(face):
    pixels = face.load()
    body = pixels[4, 4]
    occupied = []

    for row in range(3):
        for column in range(3):
            x0, y0 = SLOTS[column], SLOTS[row]
            block = tuple(pixels[x, y] for y in range(y0, y0 + 2) for x in range(x0, x0 + 2))

            if not all(pixel == body for pixel in block):
                occupied.append((column, row))

    return occupied, body


def number_of(occupied):
    spot = set(occupied)

    for number, layout in LAYOUTS.items():
        if spot == set(layout):
            return number

    return None


def check_sheet(path):
    if not path.exists():
        return ["missing texture " + str(path)]

    image = Image.open(path).convert("RGBA")
    problems = []

    if image.size != (SHEET_WIDTH, FACE):
        return ["%s is %dx%d, expected %dx%d" % (path.name, image.size[0], image.size[1], SHEET_WIDTH, FACE)]

    patterns = set()
    numbers = []

    for index in range(6):
        cell = image.crop((index * FACE, 0, (index + 1) * FACE, FACE))
        occupied, _ = occupied_slots(cell)
        number = number_of(occupied)
        numbers.append(number)

        if number != index + 1:
            problems.append("%s cell %d shows %s pips, expected %d" % (path.name, index + 1, number, index + 1))

        for column, row in occupied:
            x0, y0 = SLOTS[column], SLOTS[row]
            patterns.add(tuple(cell.load()[x, y] for y in range(y0, y0 + 2) for x in range(x0, x0 + 2)))

    if len(patterns) != 1:
        problems.append("%s uses %d different pip patterns" % (path.name, len(patterns)))

    if sorted(n for n in numbers if n is not None) != [1, 2, 3, 4, 5, 6]:
        problems.append("%s does not cover 1 to 6 exactly once: %s" % (path.name, numbers))

    print("  %s %dx%d cells=%s pip=%s" % (path.name, image.size[0], image.size[1], numbers,
            patterns.pop() if patterns else "-"))
    return problems


def check_model(path, texture_path):
    if not path.exists():
        return ["missing model " + str(path)]

    model = json.loads(path.read_text(encoding="utf-8"))
    problems = []
    elements = model.get("elements") or []

    if len(elements) != 1:
        return ["%s has %d elements, expected one cube" % (path.name, len(elements))]

    element = elements[0]

    if element.get("from") != [2, 2, 2] or element.get("to") != [14, 14, 14]:
        problems.append("%s cube is %s..%s, expected 2..14 so the six 12 pixel faces stay square"
                % (path.name, element.get("from"), element.get("to")))

    textures = model.get("textures") or {}
    reference = textures.get("dice", "")
    expected = "devil_bone_gambler:item/" + texture_path.stem

    if reference != expected:
        problems.append("%s dice texture is %s, expected %s" % (path.name, reference, expected))

    faces = element.get("faces") or {}
    seen = {}

    for name, number in FACES.items():
        face = faces.get(name)

        if face is None:
            problems.append("%s has no %s face" % (path.name, name))
            continue

        if face.get("texture") != "#dice":
            problems.append("%s %s face uses texture %s" % (path.name, name, face.get("texture")))

        uv = face.get("uv") or []

        if len(uv) != 4:
            problems.append("%s %s face has uv %s" % (path.name, name, uv))
            continue

        left = uv[0] / 16.0 * SHEET_WIDTH
        right = uv[2] / 16.0 * SHEET_WIDTH
        cell = (number - 1) * FACE

        if abs(left - cell) > MARGIN or abs(right - (cell + FACE)) > MARGIN:
            problems.append("%s %s face covers pixels %.2f..%.2f, expected cell %d at %d..%d"
                    % (path.name, name, left, right, number, cell, cell + FACE))

        if abs(uv[1]) > MARGIN or abs(uv[3] - UV_MAX) > MARGIN:
            problems.append("%s %s face vertical range %s should cover the whole texture (uv 0..%g), not the pixel height"
                    % (path.name, name, uv[1:4:2], UV_MAX))

        seen[name] = number

    shown = [seen[name] for name in GUI_FACES if name in seen]

    if sorted(shown) != list(GUI_NUMBERS):
        problems.append("%s shows %s in the inventory, expected the 4, 5 and 6 side" % (path.name, shown))

    for first, second in OPPOSITE:
        if first in seen and second in seen and seen[first] + seen[second] != 7:
            problems.append("%s %s shows %d and %s shows %d, opposite faces must sum to seven"
                    % (path.name, first, seen[first], second, seen[second]))

    print("  %s cube=%s..%s faces=%s" % (path.name, element.get("from"), element.get("to"),
            ", ".join("%s=%d" % (name, seen[name]) for name in FACES if name in seen)))
    return problems


def main():
    root = pathlib.Path("src/main/resources/assets/devil_bone_gambler")
    problems = []

    for die in ("devils_dice", "gamblers_dice"):
        print(die)
        texture = root / "textures/item" / (die + ".png")
        model = root / "models/item" / (die + ".json")
        problems += check_sheet(texture)
        problems += check_model(model, texture)
        print()

    if problems:
        print("FAILED")

        for problem in problems:
            print("  " + problem)

        return 1

    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())

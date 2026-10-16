"""
Rebuilds the six faces of a dice item texture from the face art that is already there.

The source may be the original 16x16 icon, whose 12x12 face sits at (2, 2), or an already built 72x12 sheet.
Six 12x12 cells are written side by side, so a cube model can map one cell per face and every texel keeps its
original position and colour.

Running it twice is safe: with a sheet as the source it checks that the rebuilt sheet is identical to the input.
"""

import pathlib
import sys
from PIL import Image

FACE = 12
SHEET_WIDTH = FACE * 6
SLOTS = (2, 5, 8)

# 每个点数占用的穴位, 以 (列序号, 行序号) 表示, 序号 0 左/上, 1 中, 2 右/下
LAYOUTS = {
    1: ((1, 1),),
    2: ((0, 0), (2, 2)),
    3: ((0, 0), (1, 1), (2, 2)),
    4: ((0, 0), (2, 0), (0, 2), (2, 2)),
    5: ((0, 0), (2, 0), (1, 1), (0, 2), (2, 2)),
    6: ((0, 0), (2, 0), (0, 1), (2, 1), (0, 2), (2, 2)),
}


def load_source(path):
    """
    Returns the whole source image, its first 12x12 face and the source kind.
    The whole image is kept so a sheet source can still be sliced into its six cells.
    """
    image = Image.open(path).convert("RGBA")
    width, height = image.size

    if width == SHEET_WIDTH and height == FACE:
        return image, image.crop((0, 0, FACE, FACE)), "sheet"

    if width == FACE and height == FACE:
        return image, image.copy(), "single"

    if width == 16 and height == 16:
        return image, image.crop((2, 2, 2 + FACE, 2 + FACE)), "single"

    raise SystemExit("unsupported source size %dx%d for %s" % (width, height, path))


def cells_of(image):
    return [image.crop((index * FACE, 0, (index + 1) * FACE, FACE)) for index in range(6)]


def slot_box(column, row):
    x = SLOTS[column]
    y = SLOTS[row]
    return x, y, x + 2, y + 2


def read_slots(face):
    """Returns the occupied slots, the 2x2 pip pattern they share, and the body colour."""
    pixels = face.load()
    body = pixels[4, 4]
    occupied = []
    pattern = None

    for row in range(3):
        for column in range(3):
            x0, y0, x1, y1 = slot_box(column, row)
            block = tuple(pixels[x, y] for y in range(y0, y1) for x in range(x0, x1))

            if all(pixel == body for pixel in block):
                continue

            if pattern is None:
                pattern = block
            elif pattern != block:
                raise SystemExit("pip pattern differs between slots")

            occupied.append((column, row))

    if pattern is None:
        raise SystemExit("the source face has no pips to reuse")

    return occupied, pattern, body


def number_of(occupied):
    """Reads the die number back from the occupied slots, or None when the layout is not a standard one."""
    spot = set(occupied)

    for number, layout in LAYOUTS.items():
        if spot == set(layout):
            return number

    return None


def clear_frame(face, body):
    """The face with every pip slot painted back to the body colour."""
    frame = face.copy()
    pixels = frame.load()

    for row in range(3):
        for column in range(3):
            x0, y0, x1, y1 = slot_box(column, row)

            for y in range(y0, y1):
                for x in range(x0, x1):
                    pixels[x, y] = body

    return frame


def draw_face(frame, pattern, number):
    face = frame.copy()
    pixels = face.load()

    for column, row in LAYOUTS[number]:
        x0, y0, x1, y1 = slot_box(column, row)
        index = 0

        for y in range(y0, y1):
            for x in range(x0, x1):
                pixels[x, y] = pattern[index]
                index += 1

    return face


def build(path, expected_number):
    full, source, kind = load_source(path)
    occupied, pattern, body = read_slots(source)
    number = number_of(occupied)

    print("%s  source=%s  kind=%s" % (path.name, Image.open(path).size, kind))
    print("   body=%s  pip=%s" % (body, pattern[0]))
    print("   first face reads as %s with %d pips" % (number, len(occupied)))

    frame = clear_frame(source, body)

    if kind == "single":
        if number != expected_number:
            raise SystemExit("the original face must read as %d, not %s" % (expected_number, number))

        rebuilt = draw_face(frame, pattern, number)

        if rebuilt.tobytes() != source.tobytes():
            raise SystemExit("rebuilding the original face from its frame did not reproduce it")

        print("   reuse check: the original face is reproduced pixel for pixel")
    else:
        """
        以六格贴图为输入时, 校验每一格的边框, 点样式与底色都与首格一致,
        否则重建出来的贴图会与输入不同, 下面的逐字节比较会直接报错。
        """
        for index, cell in enumerate(cells_of(full), start=1):
            _, cell_pattern, cell_body = read_slots(cell)

            if cell_body != body or cell_pattern != pattern:
                raise SystemExit("cell %d uses a different body or pip style" % index)

            if clear_frame(cell, cell_body).tobytes() != frame.tobytes():
                raise SystemExit("cell %d uses a different frame" % index)

        print("   reuse check: all six cells share one frame and one pip style")

    sheet = Image.new("RGBA", (SHEET_WIDTH, FACE), (0, 0, 0, 0))

    for index in range(6):
        sheet.paste(draw_face(frame, pattern, index + 1), (index * FACE, 0))

    if kind == "sheet":
        if sheet.tobytes() != full.tobytes():
            raise SystemExit("rebuilding the sheet did not reproduce it, refusing to overwrite")

        print("   idempotence check: the rebuilt sheet is identical to the input")

    sheet.save(path)
    print("   wrote %s %dx%d" % (path.name, sheet.size[0], sheet.size[1]))

    for index, cell in enumerate(cells_of(sheet), start=1):
        cell_occupied, _, _ = read_slots(cell)
        print("   cell %d -> %s pips" % (index, number_of(cell_occupied)))


def main():
    root = pathlib.Path("src/main/resources/assets/devil_bone_gambler/textures/item")
    build(root / "devils_dice.png", 6)
    print()
    build(root / "gamblers_dice.png", 5)


if __name__ == "__main__":
    main()

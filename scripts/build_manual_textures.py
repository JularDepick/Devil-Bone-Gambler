#!/usr/bin/env python3
"""Draws the three manual item textures of Devil Bone Gambler.

The three manuals share one book shape and differ only in the palette of the cover and in the small emblem, so the
textures stay recognisable as one family. Everything is drawn on a 16x16 grid with a fixed palette, no anti aliasing
and no gradient, and the script is deterministic: running it again produces the same files.

The script only writes the three PNG files under assets/devil_bone_gambler/textures/item/.
"""

from __future__ import annotations

import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "textures" / "item"

SIZE = 16
TRANSPARENT = (0, 0, 0, 0)

# One palette per manual: cover, cover shade, cover highlight, page, page line, emblem.
# The cover carries the same colour as the manual name: gameplay 00FF00, admin 00FFAA, creator FFAA00,
# with a darker spine and a lighter highlight so the little book still reads as bound at 16 pixels.
PALETTES = {
    "gameplay_manual": {
        "cover": (0, 255, 0),
        "shade": (0, 178, 0),
        "highlight": (77, 255, 77),
        "page": (232, 224, 200),
        "line": (198, 188, 162),
        "emblem": (240, 240, 240),
    },
    "admin_manual": {
        "cover": (0, 255, 170),
        "shade": (0, 178, 119),
        "highlight": (77, 255, 196),
        "page": (232, 224, 200),
        "line": (198, 188, 162),
        "emblem": (255, 230, 150),
    },
    "creator_manual": {
        "cover": (255, 170, 0),
        "shade": (178, 119, 0),
        "highlight": (255, 204, 77),
        "page": (232, 224, 200),
        "line": (198, 188, 162),
        "emblem": (250, 245, 235),
    },
}

# Cover body: rows 2 to 13, columns 1 to 14. Spine: the first two columns.
COVER_TOP = 2
COVER_BOTTOM = 13
COVER_LEFT = 1
COVER_RIGHT = 14
SPINE_RIGHT = 2
PAGES_LEFT = 9
PAGES_RIGHT = 13
PAGES_TOP = 4
PAGES_BOTTOM = 12


def blank() -> Image.Image:
    return Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)


def draw(kind: str) -> Image.Image:
    palette = PALETTES[kind]
    image = blank()
    pixels = image.load()

    for y in range(COVER_TOP, COVER_BOTTOM + 1):
        for x in range(COVER_LEFT, COVER_RIGHT + 1):
            pixels[x, y] = palette["cover"] + (255,)

    # Spine and the outer edge of the cover, both a shade darker.
    for y in range(COVER_TOP, COVER_BOTTOM + 1):
        for x in range(COVER_LEFT, SPINE_RIGHT + 1):
            pixels[x, y] = palette["shade"] + (255,)

        pixels[COVER_RIGHT, y] = palette["shade"] + (255,)

    for x in range(COVER_LEFT, COVER_RIGHT + 1):
        pixels[x, COVER_TOP] = palette["shade"] + (255,)
        pixels[x, COVER_BOTTOM] = palette["shade"] + (255,)

    # A highlight line on the spine, which is what makes the book read as bound.
    for y in range(COVER_TOP + 2, COVER_BOTTOM - 1):
        pixels[SPINE_RIGHT - 1, y] = palette["highlight"] + (255,)

    # The page block on the right hand side, with two page lines.
    for y in range(PAGES_TOP, PAGES_BOTTOM + 1):
        for x in range(PAGES_LEFT, PAGES_RIGHT + 1):
            pixels[x, y] = palette["page"] + (255,)

    for y in (PAGES_TOP + 3, PAGES_BOTTOM - 3):
        for x in range(PAGES_LEFT + 1, PAGES_RIGHT):
            pixels[x, y] = palette["line"] + (255,)

    # Emblem: one dice pip for the gameplay manual, a bar for the admin manual and a crown tip for the creator.
    if kind == "gameplay_manual":
        pixels[6, 7] = palette["emblem"] + (255,)
        pixels[6, 8] = palette["emblem"] + (255,)
    elif kind == "admin_manual":
        for x in range(5, 8):
            pixels[x, 7] = palette["emblem"] + (255,)
        pixels[6, 9] = palette["emblem"] + (255,)
    else:
        for x in range(5, 8):
            pixels[x, 8] = palette["emblem"] + (255,)
        pixels[5, 7] = palette["emblem"] + (255,)
        pixels[7, 7] = palette["emblem"] + (255,)
        pixels[6, 6] = palette["emblem"] + (255,)

    return image


def main() -> int:
    OUTPUT.mkdir(parents=True, exist_ok=True)

    for kind in PALETTES:
        image = draw(kind)
        path = OUTPUT / (kind + ".png")
        image.save(path, "PNG")
        raw = image.tobytes()
        opaque = sum(1 for index in range(3, len(raw), 4) if raw[index] == 255)
        print("%s: %dx%d, %d opaque pixels" % (path.name, image.width, image.height, opaque))

    print("manual textures written")
    return 0


if __name__ == "__main__":
    sys.exit(main())

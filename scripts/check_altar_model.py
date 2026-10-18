# -*- coding: utf-8 -*-
"""Check the devil's altar block model, its atlas and its particle texture.

The script only uses the Python standard library. Run it from anywhere:

    python scripts/check_altar_model.py

It exits with 0 when every check passes and with 1 when at least one check fails.
"""

import json
import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "devil_bone_gambler")
NAMESPACE = "devil_bone_gambler"
MODEL = os.path.join(ASSETS, "models", "block", "devils_altar.json")
ATLAS = os.path.join(ASSETS, "textures", "block", "devils_altar.png")
PARTICLE = os.path.join(ASSETS, "textures", "block", "devils_altar_particle.png")
ITEM_MODEL = os.path.join(ASSETS, "models", "item", "devils_altar.json")
BLOCKSTATE = os.path.join(ASSETS, "blockstates", "devils_altar.json")
BLOCK_MODEL_ID = NAMESPACE + ":block/devils_altar"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
        print("FAIL %s" % message)

    return condition


def read_png(path):
    """Decode a non interlaced 8 bit RGBA png into (width, height, bytearray)."""
    with open(path, "rb") as handle:
        data = handle.read()

    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("%s is not a png" % path)

    position = 8
    payload = b""
    header = None

    while position < len(data):
        length = struct.unpack(">I", data[position:position + 4])[0]
        kind = data[position + 4:position + 8]
        chunk = data[position + 8:position + 8 + length]
        position += 12 + length

        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", chunk)
        elif kind == b"IDAT":
            payload += chunk
        elif kind == b"IEND":
            break

    width, height, depth, colour, _, _, interlace = header

    if depth != 8 or colour != 6 or interlace != 0:
        raise ValueError("%s must be 8 bit RGBA and not interlaced" % path)

    raw = zlib.decompress(payload)
    stride = width * 4
    pixels = bytearray()
    previous = bytearray(stride)
    offset = 0

    for _ in range(height):
        kind = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride

        for index in range(stride):
            left = line[index - 4] if index >= 4 else 0
            above = previous[index]
            corner = previous[index - 4] if index >= 4 else 0

            if kind == 1:
                line[index] = (line[index] + left) & 0xFF
            elif kind == 2:
                line[index] = (line[index] + above) & 0xFF
            elif kind == 3:
                line[index] = (line[index] + ((left + above) >> 1)) & 0xFF
            elif kind == 4:
                estimate = abs(above - corner)
                guess = abs(left - corner)
                diagonal = abs(left + above - 2 * corner)
                predictor = left if (estimate <= guess and estimate <= diagonal) else (
                    above if guess <= diagonal else corner)
                line[index] = (line[index] + predictor) & 0xFF
            elif kind != 0:
                raise ValueError("%s uses the unknown filter %d" % (path, kind))

        pixels += line
        previous = line

    return width, height, pixels


class Image(object):
    def __init__(self, path):
        self.width, self.height, self.pixels = read_png(path)

    def at(self, x, y):
        base = (y * self.width + x) * 4
        return tuple(self.pixels[base:base + 4])

    def alpha(self, x, y):
        return self.pixels[(y * self.width + x) * 4 + 3]


def texture_path(reference):
    namespace, _, path = reference.partition(":")
    return os.path.join(ROOT, "src", "main", "resources", "assets", namespace, "textures",
                        path + ".png")


for path in (MODEL, ATLAS, PARTICLE, ITEM_MODEL, BLOCKSTATE):
    check(os.path.isfile(path), "missing file %s" % os.path.relpath(path, ROOT))

if failures:
    print("RESULT: %d failure(s)" % len(failures))
    sys.exit(1)

with open(MODEL, encoding="utf-8") as handle:
    model = json.load(handle)

atlas = Image(ATLAS)
particle = Image(PARTICLE)

print("model parent: %s" % model.get("parent"))
print("atlas %dx%d, particle %dx%d" % (atlas.width, atlas.height, particle.width, particle.height))

check(model.get("parent") == "block/block",
      "the parent must be block/block so the inventory keeps the block display transforms")
check(list(model.get("texture_size", [])) == [atlas.width, atlas.height],
      "texture_size does not match the atlas size")

textures = model.get("textures", {})
check(bool(textures), "the model declares no texture")

for key, reference in textures.items():
    path = texture_path(reference)
    check(os.path.isfile(path), "texture key %s points at the missing %s" % (key, reference))
    print("texture key %s -> %s" % (key, reference))

check("particle" in textures, "the model declares no particle texture")

elements = model.get("elements", [])
occupied = {}
low = [16.0, 16.0, 16.0]
high = [0.0, 0.0, 0.0]
faces = 0
coverage = []

for index, element in enumerate(elements):
    start = element.get("from")
    end = element.get("to")

    if not check(bool(start) and bool(end), "element %d has no from or to" % index):
        continue

    for axis in range(3):
        low[axis] = min(low[axis], start[axis])
        high[axis] = max(high[axis], end[axis])

    for x in range(int(start[0]), int(end[0])):
        for y in range(int(start[1]), int(end[1])):
            for z in range(int(start[2]), int(end[2])):
                cell = (x, y, z)

                if cell in occupied:
                    check(False, "voxel %s is covered by two cuboids" % (cell,))

                occupied[cell] = index

    element_faces = element.get("faces", {})
    check(len(element_faces) == 6, "element %d declares %d faces" % (index, len(element_faces)))

    for direction, face in element_faces.items():
        faces += 1
        check("cullface" not in face, "element %d face %s declares cullface" % (index, direction))
        uv = face.get("uv")

        if not check(uv is not None, "element %d face %s has no uv" % (index, direction)):
            continue

        check(0 <= uv[0] and uv[2] <= 16 and 0 <= uv[1] and uv[3] <= 16,
              "element %d face %s leaves the 0 to 16 uv space" % (index, direction))
        reference = face.get("texture") or ""
        key = reference[1:] if reference.startswith("#") else reference
        check(key in textures,
              "element %d face %s references the undeclared texture %s"
              % (index, direction, reference))

        left = max(0, min(int(round(min(uv[0], uv[2]) / 16.0 * atlas.width)), atlas.width - 1))
        right = max(left + 1, min(int(round(max(uv[0], uv[2]) / 16.0 * atlas.width)), atlas.width))
        top = max(0, min(int(round(min(uv[1], uv[3]) / 16.0 * atlas.height)), atlas.height - 1))
        bottom = max(top + 1, min(int(round(max(uv[1], uv[3]) / 16.0 * atlas.height)), atlas.height))
        total = 0
        opaque = 0

        for px in range(left, right):
            for py in range(top, bottom):
                total += 1

                if atlas.alpha(px, py) > 0:
                    opaque += 1

        coverage.append((0.0 if total == 0 else float(opaque) / total, index, direction, uv))

print("elements: %d, faces: %d" % (len(elements), faces))
print("bounding box: %s to %s, occupied voxels: %d" % (low, high, len(occupied)))

check(low[0] >= 0 and low[2] >= 0 and high[0] <= 16 and high[2] <= 16,
      "the model leaves the 1x1 footprint")
check(high[1] <= 16, "the model is taller than one block")

worst = sorted(coverage)[:3]

for ratio, index, direction, uv in worst:
    print("lowest atlas coverage: element %d face %s uv %s -> %.0f%% opaque"
          % (index, direction, uv, ratio * 100))

check(not worst or worst[0][0] >= 0.9, "a face samples a mostly transparent atlas region")

for image, name in ((atlas, "atlas"), (particle, "particle")):
    semi = 0
    transparent = 0
    colours = set()

    for y in range(image.height):
        for x in range(image.width):
            pixel = image.at(x, y)
            colours.add(pixel)

            if pixel[3] == 0:
                transparent += 1
            elif pixel[3] < 255:
                semi += 1

    print("%s: %d colours, %d transparent, %d semi transparent pixels"
          % (name, len(colours), transparent, semi))
    check(semi == 0, "the %s contains semi transparent pixels" % name)

check(particle.width == 16 and particle.height == 16, "the particle texture is not 16x16")

with open(ITEM_MODEL, encoding="utf-8") as handle:
    item = json.load(handle)

with open(BLOCKSTATE, encoding="utf-8") as handle:
    blockstate = json.load(handle)

variants = blockstate.get("variants", {})
model_id = variants.get("", {}).get("model") if isinstance(variants.get(""), dict) else None

print("item model parent: %s" % item.get("parent"))
print("blockstate model: %s" % model_id)

check(item.get("parent") == BLOCK_MODEL_ID, "the item model does not point at the block model")
check(model_id == BLOCK_MODEL_ID, "the blockstate does not point at the block model")


# The altar writes its own offering value over its head while a player stands close and looks at it,
# and the offering screen carries the same number.
LABEL_RENDERER = os.path.join(ROOT, "src", "main", "java", "com", "devilbonegambler", "client", "render",
                              "AltarValueLabelRenderer.java")
ALTAR_SCREEN = os.path.join(ROOT, "src", "main", "java", "com", "devilbonegambler", "client", "gui",
                            "AltarScreen.java")
CONSTANTS = os.path.join(ROOT, "src", "main", "java", "com", "devilbonegambler", "util", "ModConstants.java")

label_source = open(LABEL_RENDERER, encoding="utf-8").read() if os.path.exists(LABEL_RENDERER) else ""
screen_source = open(ALTAR_SCREEN, encoding="utf-8").read()
constants_source = open(CONSTANTS, encoding="utf-8").read()

check(os.path.exists(LABEL_RENDERER), "the altar value label renderer is missing")
check("ClientActions.altarPosition()" in label_source and "isInAltarRange()" in label_source,
      "the label does not follow the altar that governs the player")
check("ALTAR_LABEL_RANGE" in label_source and "ALTAR_LABEL_LOOK_DOT" in label_source,
      "the label does not require the player to be close and facing the altar")
check("rotationY" in label_source and "rotationX" in label_source,
      "the label does not face the viewer")
check("textBackground" in label_source, "the label does not draw a plate behind the number")
check("uv2(" in label_source,
      "the label plate fills no lightmap element, which makes the vertex builder reject the vertex")
check(".uv(" not in label_source,
      "the label plate writes texture coordinates that its render type has no slot for, hiding a missing element")
check("drawAltarValue" in screen_source and "altar.altar_value" in screen_source,
      "the offering screen does not show the altar offering value")
check("if (!ClientActions.hasAltar())" in screen_source,
      "the offering screen shows a zero when no altar governs the player")
check("ALTAR_LABEL_RANGE" in constants_source and "ALTAR_LABEL_LOOK_DOT" in constants_source,
      "the altar label constants are missing")


check("MID_TEXT_X = 192" in screen_source and "MID_TEXT_WIDTH = 106" in screen_source,
      "the altar summary block is not laid out right of the offering grid")
check(screen_source.count("MID_TEXT_X") >= 5,
      "a summary line still sits in the creature column")
check("altar.hint\")), MOB_X" not in screen_source,
      "the hint still takes room from the creature column")


slider_source = open(ROOT + "/src/main/java/com/devilbonegambler/client/gui/IntSlider.java", encoding="utf-8").read()
check("isDraggingValue()" in screen_source and "slider.mouseDragged" in screen_source
      and "draggingValue" in slider_source
      and "public boolean mouseClicked(double mouseX, double mouseY, int button)" in slider_source,
      "the number sliders can only be clicked, not dragged")

print("RESULT: %s" % ("OK" if not failures else "%d failure(s)" % len(failures)))
sys.exit(0 if not failures else 1)

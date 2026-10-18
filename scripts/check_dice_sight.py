#!/usr/bin/env python3
"""Dice sight guard of Devil Bone Gambler.

Checks that the dice sight is a halo around the real model instead of a repaint of it, that it only shows while the
player is sneaking with a dice in hand, and that the halo is drawn with the depth test on so the model keeps its own
texture and only the rim stays visible.

  1. the sight renderer never draws a line box and never uses the line render type,
  2. the sight is drawn after the translucent blocks and both sights use the halo painter,
  3. the batch uploads itself through the colour shader instead of a render type,
  4. the depth test stays enabled, and the blend is additive so the rim reads as light,
  5. the halo is a scaled up copy of the geometry pushed away from the camera, never a white repaint,
  6. the sight needs the sneak key, and the dealer capture layer is attached to the dealer renderer.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
RENDER_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "render"
SIGHT = RENDER_ROOT / "DiceSightRenderer.java"
HALO = RENDER_ROOT / "SightHalo.java"
LAYER = RENDER_ROOT / "DiceSightLayer.java"
DEALER_RENDERER = RENDER_ROOT / "GamblersDealerRenderer.java"
OLD_PAINTER = RENDER_ROOT / "WhiteSilhouette.java"
CONSTANTS = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java"
BOX = re.compile(r"renderLineBox|RenderType\.lines\s*\(")
WHITE = re.compile(r"DICE_SIGHT_HIGHLIGHT_COLOR\s*=\s*0xFFFFFF")
WARM = re.compile(r"DICE_SIGHT_HIGHLIGHT_COLOR\s*=\s*0x[0-9A-Fa-f]{6}")
ALPHA = re.compile(r"DICE_SIGHT_HIGHLIGHT_ALPHA\s*=\s*(0?\.\d+|1\.0)F")
SCALE = re.compile(r"DICE_SIGHT_HALO_SCALE\s*=\s*1\.\d+F")
PUSH = re.compile(r"DICE_SIGHT_HALO_PUSH\s*=\s*0\.\d+F")


def main() -> int:
    problems = []

    for path in (SIGHT, HALO, LAYER, DEALER_RENDERER, CONSTANTS):
        if not path.is_file():
            print("missing the dice sight file: " + str(path))
            return 1

    if OLD_PAINTER.is_file():
        problems.append("the old white silhouette painter is still present: " + OLD_PAINTER.name)

    sight = SIGHT.read_text(encoding="utf-8")
    halo = HALO.read_text(encoding="utf-8")
    dealer = DEALER_RENDERER.read_text(encoding="utf-8")
    constants = CONSTANTS.read_text(encoding="utf-8")

    if BOX.search(sight):
        problems.append("draws a line box instead of a model halo: " + SIGHT.name)

    if "AFTER_TRANSLUCENT_BLOCKS" not in sight:
        problems.append("does not draw after the translucent blocks: " + SIGHT.name)

    if "halo.block(" not in sight or "halo.model(" not in sight:
        problems.append("does not draw both sights through the halo painter: " + SIGHT.name)

    if "BufferUploader.drawWithShader" not in halo:
        problems.append("uploads the batch through a render type instead of directly: " + HALO.name)

    if "getPositionColorShader" not in halo:
        problems.append("does not upload the batch with the colour shader: " + HALO.name)

    if "enableDepthTest" not in sight or "disableDepthTest" in sight:
        problems.append("the depth test must stay on, otherwise the halo repaints the model: " + SIGHT.name)

    if "DestFactor.ONE" not in sight:
        problems.append("the halo is not blended additively: " + SIGHT.name)

    if "haloPose(" not in sight or "haloPose(centre)" not in sight:
        problems.append("the halo is not placed with the scaled shell transform: " + SIGHT.name)

    if "isShiftKeyDown()" not in sight:
        problems.append("the sight does not require the sneak key: " + SIGHT.name)

    if WHITE.search(constants):
        problems.append("the highlight is still a white repaint: " + CONSTANTS.name)

    if not WARM.search(constants):
        problems.append("the highlight colour is missing: " + CONSTANTS.name)

    if not ALPHA.search(constants):
        problems.append("the highlight alpha is missing or out of range: " + CONSTANTS.name)

    if not SCALE.search(constants) or not PUSH.search(constants):
        problems.append("the halo scale or push is missing: " + CONSTANTS.name)

    if "addLayer(new DiceSightLayer(" not in dealer:
        problems.append("the dealer renderer never collects the sight: " + DEALER_RENDERER.name)

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("dice sight check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

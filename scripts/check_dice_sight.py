#!/usr/bin/env python3
"""Dice sight guard of Devil Bone Gambler.

Checks that the dice sight keeps drawing a white silhouette of the real geometry instead of a box outline, and that
the depth test is really off for the batch, because a render type would switch it back on when the batch is uploaded.

  1. the sight renderer never draws a line box and never uses the line render type,
  2. the sight renderer disables the depth test before the batch is filled and uploaded,
  3. the batch uploads itself through the colour shader instead of a render type,
  4. the sight is drawn after the translucent blocks and both sights use the silhouette painter,
  5. the highlight is white and the dealer capture layer is attached to the dealer renderer.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
RENDER_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "render"
SIGHT = RENDER_ROOT / "DiceSightRenderer.java"
SILHOUETTE = RENDER_ROOT / "WhiteSilhouette.java"
LAYER = RENDER_ROOT / "DiceSightLayer.java"
DEALER_RENDERER = RENDER_ROOT / "GamblersDealerRenderer.java"
CONSTANTS = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java"
BOX = re.compile(r"renderLineBox|RenderType\.lines\s*\(")
WHITE = re.compile(r"DICE_SIGHT_HIGHLIGHT_COLOR\s*=\s*0xFFFFFF")
ALPHA = re.compile(r"DICE_SIGHT_HIGHLIGHT_ALPHA\s*=\s*(0?\.\d+|1\.0)F")


def main() -> int:
    problems = []

    for path in (SIGHT, SILHOUETTE, LAYER, DEALER_RENDERER, CONSTANTS):
        if not path.is_file():
            print("missing the dice sight file: " + str(path))
            return 1

    sight = SIGHT.read_text(encoding="utf-8")
    silhouette = SILHOUETTE.read_text(encoding="utf-8")
    dealer = DEALER_RENDERER.read_text(encoding="utf-8")
    constants = CONSTANTS.read_text(encoding="utf-8")

    if BOX.search(sight):
        problems.append("draws a line box instead of a model silhouette: " + SIGHT.name)

    if "AFTER_TRANSLUCENT_BLOCKS" not in sight:
        problems.append("does not draw after the translucent blocks: " + SIGHT.name)

    if "silhouette.block(" not in sight or "silhouette.model(" not in sight:
        problems.append("does not draw both sights through the silhouette painter: " + SIGHT.name)

    if "BufferUploader.drawWithShader" not in silhouette:
        problems.append("uploads the batch through a render type instead of directly: " + SILHOUETTE.name)

    if "getPositionColorShader" not in silhouette:
        problems.append("does not upload the batch with the colour shader: " + SILHOUETTE.name)

    for required, order in (
        ("disableDepthTest", "WhiteSilhouette.begin"),
        ("depthMask(false)", "WhiteSilhouette.begin"),
        ("disableDepthTest", "silhouette.finish()"),
    ):
        state_at = sight.find(required)
        use_at = sight.find(order)

        if state_at < 0 or use_at < 0 or state_at > use_at:
            problems.append("the depth state is not set before the batch: " + required)

    if not WHITE.search(constants):
        problems.append("the highlight is not white: " + CONSTANTS.name)

    if not ALPHA.search(constants):
        problems.append("the highlight alpha is missing or out of range: " + CONSTANTS.name)

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

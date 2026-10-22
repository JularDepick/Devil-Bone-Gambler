#!/usr/bin/env python3
"""Placed dice guard of Devil Bone Gambler.

The modifier key places a held die as a block. Breaking it without that key uses the die whatever the player is holding
and never spawns a drop, while breaking it with the key held only drops the die. This check asserts the placement
packet, the modifier mirror, the break branch and the client key registration are all present.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
BLOCK = JAVA / "block" / "PlacedDiceBlock.java"
SERVICE = JAVA / "block" / "PlacedDiceService.java"
KEY = JAVA / "client" / "DicePlacementKey.java"
SETUP = JAVA / "client" / "ClientSetup.java"
NETWORK = JAVA / "network" / "ModNetwork.java"
HANDLER = JAVA / "client" / "ClientPacketHandler.java"
PACKET = JAVA / "network" / "packet" / "PlacedDiceUsePacket.java"
BREAK_PACKET = JAVA / "network" / "packet" / "PlacedDiceBreakPacket.java"
REQUESTS = JAVA / "network" / "ServerRequestHandler.java"
GUARD = JAVA / "network" / "RequestGuard.java"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def main() -> int:
    service = read(SERVICE)
    key = read(KEY)
    setup = read(SETUP)
    network = read(NETWORK)
    handler = read(HANDLER)
    packet = read(PACKET)
    block = read(BLOCK)
    break_packet = read(BREAK_PACKET)
    requests = read(REQUESTS)
    guard = read(GUARD)

    check("modifier key", "GLFW.GLFW_KEY_TAB" in key and "KeyMapping" in key,
          "the default Tab key mapping is missing")
    check("key registration", "DicePlacementKey.register" in setup and "RegisterKeyMappingsEvent" in setup,
          "the key mapping is not registered")
    check("right click interception", "InputEvent.MouseButton.Pre" in key and "event.setCanceled(true)" in key,
          "a right click with the modifier still reaches the dice use")
    check("placement packet", "PlaceDicePacket" in key and "PlaceDicePacket::encode" in network,
          "the placement request is missing or unregistered")
    check("modifier mirror", "DiceModifierPacket" in key and "DiceModifierPacket::encode" in network,
          "the modifier state is not mirrored to the server")

    check("place service", "setBlockAndUpdate" in service and "PLACED_DEVILS_DICE" in service,
          "the server does not place the die block")
    check("consume survival", "stack.shrink(1)" in service and "instabuild" in service,
          "placing a die neither consumes it nor respects creative mode")
    check("no empty hand gate", "player.getMainHandItem().isEmpty()" not in service
          and "player.getOffhandItem().isEmpty()" not in service,
          "the break branch still requires empty hands")
    check("use without drop", "getInventory().add(dice)" in service and "setBlockAndUpdate(pos, broken)" in service,
          "the use branch must charge the inventory and must put the block back when it is full")
    check("key held drops", "if (modifierHeld)" in service and "popResource" in service,
          "the key held break does not drop the die")
    check("player break drops nothing", "LootContextParams.THIS_ENTITY" in block and "instanceof Player" in block
          and "Collections.emptyList()" in block,
          "a player break still drops through the loot path, so a use can turn into a silent drop")
    check("break carries the key", "PlacedDiceBreakPacket(hit.getBlockPos(), KEY.isDown())" in key
          and "breakAt(player, message.pos(), message.modifierHeld())" in requests,
          "the break request does not carry the key state, so a stale mirror decides the outcome")
    check("break packet", "writeBlockPos" in break_packet and "writeBoolean" in break_packet
          and "PlacedDiceBreakPacket.class" in network,
          "the break packet is not built or not registered")
    check("modifier branch", "isModifierHeld(player)" in service and "event.setCanceled(true)" in service,
          "the break branch ignores the modifier or does not stop the vanilla break")
    check("use branch", "PlacedDiceUsePacket.GAMBLERS_DRAW" in service
          and "PlacedDiceUsePacket.DEVILS_DRAW" in service
          and "PlacedDiceUsePacket.DEVILS_ALTAR" in service,
          "the three break use modes are not wired")
    check("altar inventory", "AltarService.governingValue" in service and "getInventory().add" in service,
          "the devil's die in altar range is not returned to the inventory")
    peek = guard.split("public static String peekDraw(", 1)[-1].split("public static void clear(", 1)[0]
    check("throttle gate read only", "isSettling(player)" in peek and "MIN_REQUEST_INTERVAL_MS" in peek
          and "state.sequence =" not in peek and "state.timestamp =" not in peek,
          "the read only gate missing, incomplete, or advancing the sequence and interval stamp")
    check("throttle gate on break", "if (!modifierHeld)" in service and "RequestGuard.peekDraw(player)" in service,
          "breaking a placed die is not gated by the draw guard before it removes the block")
    check("throttle gate ordering", service.find("RequestGuard.peekDraw(player)")
          < service.find("removeBlock(pos, false)")
          and service.count("RequestGuard.peekDraw(player)") >= 2,
          "the guard is not consulted before the block disappears in both break paths")
    check("drops", "getDrops" in block and "DEVILS_DICE" in block and "GAMBLERS_DICE" in block,
          "the placed block does not drop the matching die")
    constants = read(JAVA / "util" / "ModConstants.java")
    registry = read(JAVA / "registry" / "ModBlocks.java")
    check("blast proof", "PLACED_DICE_EXPLOSION_RESISTANCE" in constants
          and registry.count("PLACED_DICE_EXPLOSION_RESISTANCE") >= 2,
          "a placed die can still be blown up by an explosion")
    check("piston proof", "getPistonPushReaction" in block and "PushReaction.BLOCK" in block,
          "a piston can still shove a placed die")
    for immune in ("dragon_immune", "wither_immune"):
        tag = read(ROOT / "src" / "main" / "resources" / "data" / "minecraft" / "tags" / "blocks"
                   / (immune + ".json"))
        check(immune, "placed_gamblers_dice" in tag and "placed_devils_dice" in tag,
              "the placed dice are missing from the " + immune + " block tag")
    check("client use modes", "handlePlacedDiceUse" in handler and "sendDevilsDiceDraw" in handler,
          "the client does not start the normal action after the break")
    check("packet registered", "PlacedDiceUsePacket.class" in network,
          "the use packet is not registered")

    for kind in ("gamblers", "devils"):
        for part in ("blockstates", "models/block"):
            suffix = "json"
            path = ASSETS / part / ("placed_" + kind + "_dice." + suffix)
            check("asset " + kind + " " + part, path.is_file(), "the placed die asset is missing")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("placed dice check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

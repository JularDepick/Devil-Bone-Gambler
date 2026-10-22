#!/usr/bin/env python3
"""Luck system guard of Devil Bone Gambler.

Checks that

  1. the default value is 80 and the update interval is 200 ticks,
  2. the stored entry keeps the first join instant and reads old bare values,
  3. the fate model is deterministic, argument driven and clamped to [-50, 50],
  4. the periodic increment combines the fate value, a player seeded random term and the subjective behaviour value,
  5. the value is clamped to 0..100 and the behaviour round resets after the increment,
  6. the client receives the value and paints it in the top left corner,
  7. the colour bands and the language key are present.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
CONSTANTS = JAVA / "util" / "ModConstants.java"
DATA = JAVA / "data" / "LuckData.java"
MODEL = JAVA / "luck" / "FateModel.java"
SERVICE = JAVA / "luck" / "LuckService.java"
BEHAVIOR = JAVA / "luck" / "SubjectiveBehaviorService.java"
BEHAVIOR_PACKET = JAVA / "network" / "packet" / "BehaviorStatePacket.java"
BEHAVIOR_CLIENT = JAVA / "client" / "ClientBehaviorState.java"
DEALER = JAVA / "mob" / "GamblersDealer.java"
PROVENANCE = JAVA / "item" / "DiceProvenance.java"
OVERLAY = JAVA / "client" / "gui" / "LuckOverlay.java"
PACKET = JAVA / "network" / "packet" / "LuckSyncPacket.java"
NETWORK = JAVA / "network" / "ModNetwork.java"
HANDLER = JAVA / "client" / "ClientPacketHandler.java"
LANG = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def main() -> int:
    constants = read(CONSTANTS)
    data = read(DATA)
    model = read(MODEL)
    service = read(SERVICE)
    behavior = read(BEHAVIOR)
    behavior_packet = read(BEHAVIOR_PACKET)
    behavior_client = read(BEHAVIOR_CLIENT)
    dealer = read(DEALER)
    provenance = read(PROVENANCE)
    overlay = read(OVERLAY)
    packet = read(PACKET)
    network = read(NETWORK)
    handler = read(HANDLER)
    events = read(JAVA / "luck" / "LuckBehaviorEvents.java")
    draw = read(JAVA / "draw" / "DrawEngine.java")
    wager = read(JAVA / "wager" / "WagerService.java")
    dealer_wager = read(JAVA / "dealer" / "DealerWagerService.java")
    commands = read(JAVA / "commands" / "ModCommands.java")
    zh = json.loads(read(LANG / "zh_cn.json"))
    en = json.loads(read(LANG / "en_us.json"))

    check("default value", re.search(r"BASE_LUCK\s*=\s*80", constants) is not None,
          "the base luck is not 80")
    check("update interval", re.search(r"LUCK_UPDATE_INTERVAL_TICKS\s*=\s*200", constants) is not None,
          "the update interval is not 200 ticks")
    check("random span", re.search(r"LUCK_INCREMENT_RANDOM_SPAN\s*=\s*10", constants) is not None,
          "the random span is not 10")
    check("drift interval", re.search(r"LUCK_DRIFT_INTERVAL_TICKS\s*=\s*60", constants) is not None
          and "LUCK_DRIFT_INTERVAL_TICKS" in service,
          "the quick pull does not run every 60 ticks")
    check("drift step", re.search(r"LUCK_DRIFT_STEP\s*=\s*1", constants) is not None
          and "LUCK_DRIFT_STEP" in service,
          "the quick pull does not step by one point")
    drift_body = service.split("private static void drift(", 1)[-1][:1400]
    check("drift direction", "step = ModConstants.LUCK_DRIFT_STEP" in drift_body
          and "-ModConstants.LUCK_DRIFT_STEP" in drift_body,
          "the quick pull does not raise a low value and lower a high one")
    check("drift skips locked", "LuckLockService.isLocked(player)" in drift_body,
          "the quick pull overwrites a value locked by the child of fortune")
    check("increment limit", re.search(r"LUCK_INCREMENT_LIMIT\s*=\s*50", constants) is not None
          and "LUCK_INCREMENT_LIMIT" in service,
          "a single increment is not capped at 50")
    check("upper bound", re.search(r"MAX_LUCK\s*=\s*100", constants) is not None,
          "the upper luck bound is not 100")
    check("behaviour limit", re.search(r"SUBJECTIVE_BEHAVIOR_LIMIT\s*=\s*10", constants) is not None,
          "the subjective behaviour limit is not 10")
    check("pickup delta", re.search(r"DEALER_PICKUP_BEHAVIOR_DELTA\s*=\s*2", constants) is not None,
          "the dealer pickup delta is not 2")
    check("owner tag", re.search(r"DICE_OWNER_TAG\s*=\s*\"", constants) is not None,
          "the dice owner tag is missing")
    check("level up delta", re.search(r"LEVEL_UP_BEHAVIOR_DELTA\s*=\s*5", constants) is not None
          and "BEHAVIOR_LEVEL_UP" in behavior,
          "levelling up no longer pays the behaviour variable five points")
    check("manual conversion delta",
          re.search(r"MANUAL_CONVERSION_BEHAVIOR_DELTA\s*=\s*4", constants) is not None
          and "BEHAVIOR_MANUAL_CONVERSION" in behavior,
          "turning a manual pile into dice no longer pays the behaviour variable four points")
    conversion = read(JAVA / "altar" / "AltarManualConversion.java")
    check("manual conversion credit", "recordManualConversion(credited)" in conversion
          and "closest" in conversion,
          "the manual conversion pays nobody, or it pays every nearby player instead of the closest one")
    check("dice result delta", re.search(r"DICE_RESULT_MAX_COUNT\s*=\s*6", constants) is not None
          and re.search(r"DICE_RESULT_POINT_DIVISOR\s*=\s*2", constants) is not None
          and re.search(r"DICE_RESULT_RED_SIGN\s*=\s*-1", constants) is not None,
          "the dice result no longer pays one point per two points rolled")
    check("dice result wired", "recordDiceResult(player, roll.isRed(), roll.point())" in draw,
          "a draw does not feed its result into the behaviour variable")
    result_body = behavior.split("public static void recordDiceResult(", 1)[-1][:1600]
    check("dice result counts six", "DICE_RESULT_MAX_COUNT" in result_body
          and "resultCount" in result_body
          and "state.triggered" not in result_body,
          "the dice result is limited to one per round instead of the configured count")
    events_source = read(JAVA / "luck" / "LuckBehaviorEvents.java")
    check("level up hook", "PlayerXpEvent.LevelChange" in events_source
          and "getLevels() <= 0" in events_source
          and "recordLevelUp(player)" in events_source,
          "the level up hook is missing or it also pays for lost levels")

    check("birth stored", "TAG_BIRTH" in data and "birth" in data,
          "the first join instant is not stored")
    check("old values readable", "NumericTag" in data and "getAsInt" in data,
          "a bare value from an older file is not readable")
    check("default on ensure", "new Entry(ModConstants.BASE_LUCK, now)" in data,
          "a missing entry is not created with the base value and the current instant")

    check("fate deterministic", "Random" not in model and "System.currentTimeMillis" not in model,
          "the fate model reads a clock or a random source instead of its arguments")
    check("fate periods", all(token in constants for token in (
        "LUCK_DAY_TICKS = 24000L", "LUCK_MONTH_TICKS = 720000L", "LUCK_YEAR_TICKS = 8640000L")),
        "the three calendar periods counted in game ticks are missing")
    check("fate weights", all(token in model for token in ("0.45D", "0.35D", "0.20D")),
          "the channel weights are not the day heavy ones")
    check("fate phase", "phaseSeed" in model and "LUCK_PHASE_STEPS" in model,
          "the fate channels ignore the player phase")
    check("fate clamped", "Math.max(-50.0D" in model and "Math.min(50.0D" in model,
        "the fate value is not clamped to [-50, 50]")
    check("fate elapsed only", "nowTick - birthTick" in model,
          "the fate value does not read the elapsed ticks")

    check("fate used", "FateModel.value(birth, now, phaseSeed(" in service,
          "the periodic increment does not use the fate value")
    check("game tick clock", "overworld().getGameTime()" in service,
          "the increment does not read the game tick clock")
    check("creation instant", "TAG_CREATED" in data and "ensureCreated" in data and "ensureCreated(" in service,
          "the save creation instant is not stored or not written once")
    check("legacy origin", "LEGACY_ORIGIN_THRESHOLD" in data,
          "a stored wall clock origin would never be rewritten as ticks")
    check("random combined", "random" in service and "fate + random + behavior + preferenceOf(current)" in service,
          "the random term, the behaviour value and the banded preference are not added to the fate value")
    check("banded preference", all(token in constants for token in (
        "LUCK_LOW_BAND = 50", "LUCK_HIGH_BAND = 80", "LUCK_LOW_DRIFT = 6", "LUCK_HIGH_DRIFT = -6")),
        "the banded luck preference is missing")
    check("preference bands", "luck < ModConstants.LUCK_LOW_BAND" in service
          and "luck > ModConstants.LUCK_HIGH_BAND" in service,
          "the preference is not applied below the low band and above the high band")
    check("behaviour read", "SubjectiveBehaviorService.value(player)" in service,
          "the increment does not read the subjective behaviour value")
    check("behaviour reset after", service.rindex("SubjectiveBehaviorService.finishRound(player)")
          > service.index("Math.round(fate + random + behavior + preferenceOf(current))"),
          "the behaviour round is not reset after the increment")
    check("behaviour applied", "state.value + delta" in behavior and "Mth.clamp" in behavior,
          "a behaviour does not change and clamp the variable")
    check("behaviour shared trigger", "state.triggered" in behavior and "if (state.triggered)" in behavior,
          "behaviours do not share one trigger per round")
    check("attack delta", "BEHAVIOR_ATTACK_DEALER" in behavior and "ATTACK_DEALER_BEHAVIOR_DELTA" in behavior,
          "attacking the dealer is not wired to the behaviour variable")
    check("dealer hook", "recordAttackDealer" in dealer,
          "the dealer does not report a accepted player hit")
    check("pickup behavior", "BEHAVIOR_DEALER_PICKUP_DICE" in behavior and "recordDealerPickup" in behavior,
          "the dealer pickup behaviour is missing")
    check("pickup amount", "recordAmount(player, behavior, 1)" in behavior and "delta * amount" in behavior,
          "a pickup cannot add the delta once per die")
    check("offering behavior", "BEHAVIOR_OFFERING" in behavior and "recordOffering(player)" in draw,
          "a settled offering does not move the behaviour variable")
    check("wager behavior", "BEHAVIOR_WAGER_JOIN" in behavior and "recordWagerJoin(player)" in wager
          and "recordWagerLosers" in wager and "recordWagerLoss(loser)" in wager,
          "joining or losing a wager does not move the behaviour variable")
    check("dealer wager behavior", "recordWagerJoin(player)" in dealer_wager
          and "recordWagerLoss(member)" in dealer_wager and "winner.getUUID()" in dealer_wager,
          "the dealer wager does not count its joins and its losers")
    check("kill behavior", "BEHAVIOR_KILL_HOSTILE" in behavior and "MobCategory.MONSTER" in events,
          "a kill is not classified into hostile and innocent")
    check("death moves value", "LUCK_DEATH_STEP" in constants and "nudgeAfterDeath" in service
          and "nudgeAfterDeath(victim)" in events,
          "dying does not move the luck value towards the comfortable band")
    check("death not a behavior", "BEHAVIOR_DEATH" not in behavior and "DEATH_BEHAVIOR_DELTA" not in constants,
          "dying still costs the behaviour variable")
    check("lock discards the round", service.index("LuckLockService.isLocked(player)")
          < service.index("data.setLuck(player.getUUID(), next)"),
          "the locked branch does not return before the value is written, so an increment could survive the lock")
    check("dice use behavior", "DICE_USE_BEHAVIOR_DELTA = 2" in constants and "BEHAVIOR_DICE_USE" in behavior
          and "recordDiceUse(player)" in draw,
          "rerolling a die does not count as a behaviour")
    check("provenance events", "ItemTossEvent" in provenance and "LivingDropsEvent" in provenance
          and "mark(event.getEntity().getItem()" in provenance,
          "player drops are not tagged with their owner")
    check("provenance read", "DICE_OWNER_TAG" in provenance and "DiceProvenance.owner(stack)" in dealer,
          "the dealer does not read the die owner")
    check("pickup aggregation", "pickedByOwner" in dealer and "awardPickups" in dealer
          and "recordDealerPickup(owner, entry.getValue().intValue())" in dealer,
          "multiple picked dice are not aggregated per player")
    check("refund provenance", "DiceProvenance.mark(stack, pending.playerId)" in read(JAVA / "draw" / "DrawEngine.java")
          and "DiceProvenance.mark(stack, player.getUUID())" in read(JAVA / "dealer" / "DealerWagerService.java"),
          "dice refunded to or dropped for a player lose their provenance")
    check("client mirror", "ClientBehaviorState.set" in handler and "triggered" in behavior_client,
          "the behaviour callback is not mirrored on the client")
    check("behaviour packet", "BehaviorStatePacket.class" in network and "handleBehaviorState" in handler,
          "the behaviour state packet is not registered or handled")
    check("behaviour packet payload", "writeBoolean" in behavior_packet and "writeVarInt" in behavior_packet,
          "the behaviour packet does not carry the trigger and the value")
    check("seed state", all(token in service for token in (
        "getMostSignificantBits", "getLeastSignificantBits", "birth", "current")),
        "the random seed ignores part of the player state")
    check("clamped", "Mth.clamp(current" in service,
          "the updated value is not clamped")
    check("synced", "new LuckSyncPacket" in service,
          "the new value is not sent to the client")

    check("packet registered", "LuckSyncPacket.class" in network,
          "the luck packet is not registered")
    check("packet handled", "handleLuckSync" in handler and "ClientLuck.set" in handler,
          "the client does not store the synced value")
    check("top left", "drawString(minecraft.font, text, LEFT_MARGIN, TOP_MARGIN" in overlay
          and "effectOffset" not in overlay,
          "the luck line is not anchored to the top left corner")
    check("margins", "LEFT_MARGIN = 4" in overlay and "TOP_MARGIN = 4" in overlay,
          "the luck line margins are not 4 pixels")
    check("screen gate", "minecraft.screen != null" in overlay,
          "the luck line is still drawn over open screens")
    check("colours", all(token in overlay for token in (
        "value <= 20", "value <= 40", "value <= 60", "value <= 80", "value < 90")),
        "the colour bands are incomplete")
    check("language key", "gui.devil_bone_gambler.hud.luck" in zh
          and "gui.devil_bone_gambler.hud.luck" in en,
          "the luck language key is missing")
    check("terminology", "幸运值" in zh["gui.devil_bone_gambler.hud.luck"],
          "the Chinese label does not use 幸运值")
    check("packet payload", "writeVarInt" in packet and "readVarInt" in packet,
          "the packet does not carry the value")
    check("set command", "IntegerArgumentType.integer(ModConstants.MIN_LUCK, ModConstants.MAX_LUCK)" in commands
          and "setLuck" in commands, "the admin luck command is missing or not bounded to 0..100")
    check("set command effect", "setLuck(targetId, value)" in commands and "new LuckSyncPacket(value)" in commands,
          "the admin luck command does not store and push the value")
    check("protocol version", 'PROTOCOL_VERSION = "3"' in constants,
          "the protocol version is not raised for this build")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("luck system check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())

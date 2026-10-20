"""
Guards the promise that no mod prompt is ever written above the hotbar, and that repeated refusals do not flood the chat
box.

The action bar area belongs to the game itself with one exception: the rolled dice point and colour, which the
player asked for above the hotbar. This check scans the mod sources and resources for any API or configuration value
that writes there, allows exactly that one call and exits non zero for every other one, so a future change can not
quietly reintroduce a second prompt.

The second check proves that an identical notice is held back for a second: refusals, and every invalid action that
repeats, such as sneaking with the dice while nobody is nearby or right clicking the dice outside an altar range. A
held key or a mistake made twice in a row cannot bury real messages.
"""

import pathlib
import re
import sys

SOURCE_ROOT = pathlib.Path("src/main/java")
RESOURCE_ROOT = pathlib.Path("src/main/resources")
NOTIFY_SENDER = SOURCE_ROOT / "com" / "devilbonegambler" / "draw" / "NotifySender.java"
CONSTANTS = SOURCE_ROOT / "com" / "devilbonegambler" / "util" / "ModConstants.java"

# 服务端与客户端两侧所有会写快捷栏上方的入口
SOURCE_PATTERNS = (
    ("setOverlayMessage", r"setOverlayMessage\s*\("),
    ("displayClientMessage", r"displayClientMessage\s*\("),
    ("action bar packet", r"ClientboundSetActionBarTextPacket"),
    ("title", r"\.setTitle\s*\(|\.setSubtitle\s*\(|\.setTimes\s*\("),
    ("action bar mode", r"NOTIFY_MODE_ACTIONBAR|ACTIONBAR"),
    ("actionbar literal", r"[\"\']actionbar[\"\']"),
)

# 通知模式里也不允许留有快捷栏的取值
RESOURCE_PATTERNS = (
    ("actionbar literal", r"[\"\']actionbar[\"\']"),
    ("action bar mode key", r"notify\.mode\.actionbar"),
)


def scan(root, suffixes, patterns):
    findings = []

    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.suffix not in suffixes:
            continue

        text = path.read_text(encoding="utf-8", errors="ignore")

        for line_number, line in enumerate(text.splitlines(), start=1):
            for name, pattern in patterns:
                if re.search(pattern, line):
                    findings.append("%s:%d %s -> %s" % (path, line_number, name, line.strip()))

    return findings


def cooldown_problems():
    sender = NOTIFY_SENDER.read_text(encoding="utf-8")
    constants = CONSTANTS.read_text(encoding="utf-8")
    wager = (SOURCE_ROOT / "com" / "devilbonegambler" / "wager" / "WagerService.java").read_text(encoding="utf-8")
    altar = (SOURCE_ROOT / "com" / "devilbonegambler" / "altar" / "AltarService.java").read_text(encoding="utf-8")
    commands = (SOURCE_ROOT / "com" / "devilbonegambler" / "commands" / "ModCommands.java").read_text(encoding="utf-8")
    actions = (SOURCE_ROOT / "com" / "devilbonegambler" / "client" / "ClientActions.java").read_text(encoding="utf-8")
    client_notices = SOURCE_ROOT / "com" / "devilbonegambler" / "client" / "ClientNotices.java"
    problems = []

    if not re.search(r"NOTICE_COOLDOWN_MS\s*=\s*1000L", constants):
        problems.append("the notice cooldown is not one second")

    if "NOTICE_COOLDOWN_MS" not in sender:
        problems.append("the notice sender does not use the notice cooldown")

    if "sendFailure" not in sender or "suppressNotice" not in sender:
        problems.append("a repeated failure notice is no longer held back")

    if "ModConstants.REASON_TOO_FREQUENT" not in sender:
        problems.append("a throttled request is no longer rejected in silence")

    altar_screen = (SOURCE_ROOT / "com" / "devilbonegambler" / "client" / "gui" / "AltarScreen.java").read_text(
        encoding="utf-8")

    if "ModConstants.REASON_TOO_FREQUENT" not in altar_screen:
        problems.append("the altar screen still shows the throttled request reason")

    if "sendChatThrottled(player, KEY_NO_PLAYER)" not in wager:
        problems.append("the no-player wager hint is no longer held back")

    if "sendChatThrottled(player, KEY_ENTERED" not in wager:
        problems.append("the area-entry wager hint is no longer held back")

    if "sendChatThrottled(player, KEY_INTRUSION)" not in altar:
        problems.append("the altar intrusion warning is no longer held back")

    if 'sendFailure(player, "not_ready")' not in commands:
        problems.append("the not-ready command refusal is no longer held back")

    if not client_notices.is_file() or "NOTICE_COOLDOWN_MS" not in client_notices.read_text(encoding="utf-8"):
        problems.append("the client side has no notice cooldown")

    if "ClientNotices.sendThrottled" not in actions:
        problems.append("the client invalid-action hint bypasses the notice cooldown")

    if 'addMessage(Component.translatable("gui." + ModConstants.MOD_ID + ".altar.place_hint")' in actions:
        problems.append("the client place hint is printed without the notice cooldown")

    return problems


def dice_prompt_problems():
    # The one allowed action bar writer: the rolled point and colour above the hotbar.
    actions = (SOURCE_ROOT / "com" / "devilbonegambler" / "client" / "ClientActions.java").read_text(encoding="utf-8")
    problems = []

    if actions.count("setOverlayMessage(") != 1:
        problems.append("the dice prompt must be the only action bar call in ClientActions")

    start = actions.find("public static void showPointDisplay")

    if start < 0:
        problems.append("the dice prompt method is missing")
        return problems

    end = actions.find("\n    }", start)
    method = actions[start:end if end > 0 else len(actions)]

    for marker in ("gamblers", "devils", "color.", "setOverlayMessage("):
        if marker not in method:
            problems.append("the dice prompt does not build the expected line: missing " + marker)

    if "overflow" in method:
        problems.append("the dice prompt shows the overflow value again")

    if "getChat().addMessage" in method:
        problems.append("the dice prompt is back in the chat box")

    return problems


def main():
    findings = scan(SOURCE_ROOT, {".java"}, SOURCE_PATTERNS)
    findings += scan(RESOURCE_ROOT, {".json"}, RESOURCE_PATTERNS)
    allowed = [finding for finding in findings
               if "setOverlayMessage" in finding and "ClientActions.java" in finding]
    findings = [finding for finding in findings if finding not in allowed]

    if len(allowed) != 1:
        findings.append("expected exactly one allowed action bar call, found " + str(len(allowed)))

    findings += cooldown_problems()
    findings += dice_prompt_problems()

    if findings:
        print("FAILED: the action bar area must stay free of mod prompts")

        for finding in findings:
            print("  " + finding)

        return 1

    print("OK: only the dice point line writes above the hotbar, repeated refusals are held back")
    print("  scanned %d java patterns and %d resource patterns" % (len(SOURCE_PATTERNS), len(RESOURCE_PATTERNS)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

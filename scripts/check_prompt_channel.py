"""
Guards the promise that no mod prompt is ever written above the hotbar.

The action bar area belongs to the game itself: every mod prompt is expected on the chat box, the item tooltip or a
screen. This check scans the mod sources and resources for any API or configuration value that writes there, and
exits non zero when one is found, so a future change can not quietly reintroduce it.
"""

import pathlib
import re
import sys

SOURCE_ROOT = pathlib.Path("src/main/java")
RESOURCE_ROOT = pathlib.Path("src/main/resources")

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


def main():
    findings = scan(SOURCE_ROOT, {".java"}, SOURCE_PATTERNS)
    findings += scan(RESOURCE_ROOT, {".json"}, RESOURCE_PATTERNS)

    if findings:
        print("FAILED: the action bar area must stay free of mod prompts")

        for finding in findings:
            print("  " + finding)

        return 1

    print("OK: no mod prompt writes above the hotbar")
    print("  scanned %d java patterns and %d resource patterns" % (len(SOURCE_PATTERNS), len(RESOURCE_PATTERNS)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

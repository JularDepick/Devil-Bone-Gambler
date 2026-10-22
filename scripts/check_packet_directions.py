"""
Guards the two directions of the config payload.

Forge keys a channel's message handlers by message class, so registering one class twice keeps only the last handler.
That is exactly how the admin panel stopped opening once the config payload was split into parts: the server push was
encoded with the save side's index and delivered to the server handler on the client, where it was dropped. This check
keeps a message class tied to one direction and proves the push and the save use different classes.
"""

import pathlib
import re
import sys

NETWORK = pathlib.Path("src/main/java/com/devilbonegambler/network")
PACKETS = NETWORK / "packet"
MOD_NETWORK = NETWORK / "ModNetwork.java"
SERVER_HANDLER = NETWORK / "ServerRequestHandler.java"
CLIENT_HANDLER = pathlib.Path("src/main/java/com/devilbonegambler/client/ClientPacketHandler.java")
SCREEN = pathlib.Path("src/main/java/com/devilbonegambler/client/gui/AdminConfigScreen.java")

REGISTRATION = re.compile(r"CHANNEL\.registerMessage\(\s*index\+\+,\s*(\w+)\.class,(.*?)\)\s*;\n", re.S)


def read(path):
    return path.read_text(encoding="utf-8")


def registrations():
    text = read(MOD_NETWORK)
    found = []

    for match in REGISTRATION.finditer(text):
        body = match.group(2)
        names = re.findall(r"(\w+Packet)::(\w+)", body)
        direction = re.search(r"NetworkDirection\.([A-Z_]+)", body)
        found.append((match.group(1), names, direction.group(1) if direction else ""))

    return found


def main():
    problems = []

    if "index++" not in read(MOD_NETWORK):
        problems.append("ModNetwork no longer numbers its messages")

    registered = registrations()
    classes = [entry[0] for entry in registered]
    duplicates = sorted({name for name in classes if classes.count(name) > 1})

    if duplicates:
        problems.append("a message class is registered twice: %s" % ", ".join(duplicates))

    by_class = {entry[0]: entry for entry in registered}

    for expected, direction, handler in (
            ("ConfigPayloadPacket", "PLAY_TO_CLIENT", "handleFromServer"),
            ("ConfigPayloadWritePacket", "PLAY_TO_SERVER", "handleFromClient")):
        entry = by_class.get(expected)

        if entry is None:
            problems.append("%s is not registered" % expected)
            continue

        if expected not in [name for name, method in entry[1]]:
            problems.append("%s registers as %s" % (expected, entry[1]))

        if handler not in [method for name, method in entry[1]]:
            problems.append("%s is not handled by %s" % (expected, handler))

        if entry[2] != direction:
            problems.append("%s has direction %s, expected %s" % (expected, entry[2] or "(none)", direction))

    push = read(PACKETS / "ConfigPayloadPacket.java")
    save = read(PACKETS / "ConfigPayloadWritePacket.java")

    if "handleFromClient" in push:
        problems.append("the server push must not carry the save handler")

    if "handleFromServer" in save:
        problems.append("the save must not carry the push handler")

    if "ClientPacketHandler.handleConfigPayload" not in push:
        problems.append("the server push no longer feeds the client cache")

    if "ServerRequestHandler.handleConfigPayload" not in save:
        problems.append("the save no longer reaches the server assembler")

    if "public static void handleConfigPayload(ServerPlayer player, ConfigPayloadWritePacket message)" \
            not in read(SERVER_HANDLER):
        problems.append("the server assembler takes the wrong message type")

    if "ConfigPayloadPacket message" not in read(CLIENT_HANDLER):
        problems.append("the client cache takes the wrong message type")

    if "new ConfigPayloadWritePacket(domain, index, parts.size(), parts.get(index))" not in read(SCREEN):
        problems.append("the panel does not ship its save with the write packet")

    if "new ConfigPayloadPacket(" in read(SCREEN):
        problems.append("the panel ships its save with the push packet")

    for problem in problems:
        print("FAIL %s" % problem)

    if problems:
        return 1

    print("PASS message classes: %d, all registered once" % len(registered))
    print("PASS config push: ConfigPayloadPacket / handleFromServer / PLAY_TO_CLIENT")
    print("PASS config save: ConfigPayloadWritePacket / handleFromClient / PLAY_TO_SERVER")
    return 0


if __name__ == "__main__":
    sys.exit(main())

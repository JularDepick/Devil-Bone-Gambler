"""
Guards the interrupted config transfer.

A config payload travels in parts, so a disconnect in the middle of a save or a push leaves a half built buffer behind.
The buffer has to be dropped on both edges of a connection: a client that never sees a clean logout would otherwise
start the next server with the parts of the last one, and a retry that reuses the same part count would merge its head
with the tail of the attempt it replaces. This check proves the drop happens and that the retry restarts the buffer
instead of completing the old one.
"""

import pathlib
import re
import sys

CLIENT_CACHE = pathlib.Path("src/main/java/com/devilbonegambler/client/ClientConfigCache.java")
CLIENT_EVENTS = pathlib.Path("src/main/java/com/devilbonegambler/client/ClientSessionEvents.java")
SERVER_BUFFER = pathlib.Path("src/main/java/com/devilbonegambler/network/ServerConfigPayloads.java")
CHUNKS = pathlib.Path("src/main/java/com/devilbonegambler/network/ConfigChunks.java")


def read(path):
    return path.read_text(encoding="utf-8")


def handler_bodies(text):
    """Every method body that follows a subscription annotation, keyed by the event it takes."""
    bodies = {}

    for match in re.finditer(r"@SubscribeEvent\s+public static void (\w+)\(([^)]*)\)\s*\{(.*?)\n    \}",
            text, re.S):
        bodies[match.group(2).strip()] = match.group(3)

    return bodies


def main():
    problems = []

    for path in (CLIENT_CACHE, CLIENT_EVENTS, SERVER_BUFFER, CHUNKS):
        if not path.exists():
            problems.append("missing %s" % path)

    if problems:
        for problem in problems:
            print("FAIL %s" % problem)
        return 1

    events = read(CLIENT_EVENTS)
    cache = read(CLIENT_CACHE)
    server = read(SERVER_BUFFER)
    chunks = read(CHUNKS)

    bodies = handler_bodies(events)

    if not any("LoggingIn" in signature for signature in bodies):
        problems.append("the client does not clear its cache when a connection begins")

    if not any("LoggingOut" in signature for signature in bodies):
        problems.append("the client does not clear its cache when a connection ends")

    for signature, body in bodies.items():
        if "ClientConfigCache.clear()" not in body:
            problems.append("%s does not clear the config cache" % signature.strip())

    if "Dist.CLIENT" not in events:
        problems.append("the session events are not confined to the client")

    if "partIndex == 0" not in cache:
        problems.append("a retry does not restart the part buffer and can merge two transfers")

    if "ConfigChunks.isPlausible" not in cache:
        problems.append("the client accepts a part count the sender is not allowed to use")

    clear_calls = len(re.findall(r"ClientConfigCache\.clear\(\)", events))

    if clear_calls < 2:
        problems.append("clear() is not reached on both edges of the connection")

    if "PENDING.remove(event.getEntity().getUUID())" not in server:
        problems.append("the server keeps the parts of a player who left")

    server_bodies = handler_bodies(server)

    if not any("PlayerLoggedInEvent" in signature for signature in server_bodies):
        problems.append("the server does not start a session from an empty buffer")

    if not any("PlayerLoggedOutEvent" in signature for signature in server_bodies):
        problems.append("the server does not drop the buffer on logout")

    for signature, body in server_bodies.items():
        if "PENDING.remove" not in body:
            problems.append("%s keeps the half built buffer" % signature.strip())

    if "pending.partCount != message.partCount()" not in server:
        problems.append("a part of another transfer is accepted into the current buffer")

    if "MAX_PARTS" not in chunks or "partCount <= MAX_PARTS" not in chunks:
        problems.append("the part count has no ceiling")

    for problem in problems:
        print("FAIL %s" % problem)

    if problems:
        return 1

    print("PASS client buffer: dropped on login and on logout")
    print("PASS part buffer: a retry restarts at part zero")
    print("PASS server buffer: dropped on login and on logout, part count checked")
    return 0


if __name__ == "__main__":
    sys.exit(main())

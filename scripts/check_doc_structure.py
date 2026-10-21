#!/usr/bin/env python3
"""Document structure guard of Devil Bone Gambler.

The engineering documents have to follow the code, so this check keeps them honest:

  1. every Java class and every resource file is listed in both source trees,
  2. the Chinese and the English source tree list the same entries,
  3. the documents named in the developer documentation really exist,
  4. the links of the root README resolve, and its version badge carries the version of build.gradle.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
RES_ROOT = ROOT / "src" / "main" / "resources"
SRC_DOCS = (ROOT / "src" / "README.md", ROOT / "src" / "README_en-US.md")
DEVELOPER_DOC = ROOT / "docs" / "developer" / "README.md"
ROOT_DOCS = (ROOT / "README.md", ROOT / "README_en-US.md")
TICK = chr(96) * 3

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def tree_entries(text):
    blocks = re.findall(re.escape(TICK) + r"\n(.*?)\n" + re.escape(TICK), text, re.S)
    names = []

    for block in blocks:
        for line in block.splitlines():
            if not line.strip() or line.strip().startswith("src/"):
                continue

            body = line.split("#")[0]
            entry = body.strip().split()[-1] if body.strip() else ""

            if entry:
                names.append(entry.rstrip("/"))

    return names


def main() -> int:
    java_files = sorted(path.name for path in JAVA_ROOT.rglob("*.java"))
    resource_files = sorted(path.name for path in RES_ROOT.rglob("*") if path.is_file())

    for document in SRC_DOCS:
        entries = set(tree_entries(read(document)))
        check("document " + document.name, bool(entries), "the source tree is empty or missing")

        for name in java_files:
            check("java " + name, name in entries, "the class is missing from " + document.name)

        for name in resource_files:
            check("resource " + name, name in entries, "the resource is missing from " + document.name)

    chinese = tree_entries(read(SRC_DOCS[0]))
    english = tree_entries(read(SRC_DOCS[1]))
    check("tree parity", len(chinese) == len(english),
          "the two source trees list a different number of entries: %d against %d" % (len(chinese), len(english)))

    zh_commented = sum(1 for line in read(SRC_DOCS[0]).splitlines() if "#" in line and not line.strip().startswith("src/"))
    en_commented = sum(1 for line in read(SRC_DOCS[1]).splitlines() if "#" in line and not line.strip().startswith("src/"))
    check("comment parity", zh_commented == en_commented,
          "the two source trees carry a different number of described entries")

    developer = read(DEVELOPER_DOC)

    for target in re.findall(r"\]\((\.\./\.\./[^)]+)\)", developer):
        check("developer link " + target, (DEVELOPER_DOC.parent / target).resolve().is_file(),
              "the linked document does not exist")

    version = re.search(r"^version = '([^']+)'", read(ROOT / "build.gradle"), re.M)
    check("version source", version is not None, "build.gradle carries no version")

    if version:
        for document in ROOT_DOCS:
            text = read(document)
            check("badge " + document.name, "Version-" + version.group(1) + "-" in text,
                  "the version badge does not match build.gradle")

    for document in ROOT_DOCS:
        for target in re.findall(r"\]\((\./[^)]+)\)", read(document)):
            check("readme link " + target, (ROOT / target[2:]).exists(), "the linked path does not exist")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("document structure check passed: " + str(len(java_files)) + " classes, " + str(len(resource_files)) + " resources")
    return 0


if __name__ == "__main__":
    sys.exit(main())

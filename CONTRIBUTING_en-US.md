<div align="center">

# Contribution Guide

[简体中文](./CONTRIBUTING.md)
| [English]

</div>

Welcome to contribute code fixes, documentation improvements and feature suggestions to Devil Bone Gambler

## Contribution Guidelines

### Code Guidelines

- Keep the existing directory structure; do not add top-level directories without discussion
- Java code uses the Allman brace style, 4-space indentation and block comments
- Use only the API provided by the official Forge 1.20.1 mappings; do not introduce external dependencies
- When adding or changing visible text, also maintain the Simplified Chinese and the English language file under `src/main/resources/assets/devil_bone_gambler/lang/`, and keep their key names identical
- Put client-only code under `src/main/java/com/devilbonegambler/client/`; do not reference client classes from server-side code

### Commit Guidelines

- One pull request handles one kind of change, which keeps review simple
- Run `gradlew build` before submitting to confirm the build passes
- Describe the scope and the impact of the change in the pull request description

## How to Contribute

### 1. Submit an Issue
- [Open a new Issue](https://github.com/JularDepick/Devil-Bone-Gambler/issues/new)
- Describe the bug you found or state your expectations
- Submit the Issue

### 2. Contribute Code
- [Fork this project](https://github.com/JularDepick/Devil-Bone-Gambler/fork)
- Complete the code changes on your fork branch
- [Create a Pull Request in this project](https://github.com/JularDepick/Devil-Bone-Gambler/compare)
- Submit the PR and wait for review

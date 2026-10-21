<div align="center">

# Contributing

[简体中文](./CONTRIBUTING.md)
| [English]

</div>

Corrections, documentation improvements and feature proposals for Devil Bone Gambler are welcome

## Rules

### Code

- Keep the existing directory layout and add no top level directory without a prior discussion
- Java uses the Allman brace style, four space indentation and block comments
- Only use the API of the Forge 1.20.1 official mappings, and add no external dependency
- When a visible text is added or changed, update both the Simplified Chinese and the English file under `src/main/resources/assets/devil_bone_gambler/lang/` and keep the key names identical
- Client only code belongs under `src/main/java/com/devilbonegambler/client/`; server side code must not reference client classes
- Tuneable design details belong in `util/ModConstants.java`, not as magic numbers in the logic
- A new item, block or effect comes with its texture, model, language keys and tooltip, and an effect has to be visible to the sample space scan

### Documents

- The three engineering documents follow the code: the root `README.md` covers gameplay and commands, the [source directory structure](./src/README_en-US.md) lists every class, and the [developer documentation](./docs/developer/README_en-US.md) covers the build and the runtime files
- Chinese and English versions are maintained together, with the same structure and the same number of lines
- A new class or resource goes into the trees of `src/README.md` and `src/README_en-US.md`; `scripts/check_doc_structure.py` guards this
- A version change updates the files and line numbers recorded in the [version index](./version.index.md)

### Checks

- Run `gradlew build` before a commit and make sure it passes
- Then run the static guards under `scripts/`; every one of them has to exit with 0. The list is in the check scripts section of the [developer documentation](./docs/developer/README_en-US.md)
- A change to a gameplay mechanic has to be verified in game against the test checklist; the static checks are no substitute

### Commits

- One pull request handles one kind of change, which keeps the review simple
- Run `gradlew build` before committing
- Describe the scope and the impact of the change in the pull request

## How to contribute

### 1. Open an issue
- [New issue](https://github.com/JularDepick/Devil-Bone-Gambler/issues/new)
- Describe the bug you found or the behaviour you expect
- Submit the issue

### 2. Contribute code
- [Fork the repository](https://github.com/JularDepick/Devil-Bone-Gambler/fork)
- Make the change on your fork
- [Open a pull request](https://github.com/JularDepick/Devil-Bone-Gambler/compare)
- Wait for the review

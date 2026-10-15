# Developer Documentation

[简体中文](./README.md)
| [English]

## Build and Run

| Command | Description |
|:---:|:---:|
| `build.bat` | Builds with the gradle on PATH and lists the artifact |
| `gradlew build` | Builds the mod artifact into `build/libs/` |
| `gradlew runClient` | Starts the Minecraft client development environment |
| `gradlew runServer` | Starts the Minecraft server development environment |
| `gradlew clean` | Removes the build output |
| `gradlew --stop` | Stops the resident Gradle daemon |

The artifact is named `DevilBoneGambler-v<version>-mc-1.20.1-forge.jar`

The first build downloads the Forge and Minecraft dependencies; the dependency cache lives in the Gradle user directory

## Project Structure Conventions

| Location | Convention |
|:---:|:---:|
| `src/main/java/com/devilbonegambler/` | Source root, matching the modId `devil_bone_gambler` |
| `src/main/java/com/devilbonegambler/client/` | Client-only code, screens live in its `gui/` subpackage, server code must not reference it |
| `src/main/java/com/devilbonegambler/mixin/` | Mixin package, must match the `package` field of the mixin configuration |
| `src/main/java/com/devilbonegambler/util/ModConstants.java` | Central design detail constants, tune numbers here only |
| `src/main/java/com/devilbonegambler/util/AuthorInfo.java` | Author credit constants, screens and headers only reference the constants |
| `src/main/resources/` | Resource root, the resource namespace is `devil_bone_gambler` |
| `src/main/resources/assets/devil_bone_gambler/` | Models, textures, block states and language files |
| `src/main/resources/data/devil_bone_gambler/` | Data pack content, holding the altar loot table and the crafting recipes of the four items |
| `src/main/resources/mixins.devil_bone_gambler.json` | Mixin configuration, declares the mixin package and mixin class list |
| `src/main/resources/META-INF/mods.toml` | Mod metadata, the version number is injected by a build placeholder |

Empty directories are kept in git through a `.gitkeep` placeholder

## Server Runtime Files

| File | Location | Description |
|:---:|:---:|:---:|
| Prize pool | `<world save>/serverconfig/devil_bone_gambler/prize_pool.json` | Layered prize pool, created from the preset on first start; an entry command holds one vanilla command per line with an optional leading slash and supports player name, player position, altar position and ranged random position placeholders |
| Item values | `<world save>/serverconfig/devil_bone_gambler/item_values.json` | Item value table, created from the built in presets and the registry scan |
| Luck data | `<world save>/data/devil_bone_gambler_luck.dat` | Per player luck value |
| Dice usage data | `<world save>/data/devil_bone_gambler_dealer_usage.dat` | Per player consumed gambler's dice counter, used for the dealer spawn egg reward |

## Admin Entry

| Command | Permission level | Description |
|:---:|:---:|:---:|
| `/devilbonegambler config` | 2 | Sends both config domains to the caller and opens the config panel |
| `/devilbonegambler reload` | 2 | Reloads both config domains from disk |

## Version Number and Placeholders

The single source of the version number is `build.gradle`; `processResources` injects it into the `version` field of `mods.toml` at build time. See the [version index](../../version.index.md) for every occurrence

## Document List

| Document | Purpose |
|:---:|:---:|
| [README.md](../../README_en-US.md) | Project overview |
| [Source directory structure](../../src/README_en-US.md) | Source and resource layout with the responsibility of each package |
| [Contribution guide](../../CONTRIBUTING_en-US.md) | Contribution rules and ways to contribute |
| [Version index](../../version.index.md) | Where the version number appears |

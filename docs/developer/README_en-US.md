# Developer Documentation

[简体中文](./README.md)
| [English]

## Running and building

| Command | Notes |
|:---:|:---:|
| `build.bat` | Builds with the gradle on PATH and lists the artifact |
| `gradlew build` | Builds the mod, the artifact lands in `build/libs/` |
| `gradlew runClient` | Starts the Minecraft client development environment |
| `gradlew runServer` | Starts the Minecraft server development environment |
| `gradlew clean` | Removes the build output |
| `gradlew --stop` | Stops the resident Gradle daemon |

The artifact is named `DevilBoneGambler-v<version>-mc-1.20.1-forge.jar`

The first build downloads Forge and Minecraft, the dependency cache lives in the Gradle user directory

## Project layout conventions

| Location | Convention |
|:---:|:---:|
| `src/main/java/com/devilbonegambler/` | Source root, matching the modId `devil_bone_gambler` |
| `src/main/java/com/devilbonegambler/client/` | Client only code, screens live in its `gui/` subpackage; server side code must not reference it |
| `src/main/java/com/devilbonegambler/menu/` | Container menus; screens take slot coordinates from the layout constants of the menu class, menu types register in `registry/ModMenus.java` |
| `src/main/java/com/devilbonegambler/luck/` | Luck flow, the hidden fate model, the behaviour variable and the Child of Fortune lock |
| `src/main/java/com/devilbonegambler/mixin/` | Mixin classes; the package must match the `package` field of the mixin configuration |
| `src/main/java/com/devilbonegambler/util/ModConstants.java` | Design detail constants, tuning a value only changes this file |
| `src/main/java/com/devilbonegambler/util/AuthorInfo.java` | Author credit constant used by screens and comment headers |
| `src/main/resources/` | Resources, namespace `devil_bone_gambler` |
| `src/main/resources/assets/devil_bone_gambler/` | Models, textures, block states, language files and the effect icon |
| `src/main/resources/assets/devil_bone_gambler/manuals/` | Manual markdown, one file per manual and language, named `<item id>_<language>.md` |
| `src/main/resources/data/devil_bone_gambler/` | Data pack content: block loot tables and the five crafting recipes |
| `src/main/resources/mixins.devil_bone_gambler.json` | Mixin configuration, package and class list |
| `src/main/resources/META-INF/mods.toml` | Mod metadata, the version is injected by the build |

Empty directories are kept in git through a `.gitkeep` placeholder

## Server runtime files

| File | Location | Notes |
|:---:|:---:|:---:|
| Prize pools | `<save>/serverconfig/devil_bone_gambler/prize_pool.json` | Array of pools, each with an id, a name and a description; the two built in presets are written on first start; entry commands support multiple lines, an optional leading slash, player name, player position and altar position placeholders, a random position in range, the `%(n,m)` random integer, the `--R(n)` repeat and the sample space placeholders |
| Prize pool state | `<save>/serverconfig/devil_bone_gambler/prize_pool_state.json` | Active pool id and revision; a draw only reads the active pool |
| Offering values | `<save>/serverconfig/devil_bone_gambler/item_values.json` | Value table, written on first start from built in presets plus the registry scan |
| Player luck | `<save>/data/devil_bone_gambler_luck.dat` | Luck value per player, its phase origin in game ticks and the real time milliseconds of the save creation |
| Dice usage | `<save>/data/devil_bone_gambler_dealer_usage.dat` | Consumed gambler dice per player, used for the dealer spawn egg rewards |
| Random pools | `<save>/serverconfig/devil_bone_gambler/random_pools.json` | Sample space of the random prize tokens, rebuilt from the registries and overwritten on every world load, five pools |
| Sample spaces | `<save>/serverconfig/devil_bone_gambler/sample_spaces.json` | Candidates of the sample space placeholders, rebuilt the same way, 15 general types and 6 narrow ones |
| Admin access | `<save>/serverconfig/devil_bone_gambler/access.json` | Creator and admin names, stored as plain names; the first player to join an empty file becomes the creator, only the creator may add or remove admins |

## Admin entry points

| Command | Permission | Notes |
|:---:|:---:|:---:|
| `/devilbonegambler config` or `/dbgr config` | Creator and admins | Sends the config to the caller and opens the panel |
| `/devilbonegambler reload` or `/dbgr reload` | Creator and admins | Reads the config files from disk again |
| `/devilbonegambler run <command>` or `/dbgr run` | Creator and admins | Trials a command with placeholders in the chat box, without sample names |
| `/devilbonegambler luck <player> <value>` or `/dbgr luck` | Creator and admins | Sets the luck of one player to a value from 0 to 100, applied at once when online and on the next join when offline |

In game there is also the Shift plus D shortcut, which shares the same permission check and shows a neutral key name

## Check scripts

The Python scripts in `scripts/` are static guards: they read sources and resources, assert the conventions and exit with 0 on success. Run them before a commit

| Area | Example scripts |
|:---:|:---:|
| Models and textures | `check_dice_model.py`, `check_altar_model.py`, `check_dealer_model.py`, `check_manual_policy.py` |
| Language and translation boundary | `check_lang_keys.py`, `check_lang_coverage.py`, `check_translation_boundary.py`, `check_item_tooltips.py` |
| Placeholders and commands | `check_number_placeholders.py`, `check_repeat_placeholders.py`, `check_sample_placeholders.py`, `check_prompt_channel.py` |
| Gameplay mechanics | `check_luck_system.py`, `check_lucky_apple.py`, `check_placed_dice.py`, `check_altar_protection.py`, `check_recipes.py` |
| Screens and documents | `check_admin_layout.py`, `check_tooltip_delay.py`, `check_manuals.py`, `check_doc_structure.py` |

## Version and placeholders

The only source of the version is `build.gradle`; `processResources` injects it into the `version` field of `mods.toml`. Every occurrence is listed in the [version index](../../version.index.md)

## Documents

| Document | Purpose |
|:---:|:---:|
| [README_en-US.md](../../README_en-US.md) | Project overview and gameplay summary |
| [Source directory structure](../../src/README_en-US.md) | Source and resource layout with the role of every class |
| [Contributing guide](../../CONTRIBUTING_en-US.md) | Contribution rules and workflow |
| [Version index](../../version.index.md) | Where the version appears and on which lines |

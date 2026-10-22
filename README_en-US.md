<div align="center">

# Devil-Bone-Gambler

[![Version](https://img.shields.io/badge/Version-0.8.0-red)](https://github.com/JularDepick/Devil-Bone-Gambler/tree/v0.8.0)
[![Copyright](https://img.shields.io/badge/Copyright-JularDepick-0066AA)](./COPYRIGHT)
[![License](https://img.shields.io/badge/License-Apache--2.0-yellow)](./LICENSE)

[简体中文](./README.md) |
[English]

</div>

A Minecraft mod: roll a die to trigger a random reward or punishment. Every payout depends on your stake and your luck.

---

## Requirements

| Item | Requirement |
|:---:|:---:|
| Minecraft | 1.20.1 |
| Mod loader | Forge 47.4.23 |
| Dependencies | No third party dependency |
| Installation | Both the client and the server need it |

## Contents

### Items and blocks

| Name | Notes |
|:---:|:---|
| Gambler's Dice | Right click to draw; with another player nearby, sneaking and right clicking starts a wager |
| Devil's Dice | Right click to draw; inside an altar range the right click opens the offering screen |
| Devil's Altar | A placed altar gives strength to the players in its range, and it can also be worn on the head as if it stood where you stand |
| Offering Value Table | Right click to open a searchable value table with an item and a creature chapter |
| Lucky Apple | Eight gambler dice around one apple; eating it fills both bars and grants Child of Fortune |
| Gameplay, admin and creator manuals | Three shipped books handed out by role, never craftable |
| Placed die | Hold the placement key to set a die down, break it with the key held to take it back, and break it without the key to use it |
| Dealer spawn egg | Spawns a Gambler's Dealer, offered by the dice usage count and the creative tab |

### Effects

| Name | Notes |
|:---:|:---|
| Child of Fortune | Ten seconds after eating a Lucky Apple; the luck value is held at 100 and returns to the pre meal value afterwards |

## How it plays

- The point decides which region of the prize pool is drawn; a point above the region number leaves an overflow, and a higher overflow leans to the rare part of that region
- The colour comes from the luck value: a red point draws only red entries and a black point only black entries, so the colour already tells the rough result
- The point and its colour are shown in one line above the hotbar; every other prompt goes to the chat box
- Sneaking while holding a die marks the nearby altars with the Devil's Dice and the nearby dealers with the Gambler's Dice
- A wager is a countdown and its members cannot use gambler dice before it settles; a dealer wager is separate from a player wager and its stakes are held by the server
- The luck value is a hidden number from 0 to 100, shown in the top left corner and coloured by its band; it moves with time and with what you do
- A pile of eight gameplay manuals inside one altar range is turned into dice automatically
- Only an iron pickaxe or a better one takes an altar down; explosions and pistons do nothing

## Commands

| Command | Permission | Notes |
|:---:|:---:|:---:|
| `/devilbonegambler config` or `/dbgr config` | Creator and admins | Opens the config panel |
| `/devilbonegambler reload` or `/dbgr reload` | Creator and admins | Reads the config files again |
| `/devilbonegambler run <command>` or `/dbgr run` | Creator and admins | Trials a command with placeholders in the chat box |
| `/devilbonegambler luck <player> <value>` or `/dbgr luck` | Creator and admins | Sets the luck of one player to a value from 0 to 100 |

Shift plus D also opens the config panel in game, and the key is rebindable

## Configuration

Prize pools and offering values are controlled by server side config files: pools may exist side by side and be switched on demand; entry commands support multiple lines, position placeholders, the `%(n,m)` random integer, the `--R(n)` repeat and the sample space placeholders. Runtime file locations and the config field list are in the [developer documentation](./docs/developer/README_en-US.md)

## Compatibility

- Both the client and the server have to be 0.7.0; a client with another protocol is refused by the server
- Prize pool config moved to the multi pool layout in 0.7.0, an old file is backed up and replaced by the two built in presets
- The luck value is new in 0.7.0, so a player joining an old save starts at 80

## More documents

| Document | Purpose |
|:---:|:---:|
| [Developer documentation](./docs/developer/README_en-US.md) | Build, runtime files and the check scripts |
| [Source directory structure](./src/README_en-US.md) | Source and resource layout with the role of every class |
| [Version index](./version.index.md) | Where the version appears |

## Contributing

See [CONTRIBUTING_en-US.md](./CONTRIBUTING_en-US.md).

## Copyright

Copyright &copy; 2026 JularDepick

See [COPYRIGHT](./COPYRIGHT).

## License

This repository is released under the [Apache-2.0 license](./LICENSE).

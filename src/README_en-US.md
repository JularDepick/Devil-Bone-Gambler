# Source Directory Structure

[简体中文](./README.md)
| [English]

This directory is the mod's functional source directory; the remaining content in the repository root is auxiliary and explanatory.

## Java Sources

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java        # Mod entry, registers the deferred registries and the network channel
├── client/                      # Client-only code, reached only through DistExecutor
│   ├── ClientActions.java       # Client action entry, packet sending and screen opening
│   ├── ClientPacketHandler.java # Client side packet dispatch
│   └── gui/                     # Client screens
│       ├── AdminConfigScreen.java # Admin config panel
│       └── AltarScreen.java       # Offering screen
├── commands/                    # Command registration
│   └── ModCommands.java         # Admin command entry
├── config/                      # Server config models and file access
│   ├── DefaultPrizePool.java    # Preset used when the prize pool file is created
│   ├── ItemValueConfig.java     # Item value config model
│   ├── ItemValuePresets.java    # Built in baseline values for vanilla items
│   ├── NotifyConfig.java        # Result display settings model
│   ├── PrizeEntry.java          # Prize entry model
│   ├── PrizePoolConfig.java     # Prize pool model and validation
│   ├── PrizeRegion.java         # Prize region model
│   └── ServerConfigManager.java # Loading, writing, value scan and optimistic lock
├── data/                        # Save persistence
│   └── LuckData.java            # Per player luck value
├── draw/                        # Draw mechanic
│   ├── DrawEngine.java          # Server side draw pipeline
│   ├── DrawResult.java          # Draw outcome
│   ├── DrawSeed.java            # Random seed construction
│   ├── NotifySender.java        # Result text delivery
│   └── Offering.java            # Offering entry
├── item/                        # Items
│   ├── DevilsDiceItem.java      # Devil's Dice
│   ├── GamblersDiceItem.java    # Gambler's Dice
│   └── OfferingValueTableItem.java # Offering Value Table
├── mixin/                       # Mixin package, currently empty
├── network/                     # Network channel, protocol and messages
│   ├── ModNetwork.java          # Channel registration and send helpers
│   ├── RequestGuard.java        # Request sequence dedup and rate limit
│   ├── ServerRequestHandler.java # Server side request handling
│   └── packet/                  # Message packets
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── ConfigSyncPacket.java
│       ├── ConfigWriteRequestPacket.java
│       ├── ConfigWriteResponsePacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── OpenValueTablePacket.java
│       └── ValueTableRefreshRequestPacket.java
├── registry/                    # Item and creative tab registration
│   ├── ModCreativeTabs.java     # The dedicated mod tab
│   └── ModItems.java            # Item registration
└── util/                        # Shared helpers
    ├── AuthorInfo.java          # Author credit constants
    ├── ModConstants.java        # Central design detail constants
    ├── Placeholders.java        # Placeholder expansion for commands and texts
    ├── ValueTableBook.java      # Value table snapshot read and write
    └── ValueTableSnapshot.java  # Client side read-only view of the value table
```

## Resources

```
src/main/resources/
├── META-INF/
│   └── mods.toml                      # Mod metadata, the version number is injected by a build placeholder
├── assets/devil_bone_gambler/
│   ├── blockstates/                   # Block states, currently empty
│   ├── lang/
│   │   ├── en_us.json                 # English texts
│   │   └── zh_cn.json                 # Simplified Chinese texts
│   ├── models/
│   │   ├── block/                     # Block models, currently empty
│   │   └── item/
│   │       ├── devils_dice.json
│   │       ├── gamblers_dice.json
│   │       └── offering_value_table.json
│   └── textures/
│       ├── block/                     # Block textures, currently empty
│       └── item/
│           ├── devils_dice.png
│           ├── gamblers_dice.png
│           └── offering_value_table.png
├── data/devil_bone_gambler/
│   └── recipes/                       # Crafting recipes, currently empty
├── mixins.devil_bone_gambler.json     # Mixin configuration, declares the mixin package and mixin class list
└── pack.mcmeta                        # Resource pack metadata
```

## Conventions

- Empty directories are kept in git through a `.gitkeep` placeholder; delete the matching `.gitkeep` once the directory holds real files
- The Java package root is `com.devilbonegambler`, matching the modId `devil_bone_gambler`; mixin classes live in the `com.devilbonegambler.mixin` package, which must match the `package` field of the mixin configuration
- Client code lives under `client/`; common code may only reach it through `DistExecutor`, and server side code must not reference client classes directly
- Design detail constants live in `util/ModConstants.java`, the author credit lives in `util/AuthorInfo.java`
- Language files must be maintained in both Simplified Chinese and English with exactly the same key names
- Textures are 16x16 pixels, 32 bit with an alpha channel, without anti-aliasing or gradients
- Server config loading, validation, the value scan and the config sync are centralised in `config/ServerConfigManager.java`; the draw pipeline lives in `draw/DrawEngine.java` and `draw/RegionSelector.java`
- The config entry list and the runtime file layout are described in the [developer documentation](../docs/developer/README_en-US.md)

# Source Directory Structure

[简体中文](./README.md)
| [English]

This directory is the mod's functional source directory; the remaining content in the repository root is auxiliary and explanatory.

## Java Sources

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java            # Mod entry, registers the deferred registries and the network channel
├── altar/                           # Altar range service
│   └── AltarService.java            # Finds the nearest altar of a player and drives the periodic range effect
├── block/                           # Altar blocks
│   ├── DevilsAltarBlock.java        # The altar block, carrying the offering value and never opening a screen
│   └── DevilsAltarBlockEntity.java  # Altar block entity, owning the offering value and syncing it to the client
├── client/                          # Client-only code, reached only through DistExecutor
│   ├── ClientActions.java           # Client action entry, packet sending and screen opening
│   ├── ClientPacketHandler.java     # Client side packet dispatch
│   ├── gui/                         # Client screens
│   │   ├── AdminConfigScreen.java   # Admin config panel
│   │   └── AltarScreen.java         # Offering screen
│   └── render/                      # Client rendering
│       └── AltarTintHandler.java    # Screen space red tint over the altar range
├── commands/                        # Command registration
│   ├── CommandRunner.java           # Prize entry command execution and syntax validation
│   └── ModCommands.java             # Admin command entry
├── config/                          # Server config models and file access
│   ├── DefaultPrizePool.java        # Preset used when the prize pool file is created
│   ├── ItemValueConfig.java         # Item value config model
│   ├── ItemValuePresets.java        # Built in baseline values for vanilla items
│   ├── NotifyConfig.java            # Result display settings model
│   ├── PrizeEntry.java              # Prize entry model
│   ├── PrizePoolConfig.java         # Prize pool model and validation
│   ├── PrizeRegion.java             # Prize region model
│   └── ServerConfigManager.java     # Loading, writing, value scan and optimistic lock
├── data/                            # Save persistence
│   └── LuckData.java                # Per player luck value
├── draw/                            # Draw mechanic
│   ├── DiceRoll.java                # One dice roll, a point plus a red or black colour
│   ├── DrawEngine.java              # Server side draw pipeline
│   ├── DrawResult.java              # Draw outcome
│   ├── DrawSeed.java                # Random seed construction
│   ├── DropCollector.java           # Captures the drops of mobs killed as offerings
│   ├── NotifySender.java            # Result text delivery
│   ├── Offering.java                # Offering entry
│   ├── OfferingBundle.java          # Offering bundle of items, mobs and numeric offerings
│   └── RegionSelector.java          # Resolves the region hit by the point and the overflow
├── effect/                          # Mob effects
│   └── BloodlustEffect.java         # Altar range effect, drains food saturation faster
├── item/                            # Items
│   ├── DevilsAltarItem.java         # Devil's Altar, placeable and wearable on the head
│   ├── DevilsDiceItem.java          # Devil's Dice
│   ├── GamblersDiceItem.java        # Gambler's Dice
│   └── OfferingValueTableItem.java  # Offering Value Table
├── mixin/                           # Mixin package, currently empty
├── network/                         # Network channel, protocol and messages
│   ├── AltarStatusBroadcaster.java  # Periodically broadcasts the altar status to clients
│   ├── ModNetwork.java              # Channel registration and send helpers
│   ├── RequestGuard.java            # Request sequence dedup and rate limit
│   ├── ServerRequestHandler.java    # Server side request handling
│   └── packet/                      # Message packets
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── AltarStatusPacket.java
│       ├── ConfigSyncPacket.java
│       ├── ConfigWriteRequestPacket.java
│       ├── ConfigWriteResponsePacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── MobListPacket.java
│       ├── OpenValueTablePacket.java
│       ├── PointDisplayPacket.java
│       └── ValueTableRefreshRequestPacket.java
├── registry/                        # Registered objects
│   ├── ModBlockEntities.java        # Block entity registration
│   ├── ModBlocks.java               # Block registration
│   ├── ModCreativeTabs.java         # The dedicated mod tab
│   ├── ModEffects.java              # Mob effect registration
│   └── ModItems.java                # Item registration
├── util/                            # Shared helpers
│   ├── AuthorInfo.java              # Author credit constants
│   ├── CommandPlaceholders.java     # Prize command placeholder expansion and ranged random positions
│   ├── ItemValuation.java           # Per instance item valuation with enchant bonus and durability
│   ├── ModConstants.java            # Central design detail constants
│   ├── Placeholders.java            # Placeholder expansion for commands and texts
│   ├── ServerScheduler.java         # Delayed server task scheduling
│   ├── ValueTableBook.java          # Value table snapshot read and write
│   └── ValueTableSnapshot.java      # Client side read-only view of the value table
└── wager/                           # Wager mechanic
    ├── WagerArea.java               # Wager zone sphere definition and range test
    ├── WagerService.java            # Server side wager pipeline
    └── WagerSession.java            # Session state of one wager
```

## Resources

```
src/main/resources/
├── META-INF/
│   └── mods.toml                      # Mod metadata, the version number is injected by a build placeholder
├── assets/devil_bone_gambler/
│   ├── blockstates/
│   │   └── devils_altar.json          # Altar block state
│   ├── lang/
│   │   ├── en_us.json                 # English texts
│   │   └── zh_cn.json                 # Simplified Chinese texts
│   ├── models/
│   │   ├── block/
│   │   │   └── devils_altar.json      # Altar block model, 51 cuboids on a 128x256 atlas
│   │   └── item/
│   │       ├── devils_altar.json      # Altar item model, reusing the block model
│   │       ├── devils_dice.json
│   │       ├── gamblers_dice.json
│   │       └── offering_value_table.json
│   ├── shaders/
│   │   ├── post/
│   │   │   └── altar_tint.json        # Altar tint post chain
│   │   └── program/
│   │       ├── altar_tint.fsh         # Tint fragment shader
│   │       ├── altar_tint.json        # Tint shader program declaration
│   │       └── altar_tint.vsh         # Tint vertex shader
│   └── textures/
│       ├── block/
│       │   ├── devils_altar.png       # Altar block texture, 128x256 atlas
│       │   └── devils_altar_particle.png # Altar break particle texture, 16x16
│       └── item/
│           ├── devils_dice.png
│           ├── gamblers_dice.png
│           └── offering_value_table.png
├── data/
│   ├── devil_bone_gambler/
│   │   ├── loot_tables/
│   │   │   └── blocks/
│   │   │       └── devils_altar.json  # Altar loot table
│   │   └── recipes/                   # Crafting recipes
│   │       ├── devils_altar.json      # Devil's Altar
│   │       ├── devils_dice.json       # Devil's Dice
│   │       ├── gamblers_dice.json     # Gambler's Dice
│   │       └── offering_value_table.json # Offering Value Table
│   └── minecraft/
│       └── tags/
│           └── blocks/
│               ├── needs_iron_tool.json # Tag for blocks that need an iron tool
│               └── mineable/
│                   └── pickaxe.json     # Tag for blocks mineable with a pickaxe
├── mixins.devil_bone_gambler.json     # Mixin configuration, declares the mixin package and mixin class list
└── pack.mcmeta                        # Resource pack metadata
```

## Conventions

- Empty directories are kept in git through a `.gitkeep` placeholder; delete the matching `.gitkeep` once the directory holds real files
- The Java package root is `com.devilbonegambler`, matching the modId `devil_bone_gambler`; mixin classes live in the `com.devilbonegambler.mixin` package, which must match the `package` field of the mixin configuration
- Client code lives under `client/`; common code may only reach it through `DistExecutor`, and server side code must not reference client classes directly
- Design detail constants live in `util/ModConstants.java`, the author credit lives in `util/AuthorInfo.java`
- Language files must be maintained in both Simplified Chinese and English with exactly the same key names
- Textures are a multiple of 16 pixels, 32 bit with an alpha channel, without anti-aliasing or gradients; items and single face textures use 16x16, multi part block models use an atlas that is a multiple of 16
- A custom block model must declare a `particle` texture, otherwise the break particles fall back to the vanilla missing texture
- Server config loading, validation, the value scan and the config sync are centralised in `config/ServerConfigManager.java`; the draw pipeline lives in `draw/DrawEngine.java` and `draw/RegionSelector.java`
- The wager mechanic lives in `wager/`; the altar block, its block entity and the range service live in `block/` and `altar/`
- The shader programs and the post chain of the altar tint live in `assets/devil_bone_gambler/shaders/`
- The mining tool requirement of a block is carried both by the tags under `data/minecraft/tags/blocks/` and by the destroy progress check in code, one without the other is not enough
- The config entry list and the runtime file layout are described in the [developer documentation](../docs/developer/README_en-US.md)

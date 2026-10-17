# Source Directory Structure

[简体中文](./README.md)
| [English]

This directory is the mod's functional source directory; the remaining content in the repository root is auxiliary and explanatory.

## Java Sources

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java            # Mod entry, registers the deferred registries and the network channel
├── altar/                           # Altar range service
│   ├── AltarMobScan.java            # Scans the governing altar range and answers the creature list request
│   └── AltarService.java            # Finds the nearest altar of a player and drives the periodic range effect
├── block/                           # Altar blocks
│   ├── DevilsAltarBlock.java        # The altar block, carrying the offering value and never opening a screen
│   └── DevilsAltarBlockEntity.java  # Altar block entity, owning the offering value and syncing it to the client
├── client/                          # Client-only code, reached only through DistExecutor
│   ├── AdminPanelKey.java           # Admin panel shortcut, Shift plus D, registered so it can be rebound
│   ├── AdminPanelKeyInput.java      # Key trigger and panel request
│   ├── ClientActions.java           # Client action entry, packet sending and screen opening
│   ├── ClientPacketHandler.java     # Client side packet dispatch
│   ├── ClientSetup.java             # Client registration entry, registers the entity renderer
│   ├── gui/                         # Client screens
│   │   ├── AdminConfigScreen.java   # Admin config panel with table lists and control click multi select
│   │   ├── ConfirmButton.java       # Two step confirm button, the first click only relabels it
│   │   ├── DelayedTooltip.java      # Hover tooltip that appears after a delay
│   │   ├── IntSlider.java           # Integer slider with a fixed step
│   │   ├── ItemLabels.java          # Translated names and raw registry ids for items and creatures
│   │   ├── ScrollBar.java           # Draggable list scroll bar
│   │   ├── TableLayout.java         # Table column width algorithm and cell drawing
│   │   ├── AltarScreen.java         # Offering screen with a container backpack, sliders and the creature list
│   │   ├── DealerBetScreen.java     # Dealer bet box screen on real container slots
│   │   ├── DealerScreen.java        # Dealer nine grid screen
│   │   └── ValueTableScreen.java    # Searchable offering value table on the shared table rules
│   ├── model/                       # Mod owned entity model
│   │   └── GamblersDealerModel.java # Dealer humanoid model, nine cuboids on the vanilla shoulder and hip pivots
│   └── render/                      # Client rendering
│       ├── AltarTintHandler.java    # Screen space red tint over the altar range
│       └── GamblersDealerRenderer.java # Dealer renderer, using the mod owned model and the dealer uniform texture
├── commands/                        # Command registration
│   ├── CommandRunner.java           # Prize entry command execution and syntax validation
│   ├── ModCommands.java             # Admin command entry
│   └── RandomPoolSampler.java       # Random pool sampling, replaces a random token with one entry before execution
├── config/                          # Server config models and file access
│   ├── AccessConfig.java            # Management access, the creator and the admin names
│   ├── DefaultPrizePool.java        # Preset used when the prize pool file is created
│   ├── RandomPoolConfig.java        # Random pool model, rebuilt from the registries on world load
│   ├── ItemValueConfig.java         # Item value config model
│   ├── ItemValuePresets.java        # Built in baseline values for vanilla items
│   ├── NotifyConfig.java            # Result display settings model
│   ├── PrizeEntry.java              # Prize entry model
│   ├── PrizePoolConfig.java         # Prize pool model and validation
│   ├── PrizeRegion.java             # Prize region model
│   └── ServerConfigManager.java     # Loading, writing, value scan and optimistic lock
├── data/                            # Save persistence
│   ├── DealerUsageData.java         # Per player consumed dice counter
│   └── LuckData.java                # Per player luck value
├── dealer/                          # Dealer wager and reward
│   ├── DealerBetBox.java            # Bet box of one member
│   ├── DealerRewardService.java     # Dice usage counter and spawn egg reward
│   ├── DealerWagerService.java      # Server side dealer wager pipeline
│   └── DealerWagerSession.java      # Session state of one dealer wager
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
├── item/                            # Items
│   ├── DevilsAltarItem.java         # Devil's Altar, placeable and wearable on the head
│   ├── DevilsDiceItem.java          # Devil's Dice
│   ├── GamblersDiceItem.java        # Gambler's Dice
│   ├── ManualItem.java              # Manual item, right click opens the generic book screen
│   └── OfferingValueTableItem.java  # Offering Value Table
├── menu/                            # Container menus
│   ├── AltarMenu.java               # Offering container, grid plus player slots and the layout constants
│   └── DealerBetMenu.java           # Bet box container, bet slots plus player slots and the layout constants
├── manual/                          # Manuals
│   ├── ManualKind.java              # The three manual kinds, their content paths and the language mapping
│   └── ManualService.java           # Keeps the manuals in step with the role on every join
├── mixin/                           # Mixin package, currently empty
├── mob/                             # Gambler's Dealer
│   ├── DealerSpawnService.java      # Silent sunrise and sunset spawn
│   ├── DealerWanderGoal.java        # Light first, liquid second pathing and the stay
│   └── GamblersDealer.java          # Dealer entity, feeding and hosting cost
├── network/                         # Network channel, protocol and messages
│   ├── AltarStatusBroadcaster.java  # Periodically broadcasts the altar status to clients
│   ├── ModNetwork.java              # Channel registration and send helpers
│   ├── RequestGuard.java            # Request sequence dedup and rate limit
│   ├── ServerRequestHandler.java    # Server side request handling
│   └── packet/                      # Message packets
│       ├── AdminEditPacket.java
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── AltarOpenRequestPacket.java
│       ├── AltarStatusPacket.java
│       ├── ConfigSyncPacket.java
│       ├── ConfigWriteRequestPacket.java
│       ├── ConfigWriteResponsePacket.java
│       ├── DealerBetOpenRequestPacket.java
│       ├── DealerInteractPacket.java
│       ├── DealerPanelPacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── MobListPacket.java
│       ├── MobListRequestPacket.java
│       ├── OpenValueTablePacket.java
│       ├── PointDisplayPacket.java
│       ├── ValueTableRefreshRequestPacket.java
│       └── ValueTableSyncPacket.java
├── registry/                        # Registered objects
│   ├── ModBlockEntities.java        # Block entity registration
│   ├── ModBlocks.java               # Block registration
│   ├── ModCreativeTabs.java         # The dedicated mod tab
│   ├── ModEntities.java             # Entity type registration
│   ├── ModEntityAttributes.java     # Entity attribute registration
│   ├── ModItems.java                # Item registration
│   ├── ModMenus.java                # Menu type registration
│   └── ModModelLayers.java          # Client model layer keys
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
│   ├── manuals/                       # Markdown content of the three manuals
│   │   ├── admin_manual_en_us.md      # Admin manual, English
│   │   ├── admin_manual_zh_cn.md      # Admin manual, Simplified Chinese
│   │   ├── creator_manual_en_us.md    # Creator's manual, English, reusing the admin content
│   │   ├── creator_manual_zh_cn.md    # Creator's manual, Simplified Chinese, reusing the admin content
│   │   ├── gameplay_manual_en_us.md   # Gameplay manual, English
│   │   └── gameplay_manual_zh_cn.md   # Gameplay manual, Simplified Chinese
│   ├── models/
│   │   ├── block/
│   │   │   └── devils_altar.json      # Altar block model, 51 cuboids on a 128x256 atlas
│   │   └── item/
│   │       ├── admin_manual.json
│   │       ├── creator_manual.json
│   │       ├── devils_altar.json      # Altar item model, reusing the block model
│   │       ├── devils_dice.json
│   │       ├── gamblers_dealer_spawn_egg.json # Dealer spawn egg, using the vanilla generated spawn egg model
│   │       ├── gameplay_manual.json
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
│       ├── entity/
│       │   └── gamblers_dealer.png  # Dealer uniform texture, a 64x64 self drawn UV layout
│       └── item/
│           ├── admin_manual.png
│           ├── creator_manual.png
│           ├── devils_dice.png
│           ├── gamblers_dice.png
│           ├── gameplay_manual.png
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
- Language files must be maintained in both Simplified Chinese and English with exactly the same key names and placeholders, guarded by `scripts/check_lang_keys.py`
- Container menus live in `menu/` and `registry/ModMenus.java`; screens take their slot coordinates from the layout constants of the menu class, so the drawn labels and the real slots never drift apart
- Textures are a multiple of 16 pixels, 32 bit with an alpha channel, without anti-aliasing or gradients; items and single face textures use 16x16, multi part block models use an atlas that is a multiple of 16
- A custom block model must declare a `particle` texture, otherwise the break particles fall back to the vanilla missing texture
- Server config loading, validation, the value scan and the config sync are centralised in `config/ServerConfigManager.java`; the draw pipeline lives in `draw/DrawEngine.java` and `draw/RegionSelector.java`
- The wager mechanic lives in `wager/`; the altar block, its block entity and the range service live in `block/` and `altar/`
- The dealer entity, its pathing and the silent spawn live in `mob/`, the dealer wager and its reward live in `dealer/`; the dealer renderer uses the mod owned model of `client/model/` with the dealer uniform texture and keeps the vanilla shoulder and hip pivots, and the silent spawn rule runs on the server only and never reaches a client
- The random pool sample space is rebuilt from the registries and overwritten on disk on every world load; sampling lives in `commands/RandomPoolSampler.java` and the model in `config/RandomPoolConfig.java`
- The shader programs and the post chain of the altar tint live in `assets/devil_bone_gambler/shaders/`
- The mining tool requirement of a block is carried both by the tags under `data/minecraft/tags/blocks/` and by the destroy progress check in code, one without the other is not enough
- The config entry list and the runtime file layout are described in the [developer documentation](../docs/developer/README_en-US.md)

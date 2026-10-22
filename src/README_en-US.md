# Source Directory Structure

[简体中文](./README.md)
| [English]

This directory is the mod's functional source directory; the remaining content in the repository root is auxiliary and explanatory.

## Java Sources

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java                       # Mod entry, registers the deferred registries and the network channel
├── altar/
│   ├── AltarManualConversion.java              # Turns a pile of gameplay manuals inside an altar range into dice
│   ├── AltarMobScan.java                       # Scans the governing altar range and answers the creature list request
│   └── AltarService.java                       # Finds the nearest altar of a player and drives the periodic range effect
├── block/
│   ├── DealerGlowBlock.java
│   ├── DevilsAltarBlock.java                   # The altar block, carrying the offering value and never opening a screen
│   ├── DevilsAltarBlockEntity.java             # Altar block entity, owning the offering value and syncing it to the client
│   ├── PlacedDiceBlock.java                    # A die placed on the ground; every face shows its own point
│   └── PlacedDiceService.java                  # Placing a die and breaking a placed die
├── client/
│   ├── AdminPanelKey.java                      # Admin panel shortcut, Shift plus D, registered so it can be rebound
│   ├── AdminPanelKeyInput.java                 # Key trigger and panel request
│   ├── ClientActions.java                      # Client action entry, packet sending and screen opening
│   ├── ClientBehaviorState.java                # Client mirror of the behaviour state, informational only
│   ├── ClientLuck.java                         # Latest luck value pushed by the server, used by the corner line only
│   ├── ClientNotices.java
│   ├── ClientPacketHandler.java                # Client side packet dispatch
│   ├── ClientSetup.java                        # Client registration entry, registers the entity renderer
│   ├── DicePlacementKey.java                   # The placement key, Tab by default, and the left click hook on placed dice
│   ├── ItemTooltips.java                       # Tooltip lines of the items of this mod
│   ├── ModConfigScreen.java
│   ├── gui/
│   │   ├── AdminConfigScreen.java              # Admin config panel with table lists and control click multi select
│   │   ├── AltarScreen.java                    # Offering screen with a container backpack, sliders and the creature list
│   │   ├── ConfirmButton.java                  # Two step confirm button, the first click only relabels it
│   │   ├── DealerBetScreen.java                # Dealer bet box screen on real container slots
│   │   ├── DealerScreen.java                   # Dealer nine grid screen
│   │   ├── DelayedTooltip.java                 # Hover tooltip that appears after a delay
│   │   ├── IntSlider.java                      # Integer slider with a fixed step
│   │   ├── ItemLabels.java                     # Translated names and raw registry ids for items and creatures
│   │   ├── LuckOverlay.java                    # Luck line in the top left corner with its colour bands
│   │   ├── ManualScreen.java                   # The generic book screen serving every manual
│   │   ├── ScrollBar.java                      # Draggable list scroll bar
│   │   ├── TableLayout.java                    # Table column width algorithm and cell drawing
│   │   ├── ValueTableScreen.java               # Searchable offering value table on the shared table rules
│   ├── manual/
│   │   ├── ManualContent.java                  # Reads the markdown of a manual out of the mod resources
│   │   ├── ManualLine.java                     # One logical line of a manual before wrapping
│   │   ├── ManualParser.java                   # The small markdown subset the manuals are written in
│   │   ├── ManualRow.java                      # One drawn row of a manual page
│   │   ├── ManualText.java                     # One styled run of text inside a manual line
│   ├── model/
│   │   ├── GamblersDealerModel.java            # Dealer humanoid model, nine cuboids on the vanilla shoulder and hip pivots
│   └── render/
│       ├── AltarTintHandler.java               # Screen space red tint over the altar range
│       ├── AltarValueLabelRenderer.java
│       ├── DiceSightLayer.java                 # Dealer sight geometry capture, runs only once the entity animation is placed
│       ├── DiceSightRenderer.java              # Dice sight white silhouette of altars and dealers
│       ├── GamblersDealerRenderer.java         # Dealer renderer, using the mod owned model and the dealer uniform texture
│       ├── SightHalo.java
├── commands/
│   ├── CommandRunner.java                      # Prize entry command execution and syntax validation
│   ├── ModCommands.java                        # Admin command entry
│   ├── NumberPlaceholders.java                 # The %(n,m) random integer placeholder
│   ├── RandomPoolSampler.java                  # Random pool sampling, replaces a random token with one entry before execution
│   ├── RepeatPlaceholders.java                 # The --R(n) repeat suffix of a command line
│   ├── SampleNames.java                        # Sample names appended to the result line
│   └── SamplePlaceholders.java
├── config/
│   ├── AccessConfig.java                       # Management access, the creator and the admin names
│   ├── DefaultPrizePool.java                   # Preset used when the prize pool file is created
│   ├── DefaultPrizePoolData.java               # The two preset pools, generated by a script
│   ├── ItemValueConfig.java                    # Item value config model
│   ├── ItemValuePresets.java                   # Built in baseline values for vanilla items
│   ├── NotifyConfig.java                       # Result display settings model
│   ├── PrizeEntry.java                         # Prize entry model
│   ├── PrizePoolBook.java                      # Every prize pool plus the active pool pointer
│   ├── PrizePoolConfig.java                    # Prize pool model and validation
│   ├── PrizeRegion.java                        # Prize region model
│   ├── RandomPoolConfig.java                   # Random pool model, rebuilt from the registries on world load
│   ├── SampleSpaceConfig.java
│   └── ServerConfigManager.java                # Loading, writing, value scan and optimistic lock
├── data/
│   ├── DealerUsageData.java                    # Per player consumed dice counter
│   └── LuckData.java                           # Saved luck value and phase origin per player
├── dealer/
│   ├── DealerBetBox.java                       # Bet box of one member
│   ├── DealerRewardService.java                # Dice usage counter and spawn egg reward
│   ├── DealerWagerService.java                 # Server side dealer wager pipeline
│   └── DealerWagerSession.java                 # Session state of one dealer wager
├── draw/
│   ├── DiceRoll.java                           # One dice roll, a point plus a red or black colour
│   ├── DrawEngine.java                         # Server side draw pipeline
│   ├── DrawResult.java                         # Draw outcome
│   ├── DrawSeed.java                           # Random seed construction
│   ├── DropCollector.java                      # Captures the drops of mobs killed as offerings
│   ├── NotifySender.java                       # Result text delivery
│   ├── Offering.java                           # Offering entry
│   ├── OfferingBundle.java                     # Offering bundle of items, mobs and numeric offerings
│   └── RegionSelector.java                     # Resolves the region hit by the point and the overflow
├── effect/
│   └── LuckChildEffect.java                    # The Child of Fortune effect, which holds the luck value at its ceiling
├── item/
│   ├── DevilsAltarItem.java                    # Devil's Altar, placeable and wearable on the head
│   ├── DevilsDiceItem.java                     # Devil's Dice
│   ├── DiceProvenance.java                     # Player provenance recorded on a gambler's die
│   ├── GamblersDiceItem.java                   # Gambler's Dice
│   ├── LuckyAppleItem.java                     # Lucky Apple: refills both bars and grants Child of Fortune
│   ├── ManualItem.java                         # Manual item, right click opens the generic book screen
│   └── OfferingValueTableItem.java             # Offering Value Table
├── luck/
│   ├── FateModel.java                          # Hidden fate model, three channels stacked on the in game calendar
│   ├── LuckBehaviorEvents.java                 # Kill and death side of the behaviour variable
│   ├── LuckLockService.java                    # Lock and restore of the Child of Fortune value
│   ├── LuckService.java                        # Main luck flow, updated and synced every 200 ticks
│   └── SubjectiveBehaviorService.java          # Behaviour variable and its one trigger per round
├── manual/
│   ├── ManualKind.java                         # The three manual kinds, their content paths and the language mapping
│   └── ManualService.java                      # Keeps the manuals in step with the role on every join
├── menu/
│   ├── AltarMenu.java                          # Offering container, grid plus player slots and the layout constants
│   └── DealerBetMenu.java                      # Bet box container, bet slots plus player slots and the layout constants
├── mixin/
│   └── .gitkeep
├── mob/
│   ├── DealerSpawnService.java                 # Silent sunrise and sunset spawn
│   ├── DealerWanderGoal.java                   # Light first, liquid second pathing and the stay
│   └── GamblersDealer.java                     # Dealer entity, feeding and hosting cost
├── network/
│   ├── AltarStatusBroadcaster.java             # Periodically broadcasts the altar status to clients
│   ├── ModNetwork.java                         # Channel registration and send helpers
│   ├── RequestGuard.java                       # Request sequence dedup and rate limit
│   ├── ServerRequestHandler.java               # Server side request handling
│   └── packet/
│       ├── AdminEditPacket.java
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── AltarOpenRequestPacket.java
│       ├── AltarStatusPacket.java
│       ├── BehaviorStatePacket.java            # Behaviour state mirror
│       ├── ConfigSyncPacket.java
│       ├── ConfigWriteRequestPacket.java
│       ├── ConfigWriteResponsePacket.java
│       ├── DealerBetOpenRequestPacket.java
│       ├── DealerInteractPacket.java
│       ├── DealerPanelPacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DiceModifierPacket.java             # Key state mirror, fallback only
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── LuckSyncPacket.java                 # Server to client luck value
│       ├── MobListPacket.java
│       ├── MobListRequestPacket.java
│       ├── OpenValueTablePacket.java
│       ├── PlaceDicePacket.java                # Placement request
│       ├── PlacedDiceBreakPacket.java          # Break request of a placed die, carrying the key state
│       ├── PlacedDiceUsePacket.java            # Tells the client that the placed die was used
│       ├── PointDisplayPacket.java
│       ├── ValueTableRefreshRequestPacket.java
│       ├── ValueTableSyncPacket.java
├── registry/
│   ├── ModBlockEntities.java                   # Block entity registration
│   ├── ModBlocks.java                          # Block registration
│   ├── ModCreativeTabs.java                    # The dedicated mod tab
│   ├── ModEffects.java                         # Effect registration
│   ├── ModEntities.java                        # Entity type registration
│   ├── ModEntityAttributes.java                # Entity attribute registration
│   ├── ModItems.java                           # Item registration
│   ├── ModMenus.java                           # Menu type registration
│   └── ModModelLayers.java                     # Client model layer keys
├── util/
│   ├── AuthorInfo.java                         # Author credit constants
│   ├── CommandPlaceholders.java                # Prize command placeholder expansion and ranged random positions
│   ├── CommandSequence.java                    # Server wide count of executed command lines, one seed per line
│   ├── InventoryHandover.java
│   ├── ItemValuation.java                      # Per instance item valuation with enchant bonus and durability
│   ├── ModConstants.java                       # Central design detail constants
│   ├── OfferEligibility.java
│   ├── Placeholders.java                       # Placeholder expansion for commands and texts
│   ├── SampleContext.java
│   ├── ServerScheduler.java                    # Delayed server task scheduling
│   ├── ValueTableBook.java                     # Value table snapshot read and write
│   └── ValueTableSnapshot.java                 # Client side read-only view of the value table
└── wager/
    ├── WagerArea.java                          # Wager zone sphere definition and range test
    ├── WagerService.java                       # Server side wager pipeline
    └── WagerSession.java                       # Session state of one wager
```

## Resources

```
src/main/resources/
├── META-INF/
│   └── mods.toml                                  # Mod metadata, the version number is injected by a build placeholder
├── assets/
│   └── devil_bone_gambler/
│       ├── blockstates/
│       │   ├── devils_altar.json                  # Altar block state
│       │   ├── placed_devils_dice.json
│       │   ├── placed_gamblers_dice.json
│       ├── lang/
│       │   ├── en_us.json                         # English texts
│       │   ├── zh_cn.json                         # Simplified Chinese texts
│       ├── prize_pools/
│       │   ├── presets.json                       # Preset prize pools, generated by scripts/build_preset_pool.py
│       ├── manuals/
│       │   ├── admin_manual_en_us.md              # Admin manual, English
│       │   ├── admin_manual_zh_cn.md              # Admin manual, Simplified Chinese
│       │   ├── creator_manual_en_us.md            # Creator's manual, English, reusing the admin content
│       │   ├── creator_manual_zh_cn.md            # Creator's manual, Simplified Chinese, reusing the admin content
│       │   ├── gameplay_manual_en_us.md           # Gameplay manual, English
│       │   ├── gameplay_manual_zh_cn.md           # Gameplay manual, Simplified Chinese
│       ├── models/
│       │   ├── block/
│       │   │   ├── devils_altar.json              # Altar block model, 51 cuboids on a 128x256 atlas
│       │   │   ├── placed_devils_dice.json
│       │   │   ├── placed_gamblers_dice.json
│       │   ├── item/
│       │   │   ├── admin_manual.json
│       │   │   ├── creator_manual.json
│       │   │   ├── devils_altar.json              # Altar item model, reusing the block model
│       │   │   ├── devils_dice.json
│       │   │   ├── gamblers_dealer_spawn_egg.json # Dealer spawn egg, using the vanilla generated spawn egg model
│       │   │   ├── gamblers_dice.json
│       │   │   ├── gameplay_manual.json
│       │   │   ├── lucky_apple.json
│       │   │   ├── offering_value_table.json
│       ├── shaders/
│       │   ├── post/
│       │   │   ├── altar_tint.json                # Altar tint post chain
│       │   ├── program/
│       │   │   ├── altar_tint.fsh                 # Tint fragment shader
│       │   │   ├── altar_tint.json                # Tint shader program declaration
│       │   │   ├── altar_tint.vsh                 # Tint vertex shader
│       ├── textures/
│       │   ├── block/
│       │   │   ├── devils_altar.png               # Altar block texture, 128x256 atlas
│       │   │   ├── devils_altar_particle.png      # Altar break particle texture, 16x16
│       │   ├── entity/
│       │   │   ├── gamblers_dealer.png            # Dealer uniform texture, a 64x64 self drawn UV layout
│       │   ├── item/
│       │   │   ├── admin_manual.png
│       │   │   ├── creator_manual.png
│       │   │   ├── devils_dice.png
│       │   │   ├── gamblers_dice.png
│       │   │   ├── gameplay_manual.png
│       │   │   ├── lucky_apple.png
│       │   │   ├── offering_value_table.png
│       │   ├── mob_effect/
│       │   │   ├── luck_child.png
├── data/
│   ├── devil_bone_gambler/
│   │   ├── loot_tables/
│   │   │   ├── blocks/
│   │   │   │   ├── devils_altar.json              # Altar loot table
│   │   ├── recipes/
│   │   │   ├── devils_altar.json                  # Devil's Altar
│   │   │   ├── devils_dice.json                   # Devil's Dice
│   │   │   ├── gamblers_dice.json                 # Gambler's Dice
│   │   │   ├── lucky_apple.json
│   │   │   ├── offering_value_table.json          # Offering Value Table
│   └── minecraft/
│       ├── tags/
│       │   ├── blocks/
│       │   │   ├── dragon_immune.json             # Tag for blocks the ender dragon can not destroy
│       │   │   ├── mineable/
│       │   │   │   ├── pickaxe.json               # Tag for blocks mineable with a pickaxe
│       │   │   ├── needs_iron_tool.json           # Tag for blocks that need an iron tool
│       │   │   ├── wither_immune.json             # Tag for blocks the wither can not destroy
├── mixins.devil_bone_gambler.json                 # Mixin configuration, declares the mixin package and mixin class list
└── pack.mcmeta                                    # Resource pack metadata
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
- The luck value, the hidden fate model and the behaviour variable live in `luck/`, and the Child of Fortune effect lives in `effect/`
- Placing a die and breaking a placed die belong to `block/PlacedDiceService.java`; the break request carries the placement key state from the client
- This tree is compared against the sources by `scripts/check_doc_structure.py`, so a new class or resource has to be added here and in the Chinese version

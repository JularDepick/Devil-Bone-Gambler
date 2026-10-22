# Devil Bone Gambler

[English]
| [[简体中文](#恶魔骨赌徒)]

A Minecraft Forge mod for 1.20.1: roll a die to trigger a random reward or punishment. Every payout depends on your stake and your luck.

---

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/00.png" alt="A sunset with a Gambler's Dealer and a Devil's Altar" />

# Mod Usage

## Prerequisites

| Item | Requirement |
|:---:|:---:|
| Minecraft | 1.20.1 |
| Mod loader | Forge 47.4.23 |
| Java | 17 |
| Dependencies | None beyond Forge |
| Installation | Both the client and the server |

## Items and Blocks

### Gambler's Dice

- Item ID: `devil_bone_gambler:gamblers_dice`
- Crafting: 1 Redstone in the centre, 4 Planks around it and 4 Leather in the corners
- Usage:
  - Right-click to draw; the point decides the region and the colour decides which entries can come out
  - Sneak-right-click while another player is nearby to open a wager instead of a draw
  - While sneaking, marks the nearby dealers

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/11.png" alt="The Gambler's Dice and the Devil's Dice" />

### Devil's Dice

- Item ID: `devil_bone_gambler:devils_dice`
- Crafting: 1 Redstone in the centre, 4 Obsidian around it and 4 Netherrack in the corners
- Usage:
  - Right-click to draw; inside an altar range the draw opens the offering screen instead
  - While sneaking, marks the nearby altars

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/13.png" alt="A draw made with the Gambler's Dice" />

### Devil's Altar

- Block ID: `devil_bone_gambler:devils_altar`
- Crafting: 5 Obsidian, 2 Netherrack, 2 Glowstone Dust and 1 Redstone
- Usage:
  - Placed, it grants Strength to players within 6 blocks
  - It can also be worn in the head slot, which counts as an altar at your position
  - It cannot be placed inside another player's altar range
  - Only an iron pickaxe or better takes it down; explosions and pistons do nothing

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/14.png" alt="An altar worn in the head slot" />

### Offering Screen

- Open it with the Devil's Dice inside an altar range
- Put up to 9 stacks of offerings into the grid and submit them to draw
- Items and mobs from this mod are refused as offerings

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/15.png" alt="The altar offering screen" />

### Offering Value Table

- Item ID: `devil_bone_gambler:offering_value_table`
- Crafting: 1 Book over 1 Netherrack
- Usage: right-click to open a searchable table of item and mob values

### Lucky Apple

- Item ID: `devil_bone_gambler:lucky_apple`
- Crafting: 8 Gambler's Dice around 1 Apple
- Usage:
  - Eating it refills hunger and health and clears every negative effect
  - Eating while standing grants Child of Fortune for 15 seconds
  - Eating while sneaking adds 10 luck points

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/31.png" alt="Eating a Lucky Apple" />

### Manuals

- Item IDs: `gameplay_manual`, `admin_manual`, `creator_manual`
- Not craftable; handed out by role and restored if lost
- Eight gameplay manuals inside one altar range turn into dice

### Placed Dice

- Hold a die with the place key (Tab by default) and right-click a block to place it
- Mining a placed die without the place key uses it; mining with the place key takes it back
- Explosions and pistons cannot break a placed die

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/12.png" alt="Both dice placed on the ground" />

### Gambler's Dealer

- Spawn egg: `devil_bone_gambler:gamblers_dealer_spawn_egg`
- Picks up Gambler's Dice and eats them when hurt or hungry
- Sometimes drops Lucky Apples after picking dice up
- Fights other dealers that enter its territory

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/21.png" alt="A Gambler's Dealer seen from the front" />

### Dealer Wager

- Right-click a dealer with an empty hand to open its page, then create a wager from the page; creating one costs 1 Gambler's Dice
- During recruiting, right-click the dealer with an empty hand to join; up to 8 players in total
- Stakes are held by the server and paid out by colour when the countdown ends

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/22.png" alt="A dealer summoning a wager" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/23.png" alt="Two dealers fighting over territory" />

## Luck

- A hidden value from 0 to 100, shown in the top left corner and coloured by band
- It moves with time and with what you do
- Child of Fortune holds it at 100 for its duration

## Wagers between Players

- Sneak-right-click the Gambler's Dice while another player is nearby to open a wager
- The starter joins automatically and members cannot use the Gambler's Dice until it settles
- The wager runs on a countdown and is paid out when it ends

## Commands

| Command | Permission | Notes |
|:---:|:---:|:---:|
| `/devilbonegambler config` or `/dbgr config` | Creator and admins | Opens the config panel |
| `/devilbonegambler reload` or `/dbgr reload` | Creator and admins | Reads the config files again |
| `/devilbonegambler run <command>` or `/dbgr run` | Creator and admins | Trials a command with placeholders in the chat |
| `/devilbonegambler luck <player> <value>` or `/dbgr luck` | Creator and admins | Sets one player's luck to a value from 0 to 100 |

Shift plus D opens the config panel in game as well, and both keys are rebindable

# Mod Configuration

## Server

- Folder: `<world save>/serverconfig/devil_bone_gambler/`

| File | Purpose | Notes |
|:---:|:---:|:---:|
| `access.json` | Creator and admin names | The first player to join becomes the creator when the file is empty; names are case insensitive |
| `item_values.json` | Offering values | Default item and mob value, enchantment base and per ID values |
| `prize_pool.json` | Prize pools | Up to 32 pools, each with 1 to 6 regions and up to 512 entries per region |
| `prize_pool_state.json` | Active pool and revision | Written by the mod |
| `random_pools.json` | Random pools | Rebuilt on every world load; do not edit |
| `sample_spaces.json` | Sample spaces | Rebuilt on every world load; do not edit |

- Entry commands take multiple lines and support `${player}`, `${x}`, `${y}`, `${z}`, `%(n,m)`, `--R(n)`, `${random:...}` and the sample space placeholders
- Two presets are written when no config exists: Pure Survival and Creative Survival, each with 6 regions of 260 entries
- Saving carries a revision number, so a second admin cannot overwrite a newer state

## Client

- The client stores no config file
- Keys: Shift plus D for the config panel, Tab for placing dice; both are rebindable

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/41.png" alt="The prize pool page of the config panel" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/42.png" alt="The multi pool page of the config panel" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/43.png" alt="Editing config fields in the panel" />

# Mod Data

| Data | Location |
|:---:|:---:|
| Luck value | `<world save>/data/devil_bone_gambler_luck.dat` |
| Dice usage count | `<world save>/data/devil_bone_gambler_dealer_usage.dat` |

# Supported Versions

| Minecraft | Java | Forge | Artifact |
|:---:|:---:|:---:|:---:|
| `1.20.1` | `17` | `47.4.23` | `DevilBoneGambler-v0.8.0-mc-1.20.1-forge.jar` |

> Only Forge on Minecraft 1.20.1 is supported for now

# Language Support

- English
- Simplified Chinese

> The mod follows the game language automatically

# Notes

> The mod has to be installed on both the client and the server; a client with a different protocol is refused by the server

> The point and colour line is the only prompt shown above the hotbar; every other prompt goes to the chat box

> Altars can only be taken down with an iron pickaxe or better; explosions and pistons cannot break them

> Existing prize pool files are kept as they are and are not migrated; the presets are written only when no config exists

> **For issues and feature requests, visit the GitHub repository**: [https://github.com/JularDepick/Devil-Bone-Gambler](https://github.com/JularDepick/Devil-Bone-Gambler)

---

# 恶魔骨赌徒

[简体中文]
| [[English](#devil-bone-gambler)]

一款适用于 Minecraft 1.20.1 的 Forge 模组: 掷骰子来触发随机奖励或惩罚. 所有回报都取决于你的赌注和你的运气.

---

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/00.png" alt="落日下的赌徒的荷官与恶魔的祭坛" />

# 模组使用

## 前置要求

| 项 | 要求 |
|:---:|:---:|
| Minecraft | 1.20.1 |
| 模组加载器 | Forge 47.4.23 |
| Java | 17 |
| 前置模组 | 除 Forge 外无其他依赖 |
| 安装位置 | 客户端与服务端都要安装 |

## 物品与方块

### 赌徒的骰子

- 物品 ID: `devil_bone_gambler:gamblers_dice`
- 合成: 中心 1 个红石, 四周 4 个木板, 四角 4 个皮革
- 使用:
  - 右击抽取, 点数决定区域, 颜色决定能抽出哪些条目
  - 附近有其他玩家时潜行右击, 改为发起赌局而不是抽取
  - 潜行时标出附近的荷官

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/11.png" alt="两种骰子展示" />

### 恶魔的骰子

- 物品 ID: `devil_bone_gambler:devils_dice`
- 合成: 中心 1 个红石, 四周 4 个黑曜石, 四角 4 个下界岩
- 使用:
  - 右击抽取, 站在祭坛范围内时改为打开献祭界面
  - 潜行时标出附近的祭坛

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/13.png" alt="使用了赌徒的骰子" />

### 恶魔的祭坛

- 方块 ID: `devil_bone_gambler:devils_altar`
- 合成: 5 个黑曜石, 2 个下界岩, 2 个荧石粉与 1 个红石
- 使用:
  - 放下后给 6 格范围内的玩家力量效果
  - 也可以戴在头上, 等同于在你所在的位置放了一座祭坛
  - 不能放进其他玩家的祭坛范围
  - 只有铁镐或更高等级的镐子能挖下, 爆炸与活塞都无效

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/14.png" alt="头戴祭坛" />

### 献祭界面

- 站在祭坛范围内使用恶魔的骰子打开
- 最多放入 9 组祭品, 提交后抽取
- 本模组的物品与生物不能作为祭品

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/15.png" alt="祭坛献祭页面" />

### 祭品价值表

- 物品 ID: `devil_bone_gambler:offering_value_table`
- 合成: 书在上, 下界岩在下
- 使用: 右击打开可搜索的物品与生物价值表

### 幸运苹果

- 物品 ID: `devil_bone_gambler:lucky_apple`
- 合成: 8 个赌徒的骰子围着一个苹果
- 使用:
  - 食用回满饱食度与生命值并清除全部负面状态
  - 站立食用获得 15 秒 幸运之子
  - 蹲下食用增加 10 点幸运值

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/31.png" alt="食用幸运苹果" />

### 手册

- 物品 ID: `gameplay_manual`, `admin_manual`, `creator_manual`
- 不可合成, 按身份发放, 丢失后会被补回
- 同一祭坛范围内放够 8 本玩法手册会被自动换成骰子

### 放置的骰子

- 按住放置键 (默认 Tab) 持有骰子右击方块即可放下
- 不按放置键挖掘等同于使用, 按住放置键挖掘则取回
- 爆炸与活塞都不能破坏放置的骰子

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/12.png" alt="两种骰子放置展示" />

### 赌徒的荷官

- 生物蛋: `devil_bone_gambler:gamblers_dealer_spawn_egg`
- 会捡起赌徒的骰子, 受伤或饥饿时吃掉它们
- 捡起骰子后有时会掉落幸运苹果
- 领地内出现别的荷官时会打起来

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/21.png" alt="赌徒的荷官-正面照" />

### 荷官的赌局

- 空手右击荷官打开页面, 在页面里创建赌局, 创建时消耗 1 个赌徒的骰子
- 招募期内空手右击荷官即可加入, 最多 8 名玩家
- 赌注由服务端托管, 倒计时结束后按颜色分配

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/22.png" alt="赌徒的荷官-正在召唤赌局" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/23.png" alt="赌徒的荷官-竞争攻击中" />

## 幸运值

- 0 到 100 的隐藏数值, 显示在左上角并按区间变色
- 会随时间与你的行为变化
- 幸运之子 期间锁定为 100

## 玩家之间的赌局

- 附近有其他玩家时潜行右击赌徒的骰子即可发起
- 发起者自动加入, 成员在结算前不能使用赌徒的骰子
- 赌局按倒计时运行, 结束后结算

## 指令

| 指令 | 权限 | 说明 |
|:---:|:---:|:---:|
| `/devilbonegambler config` 或 `/dbgr config` | 创建者与管理员 | 打开配置面板 |
| `/devilbonegambler reload` 或 `/dbgr reload` | 创建者与管理员 | 重新读取配置文件 |
| `/devilbonegambler run <指令>` 或 `/dbgr run` | 创建者与管理员 | 在聊天框试跑带占位符的指令 |
| `/devilbonegambler luck <玩家> <数值>` 或 `/dbgr luck` | 创建者与管理员 | 把指定玩家的幸运值设为 0 到 100 内的值 |

游戏内按 Shift 加 D 也能打开配置面板, 两个键位都可以改绑

# 模组配置

## 服务端

- 目录: `<世界存档>/serverconfig/devil_bone_gambler/`

| 文件 | 作用 | 说明 |
|:---:|:---:|:---:|
| `access.json` | 创建者与管理员名单 | 文件为空时由第一位进入服务器的玩家成为创建者, 名字比较忽略大小写 |
| `item_values.json` | 祭品价值表 | 默认物品与生物价值, 附魔价值基数与逐条目价值 |
| `prize_pool.json` | 奖池数组 | 最多 32 套, 每套 1 到 6 个区域, 每个区域最多 512 条 |
| `prize_pool_state.json` | 生效奖池与修订号 | 由模组维护 |
| `random_pools.json` | 随机池 | 每次世界加载后重建, 不要手改 |
| `sample_spaces.json` | 样本空间 | 每次世界加载后重建, 不要手改 |

- 条目指令支持多行, 以及 `${player}`, `${x}`, `${y}`, `${z}`, `%(n,m)`, `--R(n)`, `${random:...}` 与样本空间占位符
- 配置不存在时写入两套预设: 纯生存与创意生存, 每套 6 个区域各 260 条
- 保存携带修订号, 因此第二个管理员无法覆盖更新的状态

## 客户端

- 客户端不保存配置文件
- 键位: Shift 加 D 为配置面板, Tab 为放置骰子, 都可以改绑

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/41.png" alt="奖池区域配置页面" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/42.png" alt="多奖池配置页面" />

<img src="https://raw.githubusercontent.com/JularDepick/Devil-Bone-Gambler/main/curseforge/43.png" alt="管理员编辑页面" />

# 模组数据

| 数据 | 位置 |
|:---:|:---:|
| 幸运值 | `<世界存档>/data/devil_bone_gambler_luck.dat` |
| 骰子使用计数 | `<世界存档>/data/devil_bone_gambler_dealer_usage.dat` |

# 支持的版本

| Minecraft | Java | Forge | 产物 |
|:---:|:---:|:---:|:---:|
| `1.20.1` | `17` | `47.4.23` | `DevilBoneGambler-v0.8.0-mc-1.20.1-forge.jar` |

> 目前只支持 Minecraft 1.20.1 上的 Forge

# 多语言支持

- 简体中文
- English

> 模组语言随游戏语言自动变更

# 附注

> 客户端与服务端都要安装本模组, 协议号不同的客户端会被服务端拒绝

> 骰子点数与颜色是唯一显示在快捷栏上方的一行, 其余提示都进聊天框

> 祭坛只有铁镐或更高等级的镐子能挖下, 爆炸与活塞都无效

> 已有的奖池文件原样保留, 不做迁移, 只有配置不存在时才写入预设

> **如需反馈问题或建议功能, 请前往 GitHub 仓库**: [https://github.com/JularDepick/Devil-Bone-Gambler](https://github.com/JularDepick/Devil-Bone-Gambler)

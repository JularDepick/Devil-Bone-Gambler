# 源码目录结构

[简体中文]
| [[English]](./README_en-US.md)

本目录是模组的功能性源码目录, 根目录的其余内容为辅助性与说明性内容

## Java 源码

```
├── DevilBoneGambler.java            # 模组入口, 注册延迟注册器与网络通道
├── altar/                           # 祭坛范围服务
│   └── AltarService.java            # 定位玩家最近的祭坛并驱动周期范围效果
├── block/                           # 祭坛方块
│   ├── DevilsAltarBlock.java        # 祭坛方块, 承载献祭价值且不打开界面
│   └── DevilsAltarBlockEntity.java  # 祭坛方块实体, 持有献祭价值并与客户端同步
├── client/                          # 客户端专属代码, 仅经 DistExecutor 触达
│   ├── ClientActions.java           # 客户端动作入口, 发包与打开界面
│   ├── ClientPacketHandler.java     # 客户端侧网络包分发
│   ├── ClientSetup.java             # 客户端注册入口, 注册实体渲染器
│   ├── gui/                         # 客户端界面
│   │   ├── AdminConfigScreen.java   # 管理员配置面板
│   │   ├── AltarScreen.java         # 献祭界面
│   │   ├── DealerBetScreen.java     # 荷官赌注箱界面
│   │   └── DealerScreen.java        # 荷官九宫格界面
│   └── render/                      # 客户端渲染
│       ├── AltarTintHandler.java    # 祭坛范围的屏幕空间后处理红色遮罩
│       └── GamblersDealerRenderer.java # 荷官渲染, 复用村民模型与村民贴图
├── commands/                        # 命令注册
│   ├── CommandRunner.java           # 奖池条目指令执行与语法校验
│   └── ModCommands.java             # 管理员命令入口
├── config/                          # 服务端配置模型与读写
│   ├── DefaultPrizePool.java        # 首次生成奖池配置时使用的预设
│   ├── ItemValueConfig.java         # 物品价值配置模型
│   ├── ItemValuePresets.java        # 内置原版物品价值基准表
│   ├── NotifyConfig.java            # 条目结果展示配置模型
│   ├── PrizeEntry.java              # 奖池条目模型
│   ├── PrizePoolConfig.java         # 奖池配置模型与校验
│   ├── PrizeRegion.java             # 奖池区域模型
│   └── ServerConfigManager.java     # 配置加载, 写盘, 扫描兜底与乐观锁
├── data/                            # 存档持久化
│   ├── DealerUsageData.java         # 玩家骰子使用计数存档
│   └── LuckData.java                # 玩家运气值存档数据
├── dealer/                          # 荷官赌局与奖励
│   ├── DealerBetBox.java            # 单名成员的赌注箱
│   ├── DealerRewardService.java     # 骰子使用计数与生物蛋发放
│   ├── DealerWagerService.java      # 服务端荷官赌局主流程
│   └── DealerWagerSession.java      # 单场荷官赌局的会话状态
├── draw/                            # 抽取机制
│   ├── DiceRoll.java                # 一次掷骰, 点数与红黑颜色
│   ├── DrawEngine.java              # 服务端抽取主流程
│   ├── DrawResult.java              # 抽取结果
│   ├── DrawSeed.java                # 随机种子构造
│   ├── DropCollector.java           # 捕获作为祭品被杀生物的掉落物
│   ├── NotifySender.java            # 结果文本发送
│   ├── Offering.java                # 祭品条目
│   ├── OfferingBundle.java          # 献祭包, 物品与生物与数值献祭项
│   └── RegionSelector.java          # 由点数命中区域并计算溢出值
├── effect/                          # 状态效果
│   └── BloodlustEffect.java         # 祭坛范围效果, 加速饱食度消耗
├── item/                            # 物品
│   ├── DevilsAltarItem.java         # 恶魔的祭坛, 可放置也可佩戴在头部
│   ├── DevilsDiceItem.java          # 恶魔的骰子
│   ├── GamblersDiceItem.java        # 赌徒的骰子
│   └── OfferingValueTableItem.java  # 祭品价值表
├── mixin/                           # 混入类目录, 当前为空
├── mob/                             # 赌徒的荷官
│   ├── DealerSpawnService.java      # 日出日落静默生成
│   ├── DealerWanderGoal.java        # 光照优先与远离液体的寻路偏好与停留
│   └── GamblersDealer.java          # 荷官实体, 进食与主持消耗
├── network/                         # 网络通道, 协议与消息包
│   ├── AltarStatusBroadcaster.java  # 周期向客户端广播祭坛状态
│   ├── ModNetwork.java              # 通道注册与发送封装
│   ├── RequestGuard.java            # 请求序号去重与频率限制
│   ├── ServerRequestHandler.java    # 服务端请求处理
│   └── packet/                      # 消息包
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── AltarStatusPacket.java
│       ├── ConfigSyncPacket.java
│       ├── ConfigWriteRequestPacket.java
│       ├── ConfigWriteResponsePacket.java
│       ├── DealerBetMovePacket.java
│       ├── DealerInteractPacket.java
│       ├── DealerPanelPacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── MobListPacket.java
│       ├── OpenValueTablePacket.java
│       ├── PointDisplayPacket.java
│       ├── ValueTableRefreshRequestPacket.java
│       └── ValueTableSyncPacket.java
├── registry/                        # 注册项
│   ├── ModBlockEntities.java        # 方块实体注册
│   ├── ModBlocks.java               # 方块注册
│   ├── ModCreativeTabs.java         # 模组专属物品页
│   ├── ModEffects.java              # 状态效果注册
│   ├── ModEntities.java             # 实体类型注册
│   ├── ModEntityAttributes.java     # 实体属性注册
│   └── ModItems.java                # 物品注册
├── util/                            # 通用工具
│   ├── AuthorInfo.java              # 作者信息常量
│   ├── CommandPlaceholders.java     # 条目指令占位符解析与范围随机坐标
│   ├── ItemValuation.java           # 物品实例估值, 含附魔加成与耐久折算
│   ├── ModConstants.java            # 设计细节常量集中定义
│   ├── Placeholders.java            # 指令与文本占位符展开
│   ├── ServerScheduler.java         # 服务端延迟任务调度
│   ├── ValueTableBook.java          # 价值表快照读写
│   └── ValueTableSnapshot.java      # 客户端侧价值表只读视图
└── wager/                           # 赌局机制
    ├── WagerArea.java               # 赌区球体定义与范围判定
    ├── WagerService.java            # 服务端赌局主流程
    └── WagerSession.java            # 单场赌局的会话状态
```

## 资源

```
src/main/resources/
├── META-INF/
│   └── mods.toml                      # 模组元数据, 版本号由构建占位符注入
├── assets/devil_bone_gambler/
│   ├── blockstates/
│   │   └── devils_altar.json          # 祭坛方块状态
│   ├── lang/
│   │   ├── en_us.json                 # 英文文本
│   │   └── zh_cn.json                 # 简体中文文本
│   ├── models/
│   │   ├── block/
│   │   │   └── devils_altar.json      # 祭坛方块模型, 51 个长方体与 128x256 图集
│   │   └── item/
│   │       ├── devils_altar.json      # 祭坛物品模型, 复用方块模型
│   │       ├── devils_dice.json
│   │       ├── gamblers_dealer_spawn_egg.json # 荷官生物蛋, 沿用原版生成式生物蛋模型
│   │       ├── gamblers_dice.json
│   │       └── offering_value_table.json
│   ├── shaders/
│   │   ├── post/
│   │   │   └── altar_tint.json        # 祭坛遮罩后处理链
│   │   └── program/
│   │       ├── altar_tint.fsh         # 遮罩片元着色器
│   │       ├── altar_tint.json        # 遮罩着色器程序声明
│   │       └── altar_tint.vsh         # 遮罩顶点着色器
│   └── textures/
│       ├── block/
│       │   ├── devils_altar.png       # 祭坛方块贴图, 128x256 图集
│       │   └── devils_altar_particle.png # 祭坛破坏粒子贴图, 16x16
│       └── item/
│           ├── devils_dice.png
│           ├── gamblers_dice.png
│           └── offering_value_table.png
├── data/
│   ├── devil_bone_gambler/
│   │   ├── loot_tables/
│   │   │   └── blocks/
│   │   │       └── devils_altar.json  # 祭坛掉落表
│   │   └── recipes/                   # 合成配方
│   │       ├── devils_altar.json      # 恶魔的祭坛
│   │       ├── devils_dice.json       # 恶魔的骰子
│   │       ├── gamblers_dice.json     # 赌徒的骰子
│   │       └── offering_value_table.json # 祭品价值表
│   └── minecraft/
│       └── tags/
│           └── blocks/
│               ├── needs_iron_tool.json # 需要铁质工具的方块标签
│               └── mineable/
│                   └── pickaxe.json     # 可被镐挖掘的方块标签
├── mixins.devil_bone_gambler.json     # 混入配置, 声明混入包与混入类清单
└── pack.mcmeta                        # 资源包元数据
```

## 约定

- 空目录以 `.gitkeep` 占位, 便于 git 追踪目录结构; 目录内新增文件后即可删除对应的 `.gitkeep`
- Java 包根为 `com.devilbonegambler`, 与 modId `devil_bone_gambler` 对应; 混入类所在包为 `com.devilbonegambler.mixin`, 必须与混入配置中的 `package` 字段一致
- 客户端代码集中在 `client/` 下, 公共代码只能经 `DistExecutor` 触达, 服务端代码不得直接引用客户端类
- 设计细节常量集中在 `util/ModConstants.java`, 作者信息集中在 `util/AuthorInfo.java`
- 语言文件需要同时维护简体中文与英文两份, 两者键名必须完全一致
- 贴图规格为 16 的整数倍像素, 32 位带透明通道, 不使用抗锯齿与渐变; 物品与单面贴图使用 16x16, 多部件方块模型使用 16 的整数倍图集
- 自定义方块模型需要在 `textures` 中声明 `particle` 贴图, 否则破坏粒子会回退到原版缺失贴图
- 服务端配置的加载, 校验, 扫描兜底与同步集中在 `config/ServerConfigManager.java`; 抽取流程集中在 `draw/DrawEngine.java` 与 `draw/RegionSelector.java`
- 赌局机制集中在 `wager/`; 祭坛的方块, 方块实体与范围服务集中在 `block/` 与 `altar/`
- 荷官的实体, 寻路与静默生成集中在 `mob/`, 荷官赌局与奖励集中在 `dealer/`; 荷官渲染复用原版村民模型与村民贴图, 静默生成规则只在服务端执行且不下发到客户端
- 祭坛遮罩的着色器程序与后处理链位于 `assets/devil_bone_gambler/shaders/`
- 方块的挖掘工具要求同时由 `data/minecraft/tags/blocks/` 下的标签与代码中的破坏进度判定承担, 两者缺一不可
- 配置项清单与运行期文件位置见 [开发者文档](../docs/developer/README.md)
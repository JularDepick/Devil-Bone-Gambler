# 源码目录结构

[简体中文]
| [[English]](./README_en-US.md)

本目录是模组的功能性源码目录, 根目录的其余内容为辅助性与说明性内容

## Java 源码

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java                       # 模组入口, 注册延迟注册器与网络通道
├── altar/
│   ├── AltarManualConversion.java              # 祭坛范围内成堆的玩法手册换成骰子
│   ├── AltarMobScan.java                       # 治辖祭坛范围内的生物扫描与列表回发
│   └── AltarService.java                       # 定位玩家最近的祭坛并驱动周期范围效果
├── block/
│   ├── DealerGlowBlock.java
│   ├── DevilsAltarBlock.java                   # 祭坛方块, 承载献祭价值且不打开界面
│   ├── DevilsAltarBlockEntity.java             # 祭坛方块实体, 持有献祭价值并与客户端同步
│   ├── PlacedDiceBlock.java                    # 放置在地上的骰子方块, 六个面各自显示自己的点数
│   └── PlacedDiceService.java                  # 放置骰子与破坏放置骰子的服务
├── client/
│   ├── AdminPanelKey.java                      # 管理面板键位 Shift 加 D, 经原版按键注册因此可改绑
│   ├── AdminPanelKeyInput.java                 # 按键触发与开面板请求
│   ├── ClientActions.java                      # 客户端动作入口, 发包与打开界面
│   ├── ClientBehaviorState.java                # 行为变量的客户端镜像, 只作信息展示
│   ├── ClientLuck.java                         # 客户端持有的最新幸运值, 只供角标显示
│   ├── ClientConfigCache.java                  # 配置分块的拼装与面板打开时机
│   ├── ClientNotices.java
│   ├── ClientPacketHandler.java                # 客户端侧网络包分发
│   ├── ClientSetup.java                        # 客户端注册入口, 注册实体渲染器
│   ├── DicePlacementKey.java                   # 放置键(默认 Tab)与对放置骰子的左键拦截
│   ├── ItemTooltips.java                       # 物品提示行的集中表
│   ├── ModConfigScreen.java
│   ├── gui/
│   │   ├── AdminConfigScreen.java              # 管理员配置面板, 表格列表与 Ctrl 多选
│   │   ├── AltarScreen.java                    # 献祭界面, 容器背包与滑条与生物列表
│   │   ├── ConfirmButton.java                  # 二次确认按钮, 首次点击只改为确认文案
│   │   ├── DealerBetScreen.java                # 荷官赌注箱界面, 容器槽位
│   │   ├── DealerScreen.java                   # 荷官九宫格界面
│   │   ├── DelayedTooltip.java                 # 延迟出现的悬停提示
│   │   ├── IntSlider.java                      # 步长固定的整型滑条
│   │   ├── ItemLabels.java                     # 物品与生物的翻译名与原始注册名
│   │   ├── LuckOverlay.java                    # 左上角幸运值显示与区间配色
│   │   ├── ManualScreen.java                   # 通用书籍界面, 手册内容竖排滚动
│   │   ├── ScrollBar.java                      # 可拖动的列表滚动条
│   │   ├── TableLayout.java                    # 表格列宽算法与单元格绘制
│   │   ├── ValueTableScreen.java               # 可搜索祭品价值表, 表格与本地化名称
│   ├── manual/
│   │   ├── ManualContent.java                  # 按手册与语言读取随包 markdown 内容
│   │   ├── ManualLine.java                     # 手册的一个逻辑行与块类型
│   │   ├── ManualParser.java                   # markdown 子集解析与按页宽排版
│   │   ├── ManualRow.java                      # 排版后的一行绘制单元
│   │   ├── ManualText.java                     # 行内的普通, 粗体与内联代码片段
│   ├── model/
│   │   ├── GamblersDealerModel.java            # 荷官人形模型, 9 个长方体与原版肩髋挂点
│   └── render/
│       ├── AltarTintHandler.java               # 祭坛范围的屏幕空间后处理红色遮罩
│       ├── AltarValueLabelRenderer.java
│       ├── DiceSightLayer.java                 # 荷官透视的几何采集层, 只在实体动画已就位时记录
│       ├── DiceSightRenderer.java              # 骰子的透视亮影, 祭坛与荷官的白色半透明剪影
│       ├── GamblersDealerRenderer.java         # 荷官渲染, 使用自建模型与自绘制服贴图
│       ├── SightHalo.java
├── commands/
│   ├── CommandRunner.java                      # 奖池条目指令执行与语法校验
│   ├── ModCommands.java                        # 管理员命令入口
│   ├── NumberPlaceholders.java                 # %(n,m) 随机整数占位符
│   ├── RandomPoolSampler.java                  # 随机池令牌取样, 执行前替换为池内一个条目
│   ├── RepeatPlaceholders.java                 # --R(n) 行末重复执行
│   ├── SampleNames.java                        # 样本名称回填到结果提示
│   └── SamplePlaceholders.java
├── config/
│   ├── AccessConfig.java                       # 管理访问配置, 创建者与管理员名单
│   ├── DefaultPrizePool.java                   # 首次生成奖池配置时使用的预设
│   ├── DefaultPrizePoolData.java               # 两套预设奖池数据, 由脚本生成
│   ├── ItemValueConfig.java                    # 物品价值配置模型
│   ├── ItemValuePresets.java                   # 内置原版物品价值基准表
│   ├── NotifyConfig.java                       # 条目结果展示配置模型
│   ├── PrizeEntry.java                         # 奖池条目模型
│   ├── PrizePoolBook.java                      # 全部奖池与生效池指针
│   ├── PrizePoolConfig.java                    # 奖池配置模型与校验
│   ├── PrizeRegion.java                        # 奖池区域模型
│   ├── RandomPoolConfig.java                   # 随机池配置模型, 世界加载时由注册表重建
│   ├── SampleSpaceConfig.java
│   └── ServerConfigManager.java                # 配置加载, 写盘, 扫描兜底与乐观锁
├── data/
│   ├── DealerUsageData.java                    # 玩家骰子使用计数存档
│   └── LuckData.java                           # 幸运值与相位原点的存档数据
├── dealer/
│   ├── DealerBetBox.java                       # 单名成员的赌注箱
│   ├── DealerRewardService.java                # 骰子使用计数与生物蛋发放
│   ├── DealerWagerService.java                 # 服务端荷官赌局主流程
│   └── DealerWagerSession.java                 # 单场荷官赌局的会话状态
├── draw/
│   ├── DiceRoll.java                           # 一次掷骰, 点数与红黑颜色
│   ├── DrawEngine.java                         # 服务端抽取主流程
│   ├── DrawResult.java                         # 抽取结果
│   ├── DrawSeed.java                           # 随机种子构造
│   ├── DropCollector.java                      # 捕获作为祭品被杀生物的掉落物
│   ├── NotifySender.java                       # 结果文本发送
│   ├── Offering.java                           # 祭品条目
│   ├── OfferingBundle.java                     # 献祭包, 物品与生物与数值献祭项
│   └── RegionSelector.java                     # 由点数命中区域并计算溢出值
├── effect/
│   └── LuckChildEffect.java                    # 幸运之子状态, 期间把幸运值锁在上限
├── item/
│   ├── DevilsAltarItem.java                    # 恶魔的祭坛, 可放置也可佩戴在头部
│   ├── DevilsDiceItem.java                     # 恶魔的骰子
│   ├── DiceProvenance.java                     # 赌徒的骰子记录原玩家归属
│   ├── GamblersDiceItem.java                   # 赌徒的骰子
│   ├── LuckyAppleItem.java                     # 幸运苹果, 食用回满两条并授予幸运之子
│   ├── ManualItem.java                         # 手册类物品, 右击打开通用书籍界面
│   └── OfferingValueTableItem.java             # 祭品价值表
├── luck/
│   ├── FateModel.java                          # 隐藏命数模型, 三通道卦气按游戏内历法叠加
│   ├── LuckBehaviorEvents.java                 # 击杀与死亡等行为事件的记账
│   ├── LuckLockService.java                    # 幸运之子的锁定与恢复
│   ├── LuckService.java                        # 幸运值主流程, 每 200 刻更新并同步
│   └── SubjectiveBehaviorService.java          # 主观行为变量与每轮一次的触发机会
├── manual/
│   ├── ManualKind.java                         # 三本手册的种类, 内容路径与语言映射
│   └── ManualService.java                      # 每次进入按身份校准手册, 绑定手册不可丢弃
├── menu/
│   ├── AltarMenu.java                          # 献祭容器, 九宫格与玩家槽位与布局常量
│   └── DealerBetMenu.java                      # 赌注箱容器, 赌注格与玩家槽位与布局常量
├── mixin/
│   └── .gitkeep
├── mob/
│   ├── DealerSpawnService.java                 # 日出日落静默生成
│   ├── DealerWanderGoal.java                   # 光照优先与远离液体的寻路偏好与停留
│   └── GamblersDealer.java                     # 荷官实体, 进食与主持消耗
├── network/
│   ├── AltarStatusBroadcaster.java             # 周期向客户端广播祭坛状态
│   ├── ConfigChunks.java                       # 配置负载的分块切分
│   ├── ModNetwork.java                         # 通道注册与发送封装
│   ├── RequestGuard.java                       # 请求序号去重与频率限制
│   ├── ServerConfigPayloads.java               # 服务端配置分块的拼装与落地
│   ├── ServerRequestHandler.java               # 服务端请求处理
│   └── packet/
│       ├── AdminEditPacket.java
│       ├── AdminPanelDataPacket.java
│       ├── AdminPanelRequestPacket.java
│       ├── AltarOpenRequestPacket.java
│       ├── AltarStatusPacket.java
│       ├── BehaviorStatePacket.java            # 行为变量状态的镜像包
│       ├── ConfigPayloadPacket.java            # 配置负载的一片, 双向共用
│       ├── ConfigWriteRequestPacket.java       # 保存请求的元信息, 负载随后分片到达
│       ├── ConfigWriteResponsePacket.java
│       ├── DealerBetOpenRequestPacket.java
│       ├── DealerInteractPacket.java
│       ├── DealerPanelPacket.java
│       ├── DevilsDiceRequestPacket.java
│       ├── DiceModifierPacket.java             # 放置键状态的镜像包, 仅作兜底
│       ├── DrawResultPacket.java
│       ├── GamblersDiceRequestPacket.java
│       ├── LuckSyncPacket.java                 # 服务端推送的幸运值
│       ├── MobListPacket.java
│       ├── MobListRequestPacket.java
│       ├── OpenValueTablePacket.java
│       ├── PlaceDicePacket.java                # 放置骰子请求
│       ├── PlacedDiceBreakPacket.java          # 放置骰子的破坏请求, 携带放置键状态
│       ├── PlacedDiceUsePacket.java            # 放置骰子被当作使用时通知客户端
│       ├── PointDisplayPacket.java
│       ├── ValueTableRefreshRequestPacket.java
│       ├── ValueTableSyncPacket.java
├── registry/
│   ├── ModBlockEntities.java                   # 方块实体注册
│   ├── ModBlocks.java                          # 方块注册
│   ├── ModCreativeTabs.java                    # 模组专属物品页
│   ├── ModEffects.java                         # 状态效果注册
│   ├── ModEntities.java                        # 实体类型注册
│   ├── ModEntityAttributes.java                # 实体属性注册
│   ├── ModItems.java                           # 物品注册
│   ├── ModMenus.java                           # 容器菜单类型注册
│   └── ModModelLayers.java                     # 客户端模型层键
├── util/
│   ├── AuthorInfo.java                         # 作者信息常量
│   ├── CommandPlaceholders.java                # 条目指令占位符解析与范围随机坐标
│   ├── CommandSequence.java                    # 全服指令执行计数, 每行据此派生独立种子
│   ├── InventoryHandover.java
│   ├── ItemValuation.java                      # 物品实例估值, 含附魔加成与耐久折算
│   ├── ModConstants.java                       # 设计细节常量集中定义
│   ├── OfferEligibility.java
│   ├── Placeholders.java                       # 指令与文本占位符展开
│   ├── SampleContext.java
│   ├── ServerScheduler.java                    # 服务端延迟任务调度
│   ├── ValueTableBook.java                     # 价值表快照读写
│   └── ValueTableSnapshot.java                 # 客户端侧价值表只读视图
└── wager/
    ├── WagerArea.java                          # 赌区球体定义与范围判定
    ├── WagerService.java                       # 服务端赌局主流程
    └── WagerSession.java                       # 单场赌局的会话状态
```

## 资源

```
src/main/resources/
├── META-INF/
│   └── mods.toml                                  # 模组元数据, 版本号由构建占位符注入
├── assets/
│   └── devil_bone_gambler/
│       ├── blockstates/
│       │   ├── devils_altar.json                  # 祭坛方块状态
│       │   ├── placed_devils_dice.json
│       │   ├── placed_gamblers_dice.json
│       ├── lang/
│       │   ├── en_us.json                         # 英文文本
│       │   ├── zh_cn.json                         # 简体中文文本
│       ├── prize_pools/
│       │   ├── presets.json                       # 预设奖池, 由 scripts/build_preset_pool.py 生成
│       ├── manuals/
│       │   ├── admin_manual_en_us.md              # 管理员手册英文内容
│       │   ├── admin_manual_zh_cn.md              # 管理员手册简体中文内容
│       │   ├── creator_manual_en_us.md            # 创建者手册英文内容, 后半部分复用管理员手册
│       │   ├── creator_manual_zh_cn.md            # 创建者手册简体中文内容, 后半部分复用管理员手册
│       │   ├── gameplay_manual_en_us.md           # 玩法手册英文内容
│       │   ├── gameplay_manual_zh_cn.md           # 玩法手册简体中文内容
│       ├── models/
│       │   ├── block/
│       │   │   ├── devils_altar.json              # 祭坛方块模型, 51 个长方体与 128x256 图集
│       │   │   ├── placed_devils_dice.json
│       │   │   ├── placed_gamblers_dice.json
│       │   ├── item/
│       │   │   ├── admin_manual.json
│       │   │   ├── creator_manual.json
│       │   │   ├── devils_altar.json              # 祭坛物品模型, 复用方块模型
│       │   │   ├── devils_dice.json
│       │   │   ├── gamblers_dealer_spawn_egg.json # 荷官生物蛋, 沿用原版生成式生物蛋模型
│       │   │   ├── gamblers_dice.json
│       │   │   ├── gameplay_manual.json
│       │   │   ├── lucky_apple.json
│       │   │   ├── offering_value_table.json
│       ├── shaders/
│       │   ├── post/
│       │   │   ├── altar_tint.json                # 祭坛遮罩后处理链
│       │   ├── program/
│       │   │   ├── altar_tint.fsh                 # 遮罩片元着色器
│       │   │   ├── altar_tint.json                # 遮罩着色器程序声明
│       │   │   ├── altar_tint.vsh                 # 遮罩顶点着色器
│       ├── textures/
│       │   ├── block/
│       │   │   ├── devils_altar.png               # 祭坛方块贴图, 128x256 图集
│       │   │   ├── devils_altar_particle.png      # 祭坛破坏粒子贴图, 16x16
│       │   ├── entity/
│       │   │   ├── gamblers_dealer.png            # 荷官赌场制服贴图, 64x64 自绘 UV
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
│   │   │   │   ├── devils_altar.json              # 祭坛掉落表
│   │   ├── recipes/
│   │   │   ├── devils_altar.json                  # 恶魔的祭坛
│   │   │   ├── devils_dice.json                   # 恶魔的骰子
│   │   │   ├── gamblers_dice.json                 # 赌徒的骰子
│   │   │   ├── lucky_apple.json
│   │   │   ├── offering_value_table.json          # 祭品价值表
│   └── minecraft/
│       ├── tags/
│       │   ├── blocks/
│       │   │   ├── dragon_immune.json             # 末影龙无法破坏的方块标签
│       │   │   ├── mineable/
│       │   │   │   ├── pickaxe.json               # 可被镐挖掘的方块标签
│       │   │   ├── needs_iron_tool.json           # 需要铁质工具的方块标签
│       │   │   ├── wither_immune.json             # 凋灵无法破坏的方块标签
├── mixins.devil_bone_gambler.json                 # 混入配置, 声明混入包与混入类清单
└── pack.mcmeta                                    # 资源包元数据
```

## 约定

- 空目录以 `.gitkeep` 占位, 便于 git 追踪目录结构; 目录内新增文件后即可删除对应的 `.gitkeep`
- Java 包根为 `com.devilbonegambler`, 与 modId `devil_bone_gambler` 对应; 混入类所在包为 `com.devilbonegambler.mixin`, 必须与混入配置中的 `package` 字段一致
- 客户端代码集中在 `client/` 下, 公共代码只能经 `DistExecutor` 触达, 服务端代码不得直接引用客户端类
- 设计细节常量集中在 `util/ModConstants.java`, 作者信息集中在 `util/AuthorInfo.java`
- 语言文件需要同时维护简体中文与英文两份, 两者键名与占位符必须完全一致, 由 scripts/check_lang_keys.py 守卫
- 容器菜单集中在 menu/ 与 registry/ModMenus.java, 界面的槽位坐标一律取自菜单类的布局常量, 因此绘制的标签与真实槽位不会脱节
- 贴图规格为 16 的整数倍像素, 32 位带透明通道, 不使用抗锯齿与渐变; 物品与单面贴图使用 16x16, 多部件方块模型使用 16 的整数倍图集
- 自定义方块模型需要在 `textures` 中声明 `particle` 贴图, 否则破坏粒子会回退到原版缺失贴图
- 服务端配置的加载, 校验, 扫描兜底与同步集中在 `config/ServerConfigManager.java`; 抽取流程集中在 `draw/DrawEngine.java` 与 `draw/RegionSelector.java`
- 赌局机制集中在 `wager/`; 祭坛的方块, 方块实体与范围服务集中在 `block/` 与 `altar/`
- 荷官的实体, 寻路与静默生成集中在 `mob/`, 荷官赌局与奖励集中在 `dealer/`; 荷官渲染使用 client/model/ 下自建模型并把四肢挂点保持与原版人形一致, 静默生成规则只在服务端执行且不下发到客户端
- 随机池的样本空间在每次世界加载成功时由注册表重建并覆盖落盘, 取样集中在 `commands/RandomPoolSampler.java`, 配置模型在 `config/RandomPoolConfig.java`
- 祭坛遮罩的着色器程序与后处理链位于 `assets/devil_bone_gambler/shaders/`
- 方块的挖掘工具要求同时由 `data/minecraft/tags/blocks/` 下的标签与代码中的破坏进度判定承担, 两者缺一不可
- 配置项清单与运行期文件位置见 [开发者文档](../docs/developer/README.md)
- 幸运值, 隐藏命数模型与主观行为变量集中在 `luck/`, 幸运之子的状态实现放在 `effect/`
- 放置骰子的放置与破坏判定集中在 `block/PlacedDiceService.java`, 破坏请求由客户端连同放置键状态一起发出
- 本目录树由 `scripts/check_doc_structure.py` 与源码比对, 新增类或资源后必须同步这里与英文版
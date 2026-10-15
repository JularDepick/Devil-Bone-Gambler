# 源码目录结构

[简体中文]
| [[English]](./README_en-US.md)

本目录是模组的功能性源码目录, 根目录的其余内容为辅助性与说明性内容

## Java 源码

```
src/main/java/com/devilbonegambler/
├── DevilBoneGambler.java        # 模组入口, 注册延迟注册器与网络通道
├── client/                      # 客户端专属代码, 仅经 DistExecutor 触达
│   ├── ClientActions.java       # 客户端动作入口, 发包与打开界面
│   ├── ClientPacketHandler.java # 客户端侧网络包分发
│   └── gui/                     # 客户端界面
│       ├── AdminConfigScreen.java # 管理员配置面板
│       └── AltarScreen.java       # 献祭界面
├── commands/                    # 命令注册
│   └── ModCommands.java         # 管理员命令入口
├── config/                      # 服务端配置模型与读写
│   ├── DefaultPrizePool.java    # 首次生成奖池配置时使用的预设
│   ├── ItemValueConfig.java     # 物品价值配置模型
│   ├── ItemValuePresets.java    # 内置原版物品价值基准表
│   ├── NotifyConfig.java        # 条目结果展示配置模型
│   ├── PrizeEntry.java          # 奖池条目模型
│   ├── PrizePoolConfig.java     # 奖池配置模型与校验
│   ├── PrizeRegion.java         # 奖池区域模型
│   └── ServerConfigManager.java # 配置加载, 写盘, 扫描兜底与乐观锁
├── data/                        # 存档持久化
│   └── LuckData.java            # 玩家运气值存档数据
├── draw/                        # 抽取机制
│   ├── DrawEngine.java          # 服务端抽取主流程
│   ├── DrawResult.java          # 抽取结果
│   ├── DrawSeed.java            # 随机种子构造
│   ├── NotifySender.java        # 结果文本发送
│   └── Offering.java            # 祭品条目
├── item/                        # 物品
│   ├── DevilsDiceItem.java      # 恶魔的骰子
│   ├── GamblersDiceItem.java    # 赌徒的骰子
│   └── OfferingValueTableItem.java # 祭品价值表
├── mixin/                       # 混入类目录, 当前为空
├── network/                     # 网络通道, 协议与消息包
│   ├── ModNetwork.java          # 通道注册与发送封装
│   ├── RequestGuard.java        # 请求序号去重与频率限制
│   ├── ServerRequestHandler.java # 服务端请求处理
│   └── packet/                  # 消息包
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
├── registry/                    # 物品与物品页注册
│   ├── ModCreativeTabs.java     # 模组专属物品页
│   └── ModItems.java            # 物品注册
└── util/                        # 通用工具
    ├── AuthorInfo.java          # 作者信息常量
    ├── ModConstants.java        # 设计细节常量集中定义
    ├── Placeholders.java        # 指令与文本占位符展开
    ├── ValueTableBook.java      # 价值表快照读写
    └── ValueTableSnapshot.java  # 客户端侧价值表只读视图
```

## 资源

```
src/main/resources/
├── META-INF/
│   └── mods.toml                      # 模组元数据, 版本号由构建占位符注入
├── assets/devil_bone_gambler/
│   ├── blockstates/                   # 方块状态, 当前为空
│   ├── lang/
│   │   ├── en_us.json                 # 英文文本
│   │   └── zh_cn.json                 # 简体中文文本
│   ├── models/
│   │   ├── block/                     # 方块模型, 当前为空
│   │   └── item/
│   │       ├── devils_dice.json
│   │       ├── gamblers_dice.json
│   │       └── offering_value_table.json
│   └── textures/
│       ├── block/                     # 方块贴图, 当前为空
│       └── item/
│           ├── devils_dice.png
│           ├── gamblers_dice.png
│           └── offering_value_table.png
├── data/devil_bone_gambler/
│   └── recipes/                       # 合成配方, 当前为空
├── mixins.devil_bone_gambler.json     # 混入配置, 声明混入包与混入类清单
└── pack.mcmeta                        # 资源包元数据
```

## 约定

- 空目录以 `.gitkeep` 占位, 便于 git 追踪目录结构; 目录内新增文件后即可删除对应的 `.gitkeep`
- Java 包根为 `com.devilbonegambler`, 与 modId `devil_bone_gambler` 对应; 混入类所在包为 `com.devilbonegambler.mixin`, 必须与混入配置中的 `package` 字段一致
- 客户端代码集中在 `client/` 下, 公共代码只能经 `DistExecutor` 触达, 服务端代码不得直接引用客户端类
- 设计细节常量集中在 `util/ModConstants.java`, 作者信息集中在 `util/AuthorInfo.java`
- 语言文件需要同时维护简体中文与英文两份, 两者键名必须完全一致
- 贴图规格为 16x16 像素, 32 位带透明通道, 不使用抗锯齿与渐变
- 服务端配置的加载, 校验, 扫描兜底与同步集中在 `config/ServerConfigManager.java`;抽取流程集中在 `draw/DrawEngine.java` 与 `draw/RegionSelector.java`
- 配置项清单与运行期文件位置见 [开发者文档](../docs/developer/README.md)

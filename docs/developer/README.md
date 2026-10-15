# 开发者文档

[简体中文]
| [[English]](./README_en-US.md)

## 运行与构建

| 命令 | 说明 |
|:---:|:---:|
| `build.bat` | 调用 PATH 上的 gradle 构建并列出产物 |
| `gradlew build` | 构建模组产物, 产物输出到 `build/libs/` |
| `gradlew runClient` | 启动 Minecraft 客户端开发环境 |
| `gradlew runServer` | 启动 Minecraft 服务端开发环境 |
| `gradlew clean` | 清理构建产物 |
| `gradlew --stop` | 停止常驻的 Gradle 守护进程 |

产物文件名为 `DevilBoneGambler-v<版本号>-mc-1.20.1-forge.jar`

首次构建需要联网下载 Forge 与 Minecraft 依赖, 依赖缓存位于 Gradle 用户目录

## 工程结构约定

| 位置 | 约定 |
|:---:|:---:|
| `src/main/java/com/devilbonegambler/` | 源码目录, Java 包根, 与 modId `devil_bone_gambler` 对应 |
| `src/main/java/com/devilbonegambler/client/` | 仅客户端代码, 界面放在其 `gui/` 子包, 服务端代码不得引用 |
| `src/main/java/com/devilbonegambler/mixin/` | 混入类目录, 包名须与混入配置中的 `package` 字段一致 |
| `src/main/java/com/devilbonegambler/util/ModConstants.java` | 设计细节常量集中定义, 调整数值只改这里 |
| `src/main/java/com/devilbonegambler/util/AuthorInfo.java` | 作者信息常量, 界面与注释头只引用常量 |
| `src/main/resources/` | 资源目录, 资源命名空间为 `devil_bone_gambler` |
| `src/main/resources/assets/devil_bone_gambler/` | 模型, 贴图, 方块状态与语言文件 |
| `src/main/resources/data/devil_bone_gambler/` | 数据包内容, 含祭坛掉落表与四种物品的合成配方 |
| `src/main/resources/mixins.devil_bone_gambler.json` | 混入配置, 声明混入包与混入类清单 |
| `src/main/resources/META-INF/mods.toml` | 模组元数据, 版本号由构建占位符注入 |

空目录以 `.gitkeep` 占位, 便于 git 追踪目录结构

## 服务端运行期文件

| 文件 | 位置 | 说明 |
|:---:|:---:|:---:|
| 奖池配置 | `<世界存档>/serverconfig/devil_bone_gambler/prize_pool.json` | 分层奖池, 首次启动按预设生成;条目的指令文本支持多行逐行执行, 每行斜杠可选, 并支持玩家名称, 玩家坐标与祭坛位置占位符与范围随机坐标 |
| 物品价值配置 | `<世界存档>/serverconfig/devil_bone_gambler/item_values.json` | 物品价值表, 首次启动按内置预设与扫描结果生成 |
| 玩家运气数据 | `<世界存档>/data/devil_bone_gambler_luck.dat` | 按玩家标识索引的运气值 |

## 管理员入口

| 命令 | 权限等级 | 说明 |
|:---:|:---:|:---:|
| `/devilbonegambler config` | 2 | 向执行者下发两份配置并打开配置面板 |
| `/devilbonegambler reload` | 2 | 从磁盘重新加载两份配置 |

## 版本号与占位符

版本号唯一来源为 `build.gradle`, 构建期由 `processResources` 注入到 `mods.toml` 的 `version` 字段; 版本号出现位置见 [版本号索引](../../version.index.md)

## 文档清单

| 文档 | 用途 |
|:---:|:---:|
| [README.md](../../README.md) | 项目说明 |
| [源码目录结构](../../src/README.md) | 源码与资源的目录结构及各类职责 |
| [贡献指南](../../CONTRIBUTING.md) | 贡献规范与贡献方式 |
| [版本号索引](../../version.index.md) | 版本号出现位置与行数 |

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
| `src/main/java/com/devilbonegambler/menu/` | 容器菜单, 界面的槽位坐标取自菜单类的布局常量; 菜单类型注册在 `registry/ModMenus.java` |
| `src/main/java/com/devilbonegambler/luck/` | 幸运值主流程, 隐藏命数模型, 主观行为变量与幸运之子锁定 |
| `src/main/java/com/devilbonegambler/mixin/` | 混入类目录, 包名须与混入配置中的 `package` 字段一致 |
| `src/main/java/com/devilbonegambler/util/ModConstants.java` | 设计细节常量集中定义, 调整数值只改这里 |
| `src/main/java/com/devilbonegambler/util/AuthorInfo.java` | 作者信息常量, 界面与注释头只引用常量 |
| `src/main/resources/` | 资源目录, 资源命名空间为 `devil_bone_gambler` |
| `src/main/resources/assets/devil_bone_gambler/` | 模型, 贴图, 方块状态, 语言文件与状态图标 |
| `src/main/resources/assets/devil_bone_gambler/manuals/` | 三本手册的 markdown 内容, 每本每种语言一份, 命名形如 `<物品注册名>_<语言码>.md` |
| `src/main/resources/data/devil_bone_gambler/` | 数据包内容, 含方块掉落表与五种物品的合成配方 |
| `src/main/resources/mixins.devil_bone_gambler.json` | 混入配置, 声明混入包与混入类清单 |
| `src/main/resources/META-INF/mods.toml` | 模组元数据, 版本号由构建占位符注入 |

空目录以 `.gitkeep` 占位, 便于 git 追踪目录结构

## 服务端运行期文件

| 文件 | 位置 | 说明 |
|:---:|:---:|:---:|
| 奖池配置 | `<世界存档>/serverconfig/devil_bone_gambler/prize_pool.json` | 奖池数组, 每套有 id, 名称与描述; 首次启动按两套内置预设生成; 条目的指令内容支持多行逐行执行, 每行斜杠可选, 并支持玩家名称, 玩家坐标与祭坛位置占位符, 范围随机坐标, `%(n,m)` 随机整数, `--R(n)` 逐行重复与样本空间占位符 |
| 奖池状态 | `<世界存档>/serverconfig/devil_bone_gambler/prize_pool_state.json` | 记录生效奖池的 id 与修订号, 抽取只读生效的那一套 |
| 祭品价值配置 | `<世界存档>/serverconfig/devil_bone_gambler/item_values.json` | 祭品价值表, 首次启动按内置预设与扫描结果生成 |
| 玩家幸运数据 | `<世界存档>/data/devil_bone_gambler_luck.dat` | 按玩家标识索引的幸运值, 相位原点(首次进入的游戏刻)与存档创建时刻的现实毫秒 |
| 骰子使用计数数据 | `<世界存档>/data/devil_bone_gambler_dealer_usage.dat` | 按玩家标识索引的赌徒的骰子使用计数, 用于发放赌徒的荷官生物蛋 |
| 随机池 | `<世界存档>/serverconfig/devil_bone_gambler/random_pools.json` | 随机条目令牌的样本空间, 每次世界加载成功时由注册表扫描重建并覆盖写盘, 含物品, 效果, 实体, 附魔与药水五个池 |
| 样本空间 | `<世界存档>/serverconfig/devil_bone_gambler/sample_spaces.json` | 样本空间占位符的候选集合, 同样在每次世界加载成功时由注册表扫描重建并覆盖写盘, 含 15 个通用类型与 6 个细分类型 |
| 管理访问 | `<世界存档>/serverconfig/devil_bone_gambler/access.json` | 创建者与管理员名单, 只存玩家名字; 文件为空时首位进入服务器的玩家被记为创建者, 创建者与管理员可打开管理面板, 只有创建者能增减管理员 |

## 管理员入口

| 命令 | 权限 | 说明 |
|:---:|:---:|:---:|
| `/devilbonegambler config` 或 `/dbgr config` | 创建者与管理员 | 向执行者下发配置并打开配置面板 |
| `/devilbonegambler reload` 或 `/dbgr reload` | 创建者与管理员 | 从磁盘重新加载配置 |
| `/devilbonegambler run <指令>` 或 `/dbgr run` | 创建者与管理员 | 在聊天框试跑带占位符的指令, 不回填样本名称 |
| `/devilbonegambler luck <玩家> <数值>` 或 `/dbgr luck` | 创建者与管理员 | 把指定玩家的幸运值设为 0 到 100 内的值, 在线玩家立即同步, 离线玩家下次进入生效 |

游戏内的管理入口还有 Shift 加 D 键位, 与命令共用同一判定, 键位显示名为中性文案

## 校验脚本

`scripts/` 下的 Python 脚本是静态守卫, 只读源码与资源并断言约定, 退出码 0 表示通过; 提交前建议全部跑一遍

| 范围 | 脚本示例 |
|:---:|:---:|
| 模型与贴图 | `check_dice_model.py`, `check_altar_model.py`, `check_dealer_model.py`, `check_manual_policy.py` |
| 语言与翻译边界 | `check_lang_keys.py`, `check_lang_coverage.py`, `check_translation_boundary.py`, `check_item_tooltips.py` |
| 占位符与指令 | `check_number_placeholders.py`, `check_repeat_placeholders.py`, `check_sample_placeholders.py`, `check_prompt_channel.py` |
| 玩法机制 | `check_luck_system.py`, `check_lucky_apple.py`, `check_placed_dice.py`, `check_altar_protection.py`, `check_recipes.py` |
| 界面与文档 | `check_admin_layout.py`, `check_tooltip_delay.py`, `check_manuals.py`, `check_doc_structure.py` |

## 版本号与占位符

版本号唯一来源为 `build.gradle`, 构建期由 `processResources` 注入到 `mods.toml` 的 `version` 字段; 版本号出现位置见 [版本号索引](../../version.index.md)

## 文档清单

| 文档 | 用途 |
|:---:|:---:|
| [README.md](../../README.md) | 项目说明与玩法概览 |
| [源码目录结构](../../src/README.md) | 源码与资源的目录结构及各类职责 |
| [贡献指南](../../CONTRIBUTING.md) | 贡献规范与贡献方式 |
| [版本号索引](../../version.index.md) | 版本号出现位置与行数 |

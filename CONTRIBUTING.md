<div align="center">

# 贡献指南

[简体中文]
| [[English]](./CONTRIBUTING_en-US.md)

</div>

欢迎为 Devil Bone Gambler 贡献代码修正, 文档改进与功能建议

## 贡献规范

### 代码规范

- 保持既有目录结构, 不新增未经讨论的顶层目录
- Java 代码使用 Allman 大括号风格, 缩进 4 空格, 注释使用跨行注释
- 只使用 Forge 1.20.1 官方 mappings 提供的 API, 不引入外部依赖
- 新增或修改可见文本时, 同步维护 `src/main/resources/assets/devil_bone_gambler/lang/` 下的简体中文与英文语言文件, 并保持两者键名一致
- 客户端专属代码放在 `src/main/java/com/devilbonegambler/client/` 下, 不在服务端代码中引用客户端类
- 可调的设计细节常量集中写在 `util/ModConstants.java`, 不在业务代码里散落魔法数字
- 新增物品, 方块或状态时要同时准备贴图, 模型, 语言键与物品提示, 并保证状态能被样本空间扫描到

### 文档规范

- 三份工程文档必须与代码同步: 根目录 `README.md` 介绍玩法与指令, [源码目录结构](./src/README.md) 逐文件列出职责, [开发者文档](./docs/developer/README.md) 记录构建与运行期文件
- 中文与英文版本同时维护, 结构与行数保持一致
- 新增类或资源时更新 `src/README.md` 与 `src/README_en-US.md` 的目录树, 由 `scripts/check_doc_structure.py` 守卫
- 版本号变化时更新 [版本号索引](./version.index.md) 中记录的文件与行号

### 校验规范

- 提交前先执行 `gradlew build` 确认构建通过
- 再执行 `scripts/` 下的静态守卫脚本, 全部退出码为 0 才算通过; 守卫清单见 [开发者文档](./docs/developer/README.md) 的校验脚本一节
- 触及玩法机制的改动需要在游戏内按实测清单验证, 静态检查不能替代这一步

### 提交规范

- 一个 PR 只处理一类改动, 便于审阅
- 提交前先执行 `gradlew build` 确认构建通过
- PR 描述中说明改动的范围与影响

## 贡献方式

### 1. 提交 Issue
- [新建 Issue](https://github.com/JularDepick/Devil-Bone-Gambler/issues/new)
- 描述你发现的漏洞或发布你的预期
- 提交 Issue

### 2. 贡献代码
- [Fork 本项目](https://github.com/JularDepick/Devil-Bone-Gambler/fork)
- 在你的 fork 分支完成代码修改
- [在本项目创建 PR](https://github.com/JularDepick/Devil-Bone-Gambler/compare)
- 提交 PR 并等待审核

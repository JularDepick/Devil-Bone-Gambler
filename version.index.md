# 版本号索引

当前版本: `v0.8.0`

具体版本号出现的位置:

| 文件路径 | 行数 | 出现形式 | 说明 |
|:---:|:---:|:---:|:---:|
| `build.gradle` | 7 | `0.8.0` | 版本号唯一来源,产物文件名与 mods.toml 的版本号由此注入 |
| `README.md` | 5 | `0.8.0` 与 `v0.8.0` | 中文 README 版本徽章的文字与链接标签 |
| `README_en-US.md` | 5 | `0.8.0` 与 `v0.8.0` | 英文 README 版本徽章的文字与链接标签 |

由占位符承接版本号的位置(文件内不含具体版本号,版本号迭代时无需修改):

| 文件路径 | 行数 | 出现形式 | 说明 |
|:---:|:---:|:---:|:---:|
| `build.gradle` | 103 | `${project.version}` | 参与产物文件名拼接 |
| `src/main/resources/META-INF/mods.toml` | 8 | `version="${version}"` | 模组元数据版本,构建期注入 |
| `docs/developer/README.md` | 17 | `DevilBoneGambler-v<版本号>-mc-1.20.1-forge.jar` | 文档中的产物名示例 |
| `docs/developer/README_en-US.md` | 17 | `DevilBoneGambler-v<version>-mc-1.20.1-forge.jar` | 文档中的产物名示例 |

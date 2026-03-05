# SPW 歌词匹配插件 

为 [Salt Player Workshop](https://github.com/Moriafly/spw-workshop-api) 开发的歌词插件。
能够匹配网易云词库，支持通过 SPW 设置面板控制**中文翻译**与**罗马音**的显示。

##  核心特性

* **匹配**：清洗歌名中的杂质大幅提升匹配成功率。
* **罗马音支持**：针对日文歌曲自动拉取并对齐罗马音时间轴。
* **UI控制**：接入 SPW 模组管理界面。
* **逐字歌词**：将YRC转换成SPL标准，以实现逐字歌词效果。

##  如何安装

1. 前往本项目的 [Releases 页面](https://github.com/Casper-003/SPW-Lyric-Plugin/releases) 下载最新版本的 `.zip` 压缩包（**请勿解压**）。
2. 打开 SPW 软件，进入左侧菜单的 **设置** -> **创意工坊** -> **模组管理**。
3. 单击右侧的**导入模组**按钮，选择下载好的`.zip` 压缩包。
4. 在模组列表中找到 `SLyric`，点击该项目弹出菜单中的**启用**。
5. 点击弹出菜单的**配置** 按钮进入自定义设置。

##  构建指南

如果你希望自己编译此插件：
1. 克隆本项目代码。
2. 使用 IntelliJ IDEA 打开项目。
3. 在右侧 Gradle 面板中，双击运行 `Tasks -> other -> plugin`。
4. 在 `%APPDATA%\SaltPlayer\plugins` (或相应 SPW 数据目录) 中获取编译好的 `.zip` 文件。

##  问题反馈

如果遇到无法匹配的歌曲，欢迎提交 [Issue](https://github.com/Casper-003/SPW-Lyric-Plugin/issues)，并附上歌曲名称与艺术家。

---
*Developed with ❤️ by Casper-003*

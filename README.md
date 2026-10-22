# SPW 歌词匹配插件

为 [Salt Player for Windows](https://github.com/Moriafly/SPW) 开发的歌词插件，提供网易云歌词搜索、中文翻译、罗马音和 YRC 到 SPL 的逐字歌词转换。插件名称为 `SLyric`。

当前使用 `onAfterLoadLyrics` 扩展点，在播放器默认逻辑无法加载歌词时补充歌词。配置界面原有的“优先使用”说明尚未随行为修正，详见[已知问题](docs/known-issues.md)。

## 安装

1. 从 [Releases](https://github.com/Casper-003/SPW-Lyric-Plugin/releases) 下载插件 ZIP，或按下文自行构建。不要解压。
2. 在 SPW 的 **设置 → 创意工坊 → 模组管理** 中导入 ZIP。
3. 找到 `SLyric`，选择启用，并通过配置面板设置翻译、罗马音、逐字歌词和提示。

## 开发与构建

需要完整的 **JDK 21**。Gradle Wrapper 固定为 **8.11.1**，下载时验证 SHA-256，无需另装 Gradle。IntelliJ IDEA 可直接打开本仓库，并将 Gradle JVM 设置为 JDK 21。

macOS / Linux：

```sh
./gradlew test plugin
```

Windows PowerShell：

```powershell
.\gradlew.bat test plugin
```

产物位于 `build/distributions/SLyric-1.2.3.zip`。构建不会自动修改播放器目录。

可选安装命令：

```powershell
.\gradlew.bat installPlugin
# 或指定目标目录，适用于所有操作系统：
.\gradlew.bat installPlugin '-PpluginInstallDir=D:/SPW/plugins'
```

未指定 `pluginInstallDir` 时，安装任务使用 `%APPDATA%/Salt Player for Windows/workshop/plugins`。播放器加载和歌词显示需要在 Windows SPW 中验证。

### SPW API 依赖

API 固定为 `0.1.0-dev20`，来自官方 [spw-workshop-api](https://github.com/Moriafly/spw-workshop-api)。正常构建从 JitPack 解析 API，其间接依赖的 AndroidX 库从 Google Maven 解析，其余依赖从 Maven Central 解析。API 和 PF4J 由播放器提供，不打入插件运行库。

若网络无法访问 JitPack，可使用从同版本官方源码构建的 API JAR：

```sh
./gradlew -PspwApiJar=/absolute/path/api-0.1.0-dev20.jar test plugin
```

不要使用自制 API 桩代替官方 API。云环境中已经构建的官方 JAR 位于 `/workspace/.spw-env/build/api/build/libs/api-0.1.0-dev20.jar`；这是当前环境的本地路径，不是仓库的一部分。

### 版本与目录

- `gradle.properties`：插件版本的唯一来源，同时决定 JAR 元数据和 ZIP 文件名；发版只需修改这里的 `version`。
- `gradle/libs.versions.toml`：Kotlin、SPW API、网络库和测试依赖的版本。
- `NeteaseLyricPlugin.kt`：SPW 接入、配置读取与用户提示，保留原插件类名及 ID。
- `network/`：网易云请求与搜索关键词清洗。
- `lyrics/`：歌词数据模型、LRC/YRC 解析、时间轴匹配、属性行识别、SPL 输出与合并。
- `src/test/`：离线单元测试、HTTP 集成测试及旧版输出样例。

## 测试与已知限制

```sh
./gradlew test
```

报告位于 `build/reports/tests/test/index.html`。测试使用本地 MockWebServer，不请求网易云，也不需要启动播放器。

最初的规范化保留既有歌词算法。`LyricCompatibilityTest` 锁定六组旧版异常输出；它们不是正确性证明。`KnownLyricIssuesTest` 中四个目标测试仍被显式禁用，等待后续歌词修复与本地播放器验证。

`1.2.3` 修复了逐字歌词的制作信息行误占原文/翻译槽位的问题。若前两句带翻译或罗马音的原文能与逐字歌词唯一匹配，首句时间相差超过 1 秒、第二句相差不超过 1 秒，则整首自动使用普通歌词。此保护独立于翻译/罗马音的显示开关，不生成猜测的逐字时间；重复文本或证据不足时不会触发。该策略来自歌曲 `2080448369` 的数据分析，更新后的播放器显示仍需实测。

已知时间轴问题、测试含义和后续验证方式见 [docs/known-issues.md](docs/known-issues.md)。真实网易云访问和 Windows SPW 插件加载不在离线测试范围内。

GitHub Actions 会运行测试和打包，并保存 ZIP 与测试报告；首次远程执行结果需在仓库 Actions 页面确认。

## 问题反馈

请通过 [Issues](https://github.com/Casper-003/SPW-Lyric-Plugin/issues) 提交 SPW 版本、歌曲名称与歌手、准确歌曲版本或网易云 ID、相关配置和异常时间点。

---

Developed by Casper-003 & Google Gemini.

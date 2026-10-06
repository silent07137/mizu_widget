# Mizu Widget

轻量 Android 图片与 GIF 桌面组件。选择图片，调整显示效果，再添加到桌面。

**Android 9+ · 离线运行 · 无广告 · GPL-2.0-only**

## 项目进度

**0.2.0 已加入 GIF 循环播放，尚未发布正式安装包。** 源码目前位于 [开发分支](https://github.com/silent07137/mizu_widget/tree/codex/mizu-initial-development)，正在通过 [PR #1](https://github.com/silent07137/mizu_widget/pull/1) 合入主分支。

测试构建可从 [GitHub Actions](https://github.com/silent07137/mizu_widget/actions/workflows/ci.yml) 中选择开发分支最新成功运行，下载 `mizu-widget-debug` artifact 中的 `app-debug.apk`。测试包与正式包使用不同包名。

## 功能

| 功能 | 支持情况 |
| --- | --- |
| 图片格式 | JPG / JPEG、透明 PNG、WebP、GIF |
| GIF 播放 | 默认 2 FPS，可选 1 / 2 / 5 FPS；15 / 30 FPS 高帧率需手动选择；也可关闭播放 |
| 多个组件 | 各自保存图片和设置，互不影响 |
| 缩放 | 完整显示、居中裁剪、拉伸铺满 |
| 圆角 | 0–64 dp |
| 图片不透明度 | 0–100%，独立于背景 |
| 背景 | 透明、白色、深色、浅水绿 |
| 点击操作 | 打开设置、查看图片、无操作 |
| 组件管理 | 桌面调整尺寸、重新编辑、手动刷新、删除清理 |

GIF 按时间采样后交给桌面的系统 ViewFlipper 循环播放，不需要应用常驻后台。帧数和总位图内存均有限制，高帧率会降低细节，长动画可能降帧，超限文件回退静态帧。扩展名或 MIME 类型写错的 GIF 也按文件内容识别。详见 [GIF 播放说明](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/GIF.md)。深色主题和国际化尚未实现。

## 使用

1. 打开应用，点击 **添加组件**。
2. 点击 **选择图片**，从系统文件选择器中选取图片。
3. 调整缩放、圆角、不透明度、背景与点击操作。
4. 点击 **添加到桌面**，在桌面的确认窗口中确认。

也可以长按桌面 → 小组件 → Mizu Widget，选择图片后保存。若桌面不支持快捷添加，请使用这个入口。

已添加的组件可在应用内点击 **编辑**。透明区域的棋盘格仅用于设置页预览，不会显示在桌面上。

新选择 GIF 时默认开启 2 FPS 播放。旧版本已有 GIF 保持静态，可在设置中开启“GIF 播放”。高帧率默认关闭，可在“帧率上限”中选择 15 或 30 FPS。

原图需要持续可读。删除原图、撤销授权、卸载 SD 卡或云文件离线不可用时，可点击组件重新选择图片。

## 隐私

图片仅在本机读取和处理，不上传。应用没有声明网络或广泛存储权限，无账号、广告、分析或常驻后台服务。图片通过系统文件选择器的持久 URI 授权读取；渲染缓存保存在应用私有目录，删除组件时清理不再使用的配置、缓存和授权。

系统备份和设备迁移均排除应用数据。系统文件选择器可能展示其他应用提供的云文件，其可用性由文件提供方决定。

## 构建与测试

请先切换到上述开发分支。需要 **JDK 17+、Android SDK 37.0、Build Tools 36.0.0**；项目固定使用 Gradle 9.3.1 和 AGP 9.1.1。

在本机 `local.properties` 中配置 `sdk.dir`，或设置 `ANDROID_HOME`。不要提交本机配置或签名材料。

Windows：

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest lint
python tools/device_tests.py --adb "你的 Android SDK/platform-tools/adb.exe"
```

Linux / macOS：

```sh
chmod +x gradlew
./gradlew assembleDebug assembleDebugAndroidTest lint
python3 tools/device_tests.py
```

Debug 包名为 `io.github.silent07137.mizuwidget.debug`。正式签名构建由 GitHub Actions 使用 Secrets 完成，仓库不保存 keystore、密码或安装包。

采用 Java 17、Android 原生 Views、AppWidgetProvider、RemoteViews、ImageDecoder 和平台 Movie，无第三方运行库。图片解码与桌面位图均限制尺寸，避免直接传递原始大图。

已验证：构建与 lint 通过；Android 16 真机 16 项测试通过，覆盖透明通道、缩放、内存上限、独立配置、GIF 换帧与实际组件宿主。API 28 / 36 云端兼容性结果见 PR 检查与测试记录。

## 项目文档

- [开发说明](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/DEVELOPMENT.md)
- [测试记录](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/TESTING.md)
- [GIF 播放说明](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/GIF.md)
- [发布与签名](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/RELEASE.md)
- [更新记录](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/CHANGELOG.md)
- [构建工具许可](https://github.com/silent07137/mizu_widget/blob/codex/mizu-initial-development/docs/BUILD-TOOLS.md)

欢迎通过 [Issues](https://github.com/silent07137/mizu_widget/issues) 反馈问题，请附上 Android 版本、桌面应用和复现步骤。

## 许可证

本项目采用 **GNU General Public License version 2 only（GPL-2.0-only）**，详见 [LICENSE](LICENSE)。

Copyright © 2026 silent07137。应用图标与测试图片为本项目自行绘制。

# Mizu Widget

把喜欢的图片，留在 Android 桌面。

离线优先、没有账号和广告的轻量图片小组件。最低支持 Android 9，初版版本为 **0.1.0**。

## 初版功能

- JPG / JPEG、PNG（含透明通道）、WebP 静态图片。
- GIF **静态预览**；此版本没有后台动图播放。
- 多个桌面组件分别保存图片与设置。
- 完整显示、居中裁剪、拉伸铺满。
- 0–64 dp 圆角、0–100% 图片不透明度。
- 透明、白色、深色、浅水绿背景。
- 点击打开设置、查看图片或不执行操作。
- 支持桌面调整尺寸、应用内重新编辑和刷新。
- 系统文件选择器授权；删除组件时清理配置、缓存和不再引用的 URI 授权。
- 应用内关于、完整 GPLv2、源码和第三方许可说明。

## 使用

打开应用 → 添加组件 → 选择图片 → 调整效果 → 添加到桌面 → 在桌面的确认窗口中确认。

也可以长按桌面 → 小组件 → Mizu Widget，再选择图片并保存。需要支持 Android App Widget 的桌面；不支持快捷添加时使用桌面的小组件入口。

图片需要由文件提供方持续提供。删除原图、撤销授权、移除 SD 卡或云文件离线不可用时，组件显示修复入口，可以重新选择。应用不会在公共目录复制用户原图。

## 隐私

图片仅在本机读取和渲染，不上传。APK 没有声明任何权限，包括网络和广泛存储权限，也没有分析、广告、账号或常驻服务。系统文件选择器自身可能展示其他应用的云端文件；这由文件提供方控制。

系统备份与设备迁移均排除本应用数据，防止无法迁移的 URI 授权和私人配置进入备份。

## 构建与测试

需要 JDK 17+、Android SDK 平台 37.0 与 Build Tools 36.0.0。Gradle 9.3.1 Wrapper 和 AGP 9.1.1 已固定版本。

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

在本机的 local.properties 中配置 sdk.dir，或使用 ANDROID_HOME。此文件不能提交。

debug 包名：io.github.silent07137.mizuwidget.debug，与正式包分开安装。

测试使用 Android 平台 Instrumentation，包含渲染像素、内存上限、配置隔离、恢复、清理和实际组件宿主验证。测试脚本仅临时授权测试宿主绑定组件，结束后撤回。测试 APK 中的图片提供方仅生成合成素材，正式 APK 不包含它。

## 技术选择

当前实现采用 Java 17、Android 原生 Views、AppWidgetProvider、RemoteViews、ImageDecoder 和 SharedPreferences JSON。没有第三方运行时依赖。

手册中的 Kotlin / Compose / DataStore 为建议方案。为了保留 GPL-2.0-only，初版避免打包 Apache-2.0 的 AndroidX 或 Kotlin 运行库，采用平台 API；将来引入依赖时需要重新审核许可证。参见 [GNU 许可兼容性说明](https://www.gnu.org/licenses/license-list.html#apache2)。

渲染在串行后台线程完成。解码图片最多约 2 百万像素；传给桌面的位图最多 524,288 像素（约 2 MiB），最大边长 1024 像素，避免将原始大图送入 Binder。组件没有定时刷新或后台循环。

## 仓库与发布

- [开发说明](docs/DEVELOPMENT.md)
- [发布说明](docs/RELEASE.md)
- [验证记录与人工验收清单](docs/TESTING.md)
- [更新记录](CHANGELOG.md)

CI 构建 debug 并运行 API 28 / 36 模拟器测试。Release 工作流只允许手动触发或 v* 标签，使用 GitHub Secrets 临时恢复签名材料，签名并验证 APK / AAB。本地只使用 debug key。

源码仓库不保存 keystore、密码、本机配置、APK、AAB 或私人测试截图。

## License

License: **GPL-2.0-only**

GNU General Public License version 2 only。详见 [LICENSE](LICENSE)。

Copyright © 2026 silent07137。图标和测试素材为本项目自行绘制。Gradle Wrapper 的上游许可见 [构建工具声明](docs/BUILD-TOOLS.md)。

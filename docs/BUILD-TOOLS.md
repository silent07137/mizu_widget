# 构建工具声明

应用 APK 未打包第三方运行库。Android 平台由设备提供，其许可说明位于设备法律信息。

本仓库包含用于启动构建的 Gradle Wrapper（Gradle 9.3.1），由 Gradle 官方发行：

- 版权归 Gradle 作者；上游 [版权与许可](https://github.com/gradle/gradle/blob/v9.3.1/LICENSE)。
- Wrapper 文件沿用上游 Apache License 2.0 声明。
- Wrapper 是独立的构建启动工具，没有链接或打包进 Mizu Widget APK；其许可不改为 GPL。
- distributionSha256Sum 验证下载的官方 Gradle 发行包。

Android Gradle Plugin 9.1.1、JDK 和 GitHub Actions 同样仅用于构建，不随 APK 分发。

项目自行编写的应用源代码、测试代码、脚本及图标采用 GPL-2.0-only。根目录 LICENSE 保留现有官方 GPLv2 正文。

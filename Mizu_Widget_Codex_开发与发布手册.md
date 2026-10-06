# Mizu Widget — Codex 开发与发布手册

> 项目：Mizu Widget  
> 类型：纯 Android 桌面图片 / GIF Widget  
> 开源协议：GNU General Public License version 2 only (`GPL-2.0-only`)  
> 代码仓库操作：**只允许通过 GitHub Connector 操作远端仓库，不使用本地 Git**  
> Release 签名：**只允许在 GitHub Actions 中完成**  
> Release keystore：由项目所有者自行生成并保管，不提交到仓库

---

## 0. Codex 必须遵守的最高优先级规则

### 0.1 GitHub 操作规则

Codex 在本项目中：

- **不得运行任何本地 Git 命令。**
- 不得执行 `git init`、`git clone`、`git status`、`git add`、`git commit`、`git push`、`git pull`、`git fetch`、`git checkout`、`git switch`、`git merge`、`git rebase`、`git remote` 等命令。
- 不得修改本地 `.git/` 目录。
- 不得依赖本地 Git 凭据、SSH Key、Personal Access Token 或 GitHub CLI 来推送代码。
- 所有 GitHub 仓库读取、创建分支、创建 / 更新文件、提交、创建 Pull Request 等操作，**必须使用已连接的 GitHub Connector**。
- 修改已有文件前，应先通过 GitHub Connector 获取远端最新版及其 SHA，避免覆盖别人已经提交的修改。
- 推荐流程：读取远端默认分支 → Connector 创建工作分支 → Connector 创建 / 更新文件 → 检查差异 → Connector 创建 PR。
- 未经明确要求，不直接覆盖默认分支上的重要文件。
- 不把 GitHub token 写入任何文件、日志、源码、Gradle 配置或 Actions YAML。

本地工作区只作为临时编辑和构建环境，不作为 Git 提交来源。

---

## 1. 项目定位

Mizu Widget 是一个尽量纯粹、离线优先的 Android 桌面组件应用。

核心目标：

1. 将 JPG / JPEG / PNG / WebP 等静态图片放置到 Android 桌面。
2. 支持透明 PNG。
3. 支持 GIF / 动图显示。
4. 每一个 Widget 实例拥有独立配置。
5. 用户可调整 Widget 大小、图片缩放模式、圆角、透明度和背景。
6. 默认不联网。
7. 不创建账号体系。
8. 不上传用户选择的图片。
9. 尽量避免常驻后台。
10. UI 保持简单、轻量、无广告干扰。

第一阶段优先保证静态图片 Widget 的稳定性，然后再实现低帧率 GIF 播放。

---

## 2. 建议技术栈

- Kotlin
- Android SDK
- Jetpack Compose：主应用配置界面
- `AppWidgetProvider`
- `RemoteViews`：桌面 Widget
- Android Photo Picker / Storage Access Framework
- DataStore：Widget 配置
- `ImageDecoder` / Android Bitmap API：图片解码
- GIF 采用按帧解码 + 低频更新 RemoteViews 的方案
- Gradle Kotlin DSL：`build.gradle.kts`
- JDK 17 或项目实际要求的版本
- GitHub Actions：CI / Release 构建
- GitHub Connector：远端仓库读写

不要为了简单图片 Widget 引入大型网络框架、账号 SDK、分析 SDK或广告 SDK。

---

## 3. 建议的数据结构

每个 App Widget 单独保存一份配置：

```kotlin
data class WidgetConfig(
    val appWidgetId: Int,
    val imageUri: String,
    val mediaType: MediaType,
    val scaleMode: ScaleMode,
    val cornerRadiusDp: Float,
    val opacity: Float,
    val backgroundColor: Long?,
    val gifEnabled: Boolean,
    val gifFps: Int,
    val clickAction: ClickAction
)
```

建议：

- 不复制用户原图到公共目录。
- 长期访问 URI 时正确处理持久化 URI 权限。
- 为 Widget 创建尺寸适配后的缓存图，而不是一直向 Launcher 发送原始超大 Bitmap。
- Widget 删除时同步清理该实例的缓存和配置。

---

## 4. GIF 实现注意事项

Android App Widget 的核心仍受 `RemoteViews` 限制，不能把普通自定义 View 直接塞进桌面 Widget。

因此 GIF 不应按普通 App 内 GIF View 的方式实现。

建议方案：

1. 读取 GIF。
2. 根据当前 Widget 实际尺寸缩放。
3. 解码当前需要的帧。
4. 缓存少量相邻帧。
5. 以低频率更新 `RemoteViews` 中的图片。

建议默认：

- 1 FPS：极省电
- 2 FPS：默认 / 推荐
- 5 FPS：较流畅
- 原始帧率：仅实验模式，不作为默认

注意：

- 不要把一整张高分辨率 GIF 的全部 Bitmap 帧长期驻留内存。
- 不允许为了 GIF 保持永久高频后台循环。
- 必须处理系统杀进程、Launcher 重建、设备重启、电池优化等情况。
- GIF 功能不能影响静态 Widget 的稳定性。

---

## 5. 严禁上传到 GitHub 的内容

以下内容 **绝对不得进入 Git 历史，也不得通过 GitHub Connector 提交到仓库**：

### 5.1 Android 签名材料

```text
*.jks
*.keystore
*.p12
*.pfx
```

尤其包括：

```text
mizu-widget-release.jks
release.jks
upload.jks
```

这些文件属于私密签名材料。

### 5.2 密码与密钥

禁止提交：

```text
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_PASSWORD
ANDROID_KEY_ALIAS（虽然通常不是秘密，但统一放 Secrets）
API_KEY
TOKEN
ACCESS_TOKEN
PRIVATE_KEY
CLIENT_SECRET
```

以及任何真实值。

禁止写入：

- `build.gradle`
- `build.gradle.kts`
- `gradle.properties`
- README
- Issue
- PR 描述
- Actions 日志
- 测试文件
- 示例文件

### 5.3 本机配置

禁止上传：

```text
local.properties
keystore.properties
signing.properties
secrets.properties
.env
.env.*
```

`.env.example` 可以存在，但里面只能放假值和字段名。

### 5.4 构建产物

默认不提交：

```text
*.apk
*.aab
*.apks
*.idsig
build/
app/build/
.gradle/
```

APK / AAB 应由 GitHub Actions 生成并作为 Actions Artifact 或 GitHub Release 附件提供。

### 5.5 用户数据

不得提交：

- 用户实际选择的照片
- 用户 GIF
- 用户私人截图
- 手机文件路径
- 调试时采集的真实用户数据
- 含电话号码、账号、邮件、地址等个人信息的测试数据

测试素材应使用项目自制或明确允许再分发的素材。

### 5.6 开发凭据

不得提交：

- GitHub PAT
- GitHub App 私钥
- SSH 私钥
- Codex / OpenAI 凭据
- 云服务 Token
- Play Console 凭据
- Firebase / 第三方后台私钥

发现敏感信息进入 GitHub 历史时，不得仅靠“后续删除文件”处理。应立即吊销 / 更换对应密钥，并清理 Git 历史。

---

## 6. `.gitignore` 基线

仓库根目录必须至少包含：

```gitignore
# Android / Gradle
.gradle/
build/
app/build/
**/build/
local.properties
captures/
.externalNativeBuild/
.cxx/

# Android Studio / IntelliJ
.idea/
*.iml

# Release / signing
*.jks
*.keystore
*.p12
*.pfx
*.pem
*.key
keystore.properties
signing.properties
secrets.properties

# Environment secrets
.env
.env.*
!.env.example

# Build artifacts
*.apk
*.aab
*.apks
*.idsig

# OS
.DS_Store
Thumbs.db

# Temporary
*.tmp
*.log
```

注意：`.gitignore` 只是第二道防线。Codex 在上传前仍必须主动检查待提交内容是否包含敏感信息。

---

## 7. GPLv2 许可要求

本项目采用：

```text
GNU General Public License version 2 only
SPDX-License-Identifier: GPL-2.0-only
```

这里明确采用 **GPLv2 only**，而不是自动升级到“GPLv2 or later”。

仓库要求：

1. 根目录必须存在 `LICENSE`。
2. `LICENSE` 使用 GNU GPL version 2 的官方完整文本，不得自行改写许可证正文。
3. README 中明确标注：
   - `License: GPL-2.0-only`
4. 源码文件可酌情加入简短 SPDX 标识：
   ```text
   SPDX-License-Identifier: GPL-2.0-only
   ```
5. 引入第三方依赖前必须检查许可证兼容性。
6. 不引入明显与 GPLv2-only 不兼容的代码或资源。
7. 复制第三方代码时必须保留其版权和许可证声明。
8. 第三方开源组件应在应用内“开源许可”页面列出。

---

## 8. 应用内“关于”页面必须清楚显示

“关于 Mizu Widget”页面不能只写一句“开源软件”。

至少应显示：

```text
Mizu Widget
版本：{versionName}

一个用于在 Android 桌面放置图片与 GIF 的轻量桌面组件应用。

开源协议
Mizu Widget 采用 GNU General Public License version 2
（GPL-2.0-only）发布。

你可以在 GPLv2 条款允许的范围内使用、研究、修改和重新分发本项目源代码。

源代码
{GitHub Repository URL}

第三方开源许可
查看本应用使用的第三方开源组件及其许可证。

Copyright © 2026 {项目作者 / GitHub 用户名}
```

关于页面建议至少提供以下可点击入口：

- `源代码`
- `GNU GPL v2`
- `第三方开源许可`
- `版本信息`

不要使用会让用户误以为应用采用 MIT、Apache-2.0、GPLv3 或专有许可证的模糊文案。

---

## 9. Release 签名原则

### 必须遵守

- Release APK / AAB 只在 GitHub Actions 中签名。
- 本地开发只使用 Android 默认 debug key。
- Release keystore 不进入仓库。
- Release 密码不进入仓库。
- Release keystore 由项目所有者自己生成。
- GitHub Actions 从 GitHub Secrets 临时恢复 keystore。
- Actions Job 完成后不保留临时 keystore。
- 不在 Actions 日志中 `echo` 明文密码。
- 不把 Secrets 输出到构建日志。

Android 官方建议将私钥安全保存，并避免把签名密码直接写入 Gradle 构建文件。

---

## 10. 如何生成 Mizu Widget Release Keystore

在安装了 JDK 的电脑上打开终端。

推荐：

```bash
keytool -genkeypair -v \
  -keystore mizu-widget-release.jks \
  -alias mizu_widget_release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Windows PowerShell 也可写成一行：

```powershell
keytool -genkeypair -v -keystore mizu-widget-release.jks -alias mizu_widget_release -keyalg RSA -keysize 4096 -validity 10000
```

执行过程中会要求设置：

- Keystore password
- 姓名 / 组织等证书信息
- Key password

建议：

- Keystore 密码使用密码管理器生成。
- Key password 可与 Keystore password 不同。
- 不在聊天、源码、笔记截图中公开密码。
- `mizu-widget-release.jks` 至少离线备份两份。
- 密钥库丢失可能直接影响未来版本更新，因此不要只保存在一台电脑。

生成后可检查：

```bash
keytool -list -v -keystore mizu-widget-release.jks
```

只检查信息，不要把输出中的敏感信息随意公开。

---

## 11. 将 Keystore 放入 GitHub Actions Secrets

GitHub Actions Secret 本质上保存字符串，因此 `.jks` 文件应先转换成 Base64 字符串。

### Windows PowerShell

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes((Resolve-Path ".\mizu-widget-release.jks"))
) | Set-Clipboard
```

该命令会把 Base64 内容直接复制到剪贴板。

### Linux

```bash
base64 -w 0 mizu-widget-release.jks
```

### macOS

```bash
base64 < mizu-widget-release.jks | tr -d '\n'
```

然后进入：

```text
GitHub Repository
→ Settings
→ Secrets and variables
→ Actions
→ New repository secret
```

创建下面 4 个 Repository Secrets：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

其中：

### `ANDROID_KEYSTORE_BASE64`

填入：

```text
mizu-widget-release.jks 的完整 Base64 字符串
```

### `ANDROID_KEYSTORE_PASSWORD`

填入生成 keystore 时使用的 Keystore Password。

### `ANDROID_KEY_ALIAS`

推荐：

```text
mizu_widget_release
```

必须与生成密钥时的 `-alias` 完全一致。

### `ANDROID_KEY_PASSWORD`

填入对应 Key Password。

注意：

- Secret 名称不要改成带空格的形式。
- 不要创建名为 `GITHUB_*` 的自定义 Secret。
- 不要把 Base64 文本保存进仓库里的 `.txt`。
- Base64 不是加密，只是编码。真正的保护来自 GitHub Secrets。
- 配置完成后，把临时 Base64 文本安全删除。

---

## 12. Gradle Release 签名配置

推荐让 `app/build.gradle.kts` 从环境变量读取签名参数。

示例：

```kotlin
android {
    // ...

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")

            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

严禁：

```kotlin
storePassword = "真实密码"
keyPassword = "真实密码"
```

严禁：

```kotlin
storeFile = file("mizu-widget-release.jks")
```

并把真实 `.jks` 放进仓库。

---

## 13. GitHub Actions Release 工作流

工作流位置：

```text
.github/workflows/release.yml
```

本手册配套提供了 `release.yml` 模板。

设计原则：

- 手动触发 `workflow_dispatch`
- 打 `v*` Tag 时也可以构建
- 权限最小化：`contents: read`
- 使用 JDK 17
- 使用 Gradle 缓存
- 运行 lint / test
- Actions 中恢复 keystore
- Gradle 在 CI 中签名
- 构建 APK 和 AAB
- 使用 `apksigner verify` 验证 APK 签名
- 最终只上传 APK / AAB Artifact
- 不上传 `.jks`

Secrets 不应对来自不可信 fork 的 PR 执行 Release 构建，因此 Release Workflow 不以 `pull_request` 作为签名触发器。

---

## 14. GitHub Actions 工作流

```yaml
name: Build Signed Release

on:
  workflow_dispatch:
  push:
    tags:
      - "v*"

permissions:
  contents: read

jobs:
  build-release:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v6

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@017a9effdb900e5b5b2fddfb590a105619dca3c3 # v4.4.2

      - name: Restore release keystore
        shell: bash
        run: |
          set -euo pipefail
          printf '%s' "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" \
            | base64 --decode \
            > "$RUNNER_TEMP/mizu-widget-release.jks"
          chmod 600 "$RUNNER_TEMP/mizu-widget-release.jks"

      - name: Build signed release
        env:
          ANDROID_KEYSTORE_PATH: ${{ runner.temp }}/mizu-widget-release.jks
          ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          ANDROID_KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
        run: |
          chmod +x ./gradlew
          ./gradlew clean lint test assembleRelease bundleRelease

      - name: Verify APK signature
        shell: bash
        run: |
          set -euo pipefail

          APK="$(find app/build/outputs/apk/release -type f -name '*.apk' | head -n 1)"

          if [ -z "$APK" ]; then
            echo "Release APK not found."
            exit 1
          fi

          APKSIGNER="$(
            find "$ANDROID_HOME/build-tools" -type f -name apksigner \
              | sort -V \
              | tail -n 1
          )"

          if [ -z "$APKSIGNER" ]; then
            echo "apksigner not found."
            exit 1
          fi

          "$APKSIGNER" verify --verbose "$APK"

      - name: Remove keystore
        if: always()
        shell: bash
        run: |
          rm -f "$RUNNER_TEMP/mizu-widget-release.jks"

      - name: Upload APK and AAB
        uses: actions/upload-artifact@v4
        with:
          name: mizu-widget-release
          if-no-files-found: error
          retention-days: 30
          path: |
            app/build/outputs/apk/release/*.apk
            app/build/outputs/bundle/release/*.aab
```

如果模块名以后不是 `app`，应同步修改输出路径。

---

## 15. Actions 安全要求

Codex 修改 Workflow 时必须检查：

- 不允许 `echo "${{ secrets.* }}"`。
- 不允许 `set -x` 用于含 Secrets 的步骤。
- 不把 Secret 作为 Artifact 上传。
- 不上传 `$RUNNER_TEMP` 整个目录。
- 不缓存 keystore。
- 不在 PR 工作流里给不可信代码开放 Release Secrets。
- `GITHUB_TOKEN` 使用最低权限。
- 优先使用 GitHub 官方 Action。
- 第三方 Action 尽量固定到完整 commit SHA。
- 升级 Action 前检查官方 release / GitHub Docs。
- 构建结束必须删除临时 keystore。

GitHub 会对 Secrets 做日志遮罩，但不能依赖遮罩机制来弥补主动泄漏。

---

## 16. CI 与 Release 建议拆分

建议最终存在两个工作流：

```text
.github/workflows/ci.yml
.github/workflows/release.yml
```

### `ci.yml`

用于：

- push
- pull_request
- lint
- unit test
- debug build

**不读取 Release Secrets。**

### `release.yml`

只用于：

- `workflow_dispatch`
- 正式版本 tag，例如 `v1.0.0`

负责：

- Release keystore
- Release APK
- Release AAB
- 签名校验
- Artifact

这样可以最大程度减少签名密钥暴露面。

---

## 17. Codex 上传代码前的检查清单

每一次通过 GitHub Connector 写入仓库前必须检查：

- [ ] 没有 `.jks`
- [ ] 没有 `.keystore`
- [ ] 没有 `.p12/.pfx`
- [ ] 没有密码
- [ ] 没有 GitHub Token
- [ ] 没有 API Token
- [ ] 没有 `.env`
- [ ] 没有 `local.properties`
- [ ] 没有真实用户图片
- [ ] 没有私人截图
- [ ] 没有 APK / AAB 二进制
- [ ] Gradle 文件不存在硬编码签名密码
- [ ] Actions 不打印 Secrets
- [ ] `.gitignore` 已覆盖敏感文件
- [ ] LICENSE 为 GPL v2 官方文本
- [ ] README 标明 `GPL-2.0-only`
- [ ] 应用“关于”页面标明 GPLv2
- [ ] 新增依赖许可证兼容
- [ ] 修改已有远端文件前已获取最新版 / SHA
- [ ] GitHub 操作使用 Connector，不使用本地 Git

只要其中任何一项不满足，应停止上传并修复。

---

## 18. 推荐仓库结构

```text
Mizu-Widget/
├─ .github/
│  └─ workflows/
│     ├─ ci.yml
│     └─ release.yml
│
├─ app/
│  ├─ src/
│  ├─ build.gradle.kts
│  └─ proguard-rules.pro
│
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
├─ .gitignore
├─ LICENSE
├─ README.md
└─ CODEX.md
```

建议将本手册的“Codex 必须遵守的规则”整理进仓库根目录 `CODEX.md`，让后续 Codex 会话首先读取。

---

## 19. README 必须包含的信息

建议至少包含：

```markdown
# Mizu Widget

A lightweight Android home-screen widget for displaying images and GIFs.

## Features

- Static images
- Transparent PNG
- GIF support
- Multiple independent widgets
- Offline-first
- No account required

## Privacy

Mizu Widget is designed to work locally.
Selected images are not uploaded by the application.

## License

GNU General Public License version 2 only.

SPDX-License-Identifier: GPL-2.0-only
```

不要声称“绝对不联网”，除非最终 APK 中确实不存在网络功能和会联网的第三方 SDK。

---

## 20. 隐私与权限原则

由于 Mizu Widget 的核心只是用户主动选择图片：

- 优先使用 Android Photo Picker / SAF。
- 不申请不必要的广泛存储权限。
- 不申请网络权限，除非未来新增确实需要联网的功能。
- 不申请定位、通讯录、电话、短信、麦克风、相机等无关权限。
- 每增加一个权限，都必须在开发文档中说明为什么需要。
- 不做遥测或行为分析，除非未来项目方明确决定加入并同步修改隐私说明。

---

## 21. Release 版本流程

推荐：

1. 完成功能。
2. CI 通过。
3. 更新 `versionCode`。
4. 更新 `versionName`。
5. 更新 Changelog。
6. 通过 GitHub Connector 提交并合并。
7. 创建形如：
   ```text
   v1.0.0
   ```
   的 Tag。
8. GitHub Actions 自动触发 Release 构建。
9. Actions 临时恢复 keystore。
10. 构建并签名 APK / AAB。
11. 验证 APK 签名。
12. 上传 Actions Artifact。
13. 从 Actions 下载成品做安装测试。
14. 通过后再发布正式 Release。

签名材料始终不应进入仓库。

---

## 22. 关于签名密钥的灾难恢复

需要长期保留：

```text
mizu-widget-release.jks
Keystore Password
Key Alias
Key Password
```

建议保存在：

- 一份主备份
- 一份离线备份
- 密码管理器中的密码记录

不要：

- 只放 GitHub Secrets
- 只放电脑桌面
- 只存在聊天记录
- 把 `.jks` 当普通项目文件提交

GitHub Secrets 不是密钥库的唯一备份方案。

---

## 23. Codex 的最终执行约束摘要

Codex 开发 Mizu Widget 时，应始终遵守：

```text
1. Android / Kotlin。
2. 项目保持小而纯粹。
3. 用户图片默认本地处理。
4. 不随意添加网络、广告、分析、账号 SDK。
5. GPL-2.0-only。
6. 应用 About 页面明确标注 GPLv2 和源码地址。
7. 不上传签名文件。
8. 不上传密码或 Token。
9. 不上传 APK / AAB 到源码仓库。
10. Release 签名只在 GitHub Actions 中进行。
11. Keystore 由项目所有者生成。
12. Keystore 以 Base64 形式保存到 GitHub Actions Secret。
13. 密码 / Alias 使用独立 Secrets。
14. CI 与签名 Release Workflow 分离。
15. 所有 GitHub 写操作使用 GitHub Connector。
16. 禁止使用用户本地 Git。
17. 修改远端文件前读取最新版和 SHA。
18. 上传前执行敏感信息检查。
```

---

## 24. GitHub Connector 上传策略

当仓库已经确定后，Codex 应通过 GitHub Connector：

1. 获取仓库默认分支。
2. 读取远端现有：
   - `.gitignore`
   - `README.md`
   - `LICENSE`
   - `CODEX.md`
   - `.github/workflows/*`
3. 不盲目覆盖已有内容。
4. 创建工作分支，例如：
   ```text
   codex/mizu-initial-setup
   ```
5. 使用 Connector 创建 / 更新：
   - `.gitignore`
   - `CODEX.md`
   - `.github/workflows/release.yml`
   - 需要时再创建 `LICENSE`
6. 检查远端内容。
7. 创建 Pull Request。
8. 在 PR 中说明：
   - GPLv2-only
   - signing secrets 名称
   - keystore 未提交
   - release signing 在 Actions 中完成

整个过程不得调用本地 Git。

---

## 25. 必须配置的 GitHub Secrets 总表

| Secret | 内容 | 是否允许进仓库 |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | `.jks` 的 Base64 | ❌ |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore 密码 | ❌ |
| `ANDROID_KEY_ALIAS` | Key alias | ❌ |
| `ANDROID_KEY_PASSWORD` | Key 密码 | ❌ |

仓库中只允许出现这些 **Secret 名称**，绝不允许出现真实 Secret 值。

---

## 26. 最后的安全底线

**永远不要为了“方便构建”把 Release Keystore 或密码提交到 GitHub。**

即使仓库目前是 Private，也应按照未来可能公开的标准管理敏感信息。

Mizu Widget 的源码可以公开，签名身份必须私密。


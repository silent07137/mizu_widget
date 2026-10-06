# 发布说明

正式签名仅在 GitHub Actions 中完成。不要将上级目录的签名材料复制进本项目。

## Secrets

在仓库 Settings → Secrets and variables → Actions 配置：

| 名称 | 含义 |
| --- | --- |
| ANDROID_KEYSTORE_BASE64 | 所有者现有 keystore 的完整 Base64 |
| ANDROID_KEYSTORE_PASSWORD | Keystore 密码 |
| ANDROID_KEY_ALIAS | Keystore 中实际的签名 alias |
| ANDROID_KEY_PASSWORD | 对应私钥密码 |

不要在聊天、PR、日志或源码中粘贴真实值。Secret 配置入口见 [GitHub 官方说明](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)。

## 流程

1. CI 和真机验收通过。
2. 更新 versionCode、versionName、CHANGELOG。
3. 审核并合并工作分支。
4. 手动运行 Build Signed Release，或创建形如 v0.1.0 的标签。
5. 工作流在临时目录恢复 keystore，执行 lint、构建、签名及 apksigner 验证，最终删除 keystore。
6. 下载 mizu-widget-release Artifact，包含 APK、AAB 与对应源码压缩包。
7. 安装验证后再发布正式 Release；同时提供该提交的源码，满足 GPLv2 的对应源码要求。

Release 工作流不接收 PR 触发，不对 fork 代码提供签名 Secrets，contents 权限为 read。工作流不会自动创建公开 Release。

在本地执行 assembleRelease 只会得到未正式签名的构建；项目仅在 GITHUB_ACTIONS=true 时配置发布签名。本地开发与测试使用 assembleDebug。

debug 与 release 包名不同；不要以 debug APK 的升级测试代替正式签名 APK 的升级验收。

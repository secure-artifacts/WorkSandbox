# 工作沙盒 (WorkSandbox)

## 这是什么

一个极简的 Android 应用，唯一功能：触发系统自带的「创建工作资料（Work Profile）」流程。

不冻结/解冻应用，不做统计分析，不做跨资料文件转发，不依赖任何第三方开源库或工具（如 Island / Insular / Shelter）。所有逻辑均直接调用 Android 官方公开 API：`android.app.admin.DevicePolicyManager`。

创建完成后，设备上会出现一个独立的「工作资料」沙盒（系统内部是一个新的 Android 用户身份），可在其中单独安装应用（例如 Facebook、Instagram、Messenger、WhatsApp），与个人空间、三星「安全文件夹」三者完全独立、互不影响。

**重要：工作资料由系统 `system_server` 维护，一旦创建完成，即使卸载本应用，工作资料依然存在。** 如需移除，请到「设置 - 账户与备份 - 管理用户」或「设置 - 安全和隐私 - 设备管理应用」中操作，或在本 App 内点击对应入口跳转设置页查看。

## 使用方法

1. 安装 APK（见下方构建说明，或从 GitHub Actions 构建产物下载）
2. 打开 App，点击「创建工作模式（工作资料）」
3. 接下来会进入 **Android 系统自带**的引导界面（非本 App 绘制），按提示完成
4. 完成后，桌面会出现带「公文包」角标的应用图标区域，这就是工作资料
5. 在工作资料内打开 Play 商店，搜索并安装需要隔离的应用

## 权限说明

- `android.permission.BIND_DEVICE_ADMIN`：系统强制要求，仅系统本身可触发设备管理员组件，第三方应用无法伪造调用
- 无其他敏感权限（不读取通讯录、位置、存储等）

## 本地构建

```bash
git clone https://github.com/secure-artifacts/WorkSandbox.git
cd WorkSandbox
gradle wrapper --gradle-version 8.2   # 若本机没有 gradlew，先生成
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 发布新版本（触发 GitHub Actions 自动构建）

本仓库的 `.github/workflows/release.yml` 由 **Git tag 推送**触发，不是普通的代码推送：

```bash
git tag -a v1.0.0 -m "首个版本"
git push origin v1.0.0
```

推送 tag 后：

1. 进入仓库 **Actions** 标签查看构建进度
2. 构建成功后，进入仓库 **Releases** 页面，会看到自动创建的 Release，附带：
   - `WorkSandbox-v1.0.0.apk`（构建产物）
   - "Verified" 标记（Artifact Attestation，证明该文件确实由本仓库的 CI 构建，未被篡改）
3. 下载该 APK 传到手机安装（需在系统设置中临时允许「安装未知来源应用」）

### 验证 APK 来源（可选，供下载者使用）

```bash
gh attestation verify WorkSandbox-v1.0.0.apk --repo secure-artifacts/WorkSandbox
```

验证成功表示该文件确实由本仓库的官方 CI 构建，未经篡改。

## 已知限制

- 仅做最基础的工作资料创建，不包含应用冻结、统计、跨资料分享等增强功能
- Debug 签名，仅供个人测试使用，不适合分发给他人
- 三星设备 One UI 对工作资料的呈现可能与原生 Android 略有差异（图标角标样式、设置入口路径），属正常现象

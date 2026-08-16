# 资源清理记录

状态：`historical cleanup record`
最后复核：2026-08-11

本次整理从最新公共 `main` 建立 `maintenance/resource-cleanup` 分支，目标是减少
仓库中的过时设计导出和重复图片，同时不影响 APK、Figma 交接或历史审计的可追溯性。

## 资源分层

### 运行时资源

Android 只从 `app/src/main/res` 读取资源。当前自适应图标使用
`drawable-nodpi` 下的三层轻量 WebP 和 `mipmap-anydpi-v26` 的 XML 声明；
`docs/` 中的图片不会进入 APK，也不会被 Gradle 打包。

### 当前设计源

`docs/design/icon-source/` 只保留以下交接输入：

- `background.svg`
- `foreground.svg`
- `monochrome.svg`
- `daily-record-icon-1254.png`（用户确认的高分辨率源图）
- `google-play-icon-512.png`（Play Console 导出）

三层 PNG 副本、1024px 备份和自适应预览只是派生导出，不被 Android、Figma 或发布
流程引用，已删除。需要比较旧导出时，使用 Git 历史而不是把副本重新放回当前目录。

### 历史证据

`docs/product/audit/` 仍保留审计报告及其必要截图，因为它们记录了测试输入、设备和
结论。旧的 `docs/product/assets/hand-brew-v2/` 仅自慰截图包与当前双模块产品不再
一致，已从工作树移除；对应文件可以从删除前的 Git 提交恢复。

## 本次删除

- 19 张仅自慰 v2 Figma、Canva 和 API 34 截图。
- 图标 `background.png`、`foreground.png`、`monochrome.png` 三层 PNG 副本。
- `adaptive-preview-1024.png`、`figma-handoff-preview.png` 两张本地预览图。
- `daily-record-icon-1024.png` 重复备份图。

删除内容仅属于文档/设计资产，不改变业务代码、数据库、同步、Manifest 或运行时
图标。删除前的版本仍由 Git 提交历史保留，不需要把大图复制到桌面或 APK 中。

## 生成目录

根目录 `build/`、`app/build/`、`.gradle/`、`.kotlin/` 和 `node_modules/` 是本地
生成物或依赖缓存，已由 `.gitignore` 排除，`git ls-files` 不会包含它们。它们可由
开发者在本机按需清理或重新生成；本次 PR 不把这些缓存提交到公共仓库。

## 验证要求

资源清理完成后至少运行：

```powershell
git diff --check
node --test scripts/docs-integrity.test.mjs
node --test scripts/copy-integrity.test.mjs
node --test scripts/release-metadata.test.mjs
git ls-files | rg -i 'google-services\.json|local\.properties|keystore|\.apk$|\.aab$|\.jks$|\.log$'
```

资源和文档不参与 Android 业务逻辑，因此不因删除历史图片机械重复同步、数据库或
设备测试；若运行时 `res/` 或发布配置被改动，再按 `docs/TESTING.md` 扩大验证范围。

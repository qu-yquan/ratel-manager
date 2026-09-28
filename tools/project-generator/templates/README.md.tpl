# @@PROJECT_NAME@@

该项目由 `ratel-manager/tools/project-generator` 生成。

## 项目信息

- 项目标识：`@@PROJECT_ID@@`
- groupId：`@@GROUP_ID@@`
- 公共平台版本：`@@PLATFORM_VERSION@@`
- 公共前端包：`@@GWSU_CORE_RELATIVE_PATH@@`

## 后端模块

```text
@@PROJECT_ID@@-core
business/business-system
business/application/distributed/@@PROJECT_ID@@-system
business/application/single/@@PROJECT_ID@@-application
```

首次构建前，需要保证 `ratel-manager` 的公共 Maven 模块已经安装到本地仓库，或者可以从公司的 Maven 仓库获取。

```bash
mvn validate
mvn clean install
```

## 前端模块

项目只维护独立的 `sub-system`。公共主应用继续使用 `ratel-manager/web/apps/gwsu-main`，公共前端库通过外部 pnpm workspace 使用 `@gwsu/core`。

```bash
cd web
pnpm install
pnpm dev:system
pnpm build:all
```

默认目录布局要求 `@@PROJECT_NAME@@` 与 `ratel-manager` 位于同一个父目录。若移动项目，请只调整 `web/pnpm-workspace.yaml` 中 `gwsu-core` 的相对路径。

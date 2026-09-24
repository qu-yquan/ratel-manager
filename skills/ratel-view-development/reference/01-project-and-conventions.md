# 一、项目结构与开发规范

## 1.1 项目结构

```
web/
├── apps/
│   ├── gwsu-main/           # 主应用（端口 8000）— qiankun master
│   ├── sub-system/          # 子应用-系统管理（端口 8001）
│   └── sub-security/        # 子应用-安全中心（端口 8002）
├── gwsu-core/               # 共享核心库（@gwsu/core）
│   └── src/
│       ├── components/      # 共享组件（ThemeLayout, AuthGate, FileUpload, FileDownloadButton）
│       ├── constants/       # 常量（theme, events, FileScope）
│       ├── hooks/           # Hooks（useAuth, useFileUpload, useFileDownload）
│       ├── stores/          # Zustand 状态（userStore, menuStore, authStore）
│       ├── services/        # 共享 API（route, user, dict, config, file）
│       ├── types/           # 类型定义
│       ├── utils/           # 工具（request, theme, menu, fileUpload, fileDownload）
│       └── index.ts         # 统一导出
├── package.json
└── pnpm-workspace.yaml
```

## 1.2 子应用配置要点

| 配置项 | 主应用 | 子应用 |
|--------|--------|--------|
| base | `/` | `/sub-xxx`（与主应用路由前缀一致） |
| mountElementId | 默认 | 各子应用唯一（如 `sub-system-root`） |
| qiankun | `master: {}`，在 `src/app.tsx` 运行时注册 | `slave: {}` |
| esbuildMinifyIIFE | — | `true`（必须） |

主应用从公开启动配置读取 `micro_app` 字典，动态生成 qiankun 的 `apps` 与 `routes`。
字典值为微应用名，字典标签为开发环境入口；生产环境入口统一按 `/{微应用名}/` 推导。

## 1.3 业务页面目录结构

```
pages/{业务名}/
├── index.tsx                   # 页面主组件
├── index.module.less           # 页面样式
├── types/index.ts              # 类型定义
├── hooks/useXxx.tsx            # 自定义 Hooks（可选）
├── components/                 # 页面级组件
│   ├── XxxPanel/index.tsx + index.module.less
│   └── XxxFormModal/index.tsx + index.module.less
├── services/xxx.ts             # 页面级 API（可选）
└── permissionConstants.ts      # 权限标识常量
```

## 1.4 样式规范

- **样式必须抽离为 `*.module.less`**，禁止 CSS-in-JS、禁止内联样式（动态 style 除外）
- 导入：`import styles from './index.module.less'`，使用：`className={styles.xxx}`
- 颜色使用 CSS 变量（`var(--primary-color)` 等），禁止硬编码
- Less 嵌套不超过 4 层，类名使用 camelCase

**可用 CSS 变量**：`--primary-color`、`--primary-color-light`、`--primary-color-dark`、`--background-color`、`--surface-color`、`--text-color`、`--text-secondary-color`、`--border-color`、`--success-color`、`--warning-color`、`--error-color`、`--info-color`、`--header-bg`、`--sider-bg`、`--menu-text`、`--menu-text-active`、`--menu-bg-active`

## 1.5 命名规范

| 层级 | 命名规则 | 示例 |
|------|---------|------|
| 页面目录 | kebab-case | `dept/` |
| 组件目录 | PascalCase | `DeptTreePanel/` |
| 服务文件 | 小写 | `dept.ts` |
| Hooks 文件 | use + 名称 | `useDeptTree.tsx` |
| CSS 类名 | camelCase | `.deptPage` |

## 1.6 通用规范

- 禁止使用已过时方法
- 全程中文注释和 UI 文案
- 遵循组件化：复用 > 30 行或独立关注点时抽离组件
- 导入顺序：React → 第三方库 → @gwsu/core → Umi → 应用内模块 → 样式
- 使用 Ant Design 6.x + ProComponents 2.x
- 使用 `App.useApp()` 获取上下文化的 `message`/`notification`/`modal`

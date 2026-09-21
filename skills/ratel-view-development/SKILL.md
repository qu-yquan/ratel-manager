---
name: ratel-view-development
description: Ratel前端项目开发规范与指南，包含目录结构、命名规范、组件开发、微前端配置等，只要涉及到前端代码开发必读。
---

# Ratel 前端项目开发技能

## 触发条件

- 涉及前端 TypeScript/TSX/Less 代码的任何改动（**必读**）
- 创建子应用、页面或组件
- 开发菜单页面、新增功能按钮或调整按钮权限
- 创建 API 服务或调用后端接口
- 修改主题、状态管理或事件系统
- 使用 @gwsu/core 共享库

## 文档索引

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [01-project-and-conventions.md](reference/01-project-and-conventions.md) | 项目结构、样式规范、命名规范 | 编写任何前端代码 |
| [02-components-and-pages.md](reference/02-components-and-pages.md) | 组件/页面开发、WCAG 2.1、data-ai-approval | 创建页面或组件 |
| [03-core-library-and-api.md](reference/03-core-library-and-api.md) | @gwsu/core 完整指南、API 规范、状态管理、事件 | 使用共享库、调用接口 |
| [04-list-page-pattern.md](reference/04-list-page-pattern.md) | 列表页模式、操作列、权限控制 | 开发列表页 |
| [05-checklist.md](reference/05-checklist.md) | 开发检查清单 | 自查 |
| [06-menu-permission-description.md](reference/06-menu-permission-description.md) | 菜单 INSERT、功能描述与界面布局写法 | 开发菜单页面或权限按钮 |

## 快速参考

### 开发禁忌

- 禁止使用已标识过期的方法或属性
- 严格遵循 TypeScript 定义规范

### 无障碍规范（WCAG 2.1）

- **优先使用原生 HTML 标签**，原生标签自带无障碍语义
- **无法使用原生标签时，必须使用 ARIA 属性**补充语义
- 图标按钮必须 `aria-label`，装饰性图标 `aria-hidden="true"`
- 详见 [02-components-and-pages.md](reference/02-components-and-pages.md) 第 2.5 节

### 样式规范

- **样式必须抽离成 `*.module.less`**，禁止 CSS-in-JS
- 导入：`import styles from './index.module.less'`
- 使用：`className={styles.xxx}`

### 从 @gwsu/core 导入

```tsx
import { ThemeLayout, useThemeContext, AuthGate, FileUpload, FileDownloadButton, FileScope } from '@gwsu/core';
import { get, post, put, del } from '@gwsu/core';
import { useUserStore, useMenuStore, useAuthStore } from '@gwsu/core';
import { useAuth, useFileUpload, useFileDownload } from '@gwsu/core';
import { EventType, emitEvent, onEvent } from '@gwsu/core';
import { fetchDictValuesBatch, fetchConfigsBatch } from '@gwsu/core';
```

### 文件上传与下载

**禁止自行封装**，使用 @gwsu/core 组件：

```tsx
// 上传（组件方式）
<FileUpload property={{ scope: FileScope.PROTECTED, categorize: 'doc' }} maxSize={50*1024*1024} onChange={(ids) => ...} />

// 下载（组件方式）
<FileDownloadButton fileId={record.fileId} fileName={record.fileName} />

// 上传（Hook方式，自定义UI时）
const { upload, progress, abort } = useFileUpload({ scope: FileScope.PROTECTED });

// 下载（Hook方式）
const { download, progress, abort } = useFileDownload();
```

### 字典与配置批量获取

**一个页面多个字典/配置时，必须只调用一次接口**：

```typescript
const dictMap = await fetchDictValuesBatch(['user_status', 'gender']);
const configMap = await fetchConfigsBatch(['site_name', 'max_upload_size']);
```

### 按钮权限控制

权限标识必须定义为常量（`permissionConstants.ts`），禁止硬编码：

```tsx
<AuthGate buttonKey={PERM_ADD}><Button type="primary">新增</Button></AuthGate>
const canEdit = useAuth(PERM_EDIT);
```

### 菜单配置交付

菜单功能开发完成、新增按钮或调整按钮权限后，必须阅读
[06-menu-permission-description.md](reference/06-menu-permission-description.md)，同步菜单配置，并在交付结果中提供对应的 `security_menu` `INSERT` 语句。

### data-ai-approval 判断标准

**核心**：按钮点击后是否立即触发后端数据变更。需要则加，不需要则不加。

| 需要 | 不需要 |
|------|--------|
| 删除、状态切换、保存/提交 | 新增（打开空表单）、详情、搜索 |

### 子应用布局模板

```tsx
import { ThemeLayout } from '@gwsu/core';
const Layout: React.FC = () => <ThemeLayout><Outlet /></ThemeLayout>;
```

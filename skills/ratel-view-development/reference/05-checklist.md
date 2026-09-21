# 五、开发检查清单

## 5.1 新建子应用

- [ ] 在 `apps/` 下创建应用目录
- [ ] 配置 `config/config.ts`：`base`、`mountElementId`（唯一）、`esbuildMinifyIIFE: true`、`qiankun: { slave: {} }`
- [ ] 配置 `config/routes.ts`
- [ ] 布局使用 `ThemeLayout` 包裹 `<Outlet />`
- [ ] 添加到 `pnpm-workspace.yaml`
- [ ] 主应用 `config.ts` 注册 qiankun app，`routes.ts` 添加 `microApp` 路由
- [ ] 根 `package.json` 添加 dev/build 脚本

## 5.2 新建业务页面

- [ ] `src/pages/{业务名}/` 下创建页面目录
- [ ] `index.tsx` + `index.module.less` + `types/index.ts`
- [ ] `config/routes.ts` 注册路由
- [ ] API 路径包含正确模块前缀
- [ ] 样式使用 `import styles from './index.module.less'`
- [ ] 复杂逻辑抽离为自定义 Hooks

## 5.3 新建组件

- [ ] `components/` 下创建 PascalCase 目录
- [ ] `index.tsx` + `index.module.less`
- [ ] 定义清晰 Props interface
- [ ] 样式使用 CSS 变量，组件 `export default`
- [ ] 事件回调用 `useCallback`
- [ ] 文件上传用 `FileUpload`/`useFileUpload`，禁止自行封装
- [ ] 文件下载用 `FileDownloadButton`/`useFileDownload`，禁止自行封装
- [ ] 文件上传设置 `property`（至少 `scope` + `categorize`）和 `maxSize`

## 5.4 无障碍（WCAG 2.1）检查

- [ ] 交互元素优先使用原生 HTML 标签
- [ ] 无法用原生标签时添加 `role` 和 ARIA 属性
- [ ] 图标按钮有 `aria-label`，装饰性图标有 `aria-hidden="true"`
- [ ] 交互元素可键盘操作
- [ ] 表单字段关联标签（`aria-label` 或 `aria-labelledby`）

## 5.5 样式检查

- [ ] `*.module.less` 格式，无 CSS-in-JS
- [ ] 颜色使用 CSS 变量，未硬编码
- [ ] Less 嵌套 ≤ 4 层，类名 camelCase

## 5.6 主题兼容性

- [ ] 亮色/暗色主题下均正常显示

## 5.7 AI 操作审批标记

- [ ] 保存/删除/状态变更/同步按钮已添加 `data-ai-approval`
- [ ] 纯查看/导航/搜索按钮**未添加**

## 5.8 代码质量

- [ ] TypeScript 无类型错误
- [ ] 无 `console.log` 残留
- [ ] `useEffect` 依赖数组正确，事件监听器正确清理
- [ ] 异步调用已捕获异常

## 5.9 菜单与按钮配置

- [ ] 新菜单或新按钮已同步 `security_menu` 配置
- [ ] 交付结果中已提供对应的 `INSERT` 语句
- [ ] 不知道新菜单的父菜单 ID 时，`parent_id` 使用明确占位符
- [ ] `description` 已按功能介绍和界面布局规范编写
- [ ] 页面总体布局未罗列随权限变化的按钮
- [ ] 没有特殊弹框、抽屉或独立界面的按钮未编写界面布局

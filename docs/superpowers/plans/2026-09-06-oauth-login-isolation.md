# OAuth 登录页初始化隔离 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 阻止 OAuth 登录模式触发后台认证初始化，并让 OAuth 登录成功后直接返回授权端点。

**Architecture:** 主应用布局将登录模式判断提升到认证初始化之前，登录模式只渲染简化布局。OAuth 登录页只调用登录接口并执行浏览器导航，不复用后台用户和菜单初始化流程。

**Tech Stack:** React、TypeScript、UmiJS、Zustand、qiankun

---

### Task 1: 隔离主应用登录模式初始化

**Files:**
- Modify: `web/apps/gwsu-main/src/layouts/index.tsx`

- [ ] **Step 1: 提前计算登录模式**

在 `LayoutRouter` 获取 `location` 后声明：

```tsx
const isLoginPage = location.pathname.includes('/login');
```

- [ ] **Step 2: 跳过登录模式的项目配置请求**

将初始化条件改为：

```tsx
if (!isLoginPage && useUserStore.getState().checkLogin()) {
  useProjectConfigStore.getState().loadConfig().catch(console.error);
}
```

依赖数组包含 `isLoginPage`，并删除渲染分支前重复的 `isLoginPage` 声明。

### Task 2: 精简 OAuth 登录成功流程

**Files:**
- Modify: `web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx`

- [ ] **Step 1: 删除后台初始化依赖**

将 `@gwsu/core` 导入收敛为：

```tsx
import {encryptPassword} from '@gwsu/core';
```

从登录服务导入中删除不再使用的 `LoginToken`。

- [ ] **Step 2: 登录成功后直接跳转**

将成功回调改为无参数同步函数：

```tsx
const handleLoginSuccess = useCallback(() => {
    if (!redirect) {
        message.error('OAuth授权请求已失效，请从应用重新发起授权');
        return;
    }
    message.success('登录成功');
    window.location.href = redirect;
}, [message, redirect]);
```

登录请求成功后调用 `handleLoginSuccess()`，不再保存 token、获取当前用户或加载菜单。

### Task 3: 验证

**Files:**
- Verify: `web/apps/gwsu-main/src/layouts/index.tsx`
- Verify: `web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx`

- [ ] **Step 1: 检查格式与残留引用**

Run:

```bash
git diff --check
rg -n "fetchCurrentUserInfo|useMenuStore|useUserStore|LoginToken" web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx
```

Expected: `git diff --check` 无输出，`rg` 无匹配。

- [ ] **Step 2: 构建相关前端应用**

Run:

```bash
cd web && pnpm --filter gwsu-main build && pnpm --filter gwsu-sub-system build
```

Expected: 两个应用构建成功，无 TypeScript 错误。

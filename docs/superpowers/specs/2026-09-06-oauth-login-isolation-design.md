# OAuth 登录页初始化隔离设计

## 背景

OAuth 登录页运行在主应用和 `gwsu-sub-system` 子应用中。浏览器保留历史后台 token 时，主应用布局会在页面挂载阶段执行需要认证的项目配置初始化。过期 token 返回 401 后，全局请求处理器触发 `TOKEN_EXPIRED`，将 OAuth 页面跳转到后台登录页。

OAuth 登录成功回调还复用了普通后台登录流程，额外获取当前用户信息并加载菜单。这些数据仅用于后台界面初始化，与 OAuth 授权跳转无关。

## 目标行为

- `/login`、`/oauth2/login`、`/oauth2/loginConsent` 等登录模式页面不执行主应用中需要认证的初始化请求。
- OAuth 登录成功后只提示成功并通过 `window.location.href` 跳转到 `redirect` 指定的后端授权端点。
- OAuth 登录页不写入主应用用户 Store，不获取当前用户信息，不加载后台菜单。
- 登录接口仍负责设置 Sa-Token Cookie；浏览器跳转授权端点时按 Cookie 的域和路径规则自动携带。
- 普通后台登录页的用户信息、菜单和登录成功事件流程保持不变。

## 改动范围

### 主应用布局

在布局初始化项目配置前判断当前页面是否属于登录模式。登录模式下跳过 `useProjectConfigStore.loadConfig()`，避免历史 token 触发全局过期跳转。

登录模式判断继续使用当前路径规则，确保 OAuth 登录页和授权确认页都使用简化布局。

### OAuth 登录页

删除 `fetchCurrentUserInfo`、`useMenuStore` 和 `useUserStore` 依赖。登录接口成功后不再保存 token 或初始化后台状态，直接跳转授权端点。

若 `redirect` 缺失，保持现有失效提示并停止跳转。登录失败仍重置验证码状态。

## 验证

- 浏览器存在过期后台 token 时，打开 OAuth 登录页不会请求受保护的项目配置、用户信息或菜单接口，也不会跳往后台登录页。
- 密码登录成功后直接请求 `redirect` 对应的 OAuth 授权端点。
- 普通后台登录流程仍会加载用户信息和菜单。
- 前端 TypeScript 构建通过。

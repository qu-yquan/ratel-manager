# OAuth2 JSON 接口统一响应设计

## 背景与目标

Spring Authorization Server 的 OAuth2 端点由安全过滤器直接输出响应，不经过普通 Controller，因此当前 JSON 是 OAuth2 标准对象，没有项目统一的 `R` 包装。

本次将 OAuth2 JSON 接口的成功和异常响应统一为：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

OAuth2 标准字段保持原名称和含义，整体放入 `data`。项目现有 `R` 的可选 `errCode` 继续遵循全局 Jackson 空值序列化配置，不为 OAuth2 单独增加响应字段。

## 端点边界

需要包装的 JSON 端点：

- `/system/auth/oauth2/token`
- `/system/auth/oauth2/device_authorization`
- `/system/auth/oauth2/introspect`
- `/system/auth/oauth2/revoke`
- `/system/auth/oauth2/jwks`

保持原行为、不包装的浏览器端点：

- `/system/auth/oauth2/authorize`
- `/system/auth/oauth2/device_verification`
- 登录页、授权确认页及相关 `302` 重定向

该边界避免统一包装破坏 OAuth2 浏览器跳转和表单交互。

## 响应契约

### 成功响应

HTTP 状态及 OAuth2 响应头保持 Spring Authorization Server 原行为。原始 OAuth2 JSON 作为 `R.ok(data)` 的 `data`。

Token 成功示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "access_token": "...",
    "refresh_token": "...",
    "scope": "manager.user.info inherit_user_permissions",
    "token_type": "Bearer",
    "expires_in": 7199
  }
}
```

撤销成功原本没有响应体，统一返回 `R.ok()`，即 `data` 为 `null`。

### 异常响应

异常继续使用 OAuth2 原始 HTTP 状态，例如参数错误为 `400`、客户端认证失败为 `401`，并保留 `WWW-Authenticate` 等响应头。

- `code`：HTTP 状态码。
- `msg`：优先使用 `error_description`，没有描述时使用 `error`。
- `data`：保留 `error`、`error_description`、`error_uri` 等 OAuth2 标准字段。

```json
{
  "code": 400,
  "msg": "OAuth 2.0 Parameter: client_id",
  "data": {
    "error": "invalid_request",
    "error_description": "OAuth 2.0 Parameter: client_id",
    "error_uri": "..."
  }
}
```

## 实现结构

### 统一序列化组件

新增 OAuth2 响应写入组件，使用项目 Jackson `ObjectMapper` 序列化 `R`，统一设置 UTF-8 JSON Content-Type。该组件只负责响应契约，不负责端点业务。

### 端点处理器

通过 Spring Authorization Server 原生扩展钩子配置：

- Token：保留当前登录态写入逻辑，替换最终成功响应；同时配置失败处理器。
- 设备授权：配置成功与失败处理器。
- Token 检查：配置成功与失败处理器。
- Token 撤销：配置成功与失败处理器。

处理器直接从对应 `Authentication` 对象构造 OAuth2 标准数据，避免先生成 JSON 再反序列化。

### JWKS 包装

Spring Authorization Server 没有暴露 JWKS 响应处理器。使用仅匹配配置中 JWKS 精确路径的响应包装过滤器，捕获成功或异常 JSON 后包装。过滤器必须跳过非 JSON、重定向和已经包装的响应。

## 客户端影响

这是有意的非标准外层协议变更。第三方客户端不能再直接从响应根节点读取 `access_token`、`active` 或 JWK Set，必须从 `data` 读取。

同步修改应用管理的对接指南：

- Token 示例从根节点字段调整为 `data.access_token` 等。
- 设备授权响应调整为 `data.device_code` 等。
- 异常示例展示统一包装。
- 静态 HTML 导出与页面展示共用同一份响应模型。

## 测试与验收

- Token 成功响应包含 `code/msg/data`，Token 字段位于 `data`。
- Token 参数错误和客户端认证失败均被包装，并保留原 HTTP 状态。
- 设备授权、Token 检查、撤销成功和失败均被包装。
- JWKS 成功 JSON 被包装。
- `/authorize` 和 `/device_verification` 的跳转或 HTML 不被包装。
- 现有 Token 登录态写入和 Scope/角色继承测试继续通过。
- 对接指南页面和静态 HTML 中的响应示例与新契约一致。

## 非目标

- 不修改 OAuth2 请求参数和授权流程。
- 不包装授权页重定向、HTML 和表单响应。
- 不为旧的无包装响应提供兼容开关；当前没有第三方调用方。

# OAuth 客户端 Subject 信息设计

## 目标

OAuth access token 创建登录态时，`Subject.detail` 保存应用管理查询得到的 `OAuthClientInfoVO`，使业务代码可以读取完整的客户端配置，而不再降级为仅包含客户端 ID 和名称的 `ClientInfo.DefaultClientInfo`。

## 类型设计

`OAuthClientInfoVO` 改为继承 `ClientInfo`。`ClientInfo` 已通过 `Visitor -> BaseDO -> BaseVO` 间接继承 `BaseVO`，因此修改后不会丢失创建时间、修改时间等基础字段，同时满足 `Subject<T extends Visitor>` 的类型约束。

`ClientInfo.DefaultClientInfo` 继续保留，供缺少具体类型信息时的兼容反序列化使用。

## 配置读取

抽取统一的 OAuth 客户端配置查询组件，封装 `OAuthClientApi` 和 `OAuthClientInfoCache`：

- `RatelRegisteredClientRepository` 使用它读取包含密文凭证的完整配置并转换为 `RegisteredClient`。
- `RatelOAuthSubjectWriter` 使用它按 `clientId` 读取同一份应用配置。
- 缓存和 Feign 调用只在该组件中维护，避免两个调用方出现不同的加载行为。

## 敏感信息处理

处理方式与用户密码一致：敏感凭证不进入 Subject，而不是写入后再依赖序列化层隐藏。

客户端配置缓存中的原对象必须保留 `clientSecret`，供 Token 端点验证客户端凭证。写入 Subject 前创建一个新的 `OAuthClientInfoVO`，复制应用配置的其他字段但不设置 `clientSecret`。不得直接修改缓存返回的对象。

最终满足：

- `Subject.detail` 的实际类型为 `OAuthClientInfoVO`。
- 应用管理配置字段可以从 `Subject.clientInfo()` 获取。
- `Subject.detail.clientSecret` 始终为 `null`。
- 客户端认证仍可使用缓存中的密文凭证。

## 异常处理

生成 access token 后若无法再次取得客户端配置，应立即终止 Subject 写入并抛出明确异常，不创建信息不完整的客户端登录态。

## 验证

按项目要求不新增单元测试。通过 Maven 编译验证模块依赖、泛型约束和 Spring Bean 注入关系，并检查 OAuth 授权码及客户端凭证流程使用的配置读取路径保持一致。

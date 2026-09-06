# OAuth Client Subject Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将应用管理中的 `OAuthClientInfoVO` 脱敏后作为 OAuth 登录态的 `Subject.detail`，并保留客户端认证所需的完整缓存配置。

**Architecture:** `OAuthClientInfoProvider` 统一封装 Feign 与 Redis 缓存读取，`RatelRegisteredClientRepository` 使用完整配置构建 Spring Authorization Server 客户端，`RatelOAuthSubjectWriter` 使用同一配置生成不含 `clientSecret` 的 `OAuthClientInfoVO` 副本。`OAuthClientInfoVO` 继承 `ClientInfo`，从而可直接作为 `Subject<ClientInfo>` 的具体主体。

**Tech Stack:** Java 25、Spring Boot 4、Spring Authorization Server、Sa-Token、Feign、Redis

---

### Task 1: 调整客户端主体类型

**Files:**
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/api/oauth/vo/OAuthClientInfoVO.java`

- [ ] **Step 1: 修改继承关系**

将 `OAuthClientInfoVO` 的父类由 `BaseVO` 改为 `ClientInfo`，保留 Lombok `@EqualsAndHashCode(callSuper = true)`。`ClientInfo` 已经通过 `Visitor -> BaseDO -> BaseVO` 提供基础 VO 字段，并要求实现的 `getClientId()`、`getClientSecret()`、`getClientName()` 由 Lombok 生成。

- [ ] **Step 2: 检查类型使用点**

运行：

```bash
rg -n "OAuthClientInfoVO|extends BaseVO" common/common-security common/common-authentication business/business-security
```

预期：`OAuthClientInfoVO` 不再直接继承 `BaseVO`，现有 API、转换器和业务服务仍使用同一个 VO 类型。

### Task 2: 统一客户端配置查询

**Files:**
- Create: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/client/OAuthClientInfoProvider.java`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/client/RatelRegisteredClientRepository.java`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/config/OAuthAuthorizationServerConfiguration.java`

- [ ] **Step 1: 创建 OAuthClientInfoProvider**

新增组件类，构造参数为 `OAuthClientApi` 和 `OAuthClientInfoCache`，提供：

```java
public OAuthClientInfoVO getById(String id)
public OAuthClientInfoVO getByClientId(String clientId)
```

两个方法均通过 `FeignUtils.data(...)` 作为缓存 loader；未查询到时返回 `null`，与 `RegisteredClientRepository` 的契约保持一致。

- [ ] **Step 2: Repository 改用 Provider**

`RatelRegisteredClientRepository` 构造参数调整为 `OAuthClientInfoProvider` 和 `OAuthRegisteredClientConverter`。`findById`、`findByClientId` 获取 VO 后判空并转换，不再直接依赖 Feign API 与缓存。

- [ ] **Step 3: 注册 Provider Bean**

在 `OAuthAuthorizationServerConfiguration` 中新增 `OAuthClientInfoProvider` Bean，并让 `RegisteredClientRepository` Bean 注入 Provider。

### Task 3: 写入脱敏的客户端 Subject

**Files:**
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/api/oauth/vo/OAuthClientInfoVO.java`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/token/RatelOAuthSubjectWriter.java`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/config/OAuthAuthorizationServerConfiguration.java`

- [ ] **Step 1: 提供显式脱敏副本方法**

在 `OAuthClientInfoVO` 中增加静态工厂方法：

```java
public static OAuthClientInfoVO subjectInfo(OAuthClientInfoVO source)
```

该方法创建新实例，显式复制 ID、名称、类型、状态、认证方式、授权模式、回调地址、Scope、PKCE、授权确认、Token 有效期、Refresh Token 复用、备注和基础字段，唯独不复制 `clientSecret`。集合字段使用不可变副本，防止 Subject 与缓存对象共享可变集合。

- [ ] **Step 2: SubjectWriter 查询并脱敏客户端信息**

`RatelOAuthSubjectWriter` 注入 `OAuthClientInfoProvider`。将原先创建 `ClientInfo.DefaultClientInfo` 的逻辑替换为：按 `registeredClient.getClientId()` 查询完整配置，断言查询结果不为空，再调用 `OAuthClientInfoVO.subjectInfo(...)`。

- [ ] **Step 3: 更新 Bean 构造参数**

在 `OAuthAuthorizationServerConfiguration` 创建 `RatelOAuthSubjectWriter` 时传入 `OAuthClientInfoProvider`，保持 Spring 注入链完整。

### Task 4: 编译与静态验证

**Files:**
- Verify: `common/common-security`
- Verify: `common/common-authentication`
- Verify: `business/business-security/business-security-server`

- [ ] **Step 1: 检查格式与敏感字段路径**

运行：

```bash
git diff --check
rg -n "DefaultClientInfo|setClientSecret" common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth
```

预期：无空白错误；OAuth Subject 写入链路不再创建 `DefaultClientInfo`，也不向 Subject 客户端副本设置 `clientSecret`。

- [ ] **Step 2: 编译相关后端模块**

运行：

```bash
mvn -pl common/common-authentication,business/business-security/business-security-server -am -DskipTests compile
```

预期：Maven 输出 `BUILD SUCCESS`。按项目约定不新增或运行单元测试。

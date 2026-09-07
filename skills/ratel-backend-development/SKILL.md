---
name: ratel-backend-development
description: Ratel后端项目开发规范与指南，包含目录结构、命名规范、公共模块使用等，只要涉及到后端代码开发必读。
type: skill
---

# Ratel 后端项目开发技能

## 触发条件

- 涉及后端 Java 代码的任何改动（**必读**）
- 创建业务模块、API 接口、Domain/Service/Controller/Mapper
- 创建数据库表或 SQL 脚本
- 使用 common 模块工具类

## 文档索引

### 基础规范

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [01-project-and-conventions.md](reference/01-project-and-conventions.md) | 项目结构、命名规范、SQL规范、错误码 | 编写任何后端代码 |
| [02-layer-architecture.md](reference/02-layer-architecture.md) | Domain/DTO/VO/Mapper/Service/Controller 分层 | 创建实体类、数据访问层 |

### Common 模块详解

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [03-01-common-core.md](reference/03-01-common-core.md) | 核心工具与领域基类 | 使用 AssertUtils/R/SpringUtils 等基础工具 |
| [03-02-common-api.md](reference/03-02-common-api.md) | @ApiClient 跨模块调用框架 | 跨模块服务调用 |
| [03-03-common-cache.md](reference/03-03-common-cache.md) | Redis 缓存与分布式锁 | 缓存操作、分布式锁、ID生成 |
| [03-04-common-database.md](reference/03-04-common-database.md) | 数据库与多数据源 | 多数据源、动态SQL、元数据 |
| [03-05-common-deploy.md](reference/03-05-common-deploy.md) | 双部署模式 | 单体/分布式模式切换 |
| [03-06-common-security.md](reference/03-06-common-security.md) | ABAC 权限与安全 | 安全校验、会话、数据权限 |
| [03-07-common-authentication.md](reference/03-07-common-authentication.md) | 认证体系 | 登录处理器、拦截器、数据资源范围 |
| [03-08-common-log.md](reference/03-08-common-log.md) | 操作日志 | 日志记录、忽略日志 |
| [03-09-common-job.md](reference/03-09-common-job.md) | 任务调度（common-job） | @XxlJob Handler、XxlJobHelper、分片广播、注册冲突检测 |

### 速查与专题

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [04-utils-quick-reference.md](reference/04-utils-quick-reference.md) | 工具类与注解速查表 | 快速查找可用工具 |
| [05-security-and-permission.md](reference/05-security-and-permission.md) | 安全与权限体系专题 | ABAC/表模型/字段脱敏/数据资源 |
| [06-checklist.md](reference/06-checklist.md) | 开发检查清单 | 自查 |

## 快速参考

### 工具类使用优先级

1. **本项目 common 模块工具类**（最高优先级）
2. Spring Boot 自带工具类
3. Apache Commons 工具类
4. Hutool 工具类（最低优先级）

### 统一响应

```java
R.ok(data)              // 成功带数据
R.fail("msg")           // 失败带消息
throw new BusinessException(XxxErrorCode.E00001);  // 业务异常
AssertUtils.hasText(name, XxxErrorCode.E00001);     // 参数校验
```

### 枚举选项接口

后端存在枚举且前端需要下拉框、筛选项或状态展示时，必须提供枚举选项接口作为唯一数据源，禁止前端重复手写枚举选项。接口返回统一使用 `common-core` 的 `KeyValue<K,V>`，通常约定 `key=枚举编码`、`value=展示名称`。

### 部署模式

| 模式 | 调用方式 | HTTP 客户端 | 检测方式 |
|------|---------|------------|---------|
| 单体 | `LocalApiClientFactory` | 无（本地调用） | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP + 熔断 | `WebClient` | 自动检测 Nacos |

### Controller 必须注解

```java
@RestController
@RequestMapping("xxx")
@Tag(name = "XXX管理")
@TableModelPermission({XxxEntity.class})  // 必须声明表模型权限
```

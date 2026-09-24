# AGENTS.md

本文件为 Codex (Codex.ai/code) 在此代码仓库中工作时提供指导。

## 开发前必读

- **修改后端代码前**：先查看 `ratel-backend-development` 技能
- **修改前端代码前**：先查看 `ratel-view-development` 技能

## 构建命令

### 后端（Maven）

```bash
mvn clean install                                    # 构建所有模块
mvn clean install -pl business/application/distributed/gwsu-security -am  # 构建指定模块
mvn spring-boot:run -pl business/application/single/gwsu              # 运行单体应用
mvn spring-boot:run -pl business/application/distributed/gwsu-security # 运行分布式微服务（需 Nacos 127.0.0.1:8848）
mvn clean install -DskipTests                       # 跳过测试构建
```

### 前端（pnpm）

```bash
cd web
pnpm install           # 安装依赖
pnpm dev:all           # 并行启动所有应用
pnpm build:all         # 构建所有
pnpm clean             # 清理 node_modules、dist、.umi
```

## 项目架构

全栈项目，Java 后端 + 前端，支持单体/分布式双部署模式。

### 后端

Spring Boot 4.0.3 / Java 25 多模块 Maven 项目。

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.3 | 应用框架 |
| Spring Cloud Alibaba | 2025.1.0.0 | Nacos 服务发现与配置 |
| MyBatis Plus | 3.5.16 | ORM，多数据源 |
| Redisson | 4.3.0 | Redis 客户端 |
| Resilience4j | 2.4.0 | 熔断器 |
| jCasbin | 1.99.0 | ABAC 权限 |
| Sa-Token | 1.45.0 | 认证 |

**模块结构**：

```
root-pom/                    # 根 POM
project-pom/                 # 内部模块版本管理
common/                      # 公共基础设施（8个模块，详见 ratel-backend-development 技能）
business/                    # 业务模块
├── business-xxx/
│   ├── business-xxx-api/    # API 接口、VO、DTO
│   └── business-xxx-server/ # Domain、Mapper、Service、Controller
└── application/
    ├── distributed/         # 微服务应用
    └── single/gwsu          # 单体应用
```

**部署模式**：`DeployInitializer` 自动检测 Nacos → 单体用 `LocalApiClientFactory`（Bean 查找），分布式用 `RemoteApiClientFactory`（HTTP + 熔断）。

### 前端

UmiJS 4 + qiankun 微前端，pnpm monorepo。

| 应用 | 角色 | 端口 |
|------|------|------|
| gwsu-main | 主应用 | 8000 |
| sub-system | 系统管理子应用 | 8001 |
| sub-security | 安全中心子应用 | 8002 |

共享库 `@gwsu/core`：ThemeLayout、6种主题、AuthGate、FileUpload/Download、状态管理、事件系统。

## 代码编写规范

### 后端

1. 禁止使用已过时的方法
2. 所有代码必须是生产级别
3. 涉及动态代理/反射必须考虑 AOT 兼容性（`@ImportRuntimeHints` + `RuntimeHintsRegistrar`）

### 前端

1. 禁止使用已过时的方法
2. 所有代码必须是企业级别
3. 样式必须抽离成 `*.module.less`，禁止 CSS-in-JS
4. 严格遵循组件化思想

## 其他配置

1. 本地 Maven 仓库地址（反编译查看源码可以从该目录下查找）：`/Users/quyq/Documents/work/m2/respository`
2. 全程用**中文**

# common-ai 动态 Skill 与 MCP 使用指南

需要告诉智能助手如何使用某项业务能力时，在对应业务 `server` 模块中注册动态 Skill。

业务代码只需要编写工具、Skill 使用说明和 `AgentSkillProvider`，不要修改智能体实现，也不要操作动态 Skill 的内部注册机制。

## 1. 引入依赖

```xml
<dependency>
    <groupId>org.quyq.gwsu</groupId>
    <artifactId>common-ai</artifactId>
</dependency>
```

## 2. 最小完整示例

先编写供 AI 调用的工具。工具必须是 Spring Bean：

```java
@Component
public class KnowledgeSearchTool {

    @Tool(
            name = "SearchKnowledge",
            description = "根据关键词检索业务知识",
            readOnly = true)
    public Mono<String> search(
            @ToolParam(name = "query", description = "检索关键词") String query) {
        return Mono.fromSupplier(() -> "检索结果");
    }
}
```

再通过 `AgentSkillProvider` 注册 Skill：

```java
@Component
@RequiredArgsConstructor
public class KnowledgeAgentSkillProvider implements AgentSkillProvider {

    private final KnowledgeSearchTool knowledgeSearchTool;

    @Override
    public void configure(AgentSkillBuilder builder) {
        builder.skill(
                "knowledge_search",
                skill -> skill
                        .description("查询制度、操作手册和 FAQ 等业务知识")
                        .content(this::skillContent)
                        .tool(knowledgeSearchTool));
    }

    private String skillContent() {
        return """
                # 业务知识检索

                ## 何时使用
                用户询问制度、业务规则、操作手册或 FAQ 时使用。

                ## 使用方法
                1. 从用户问题中提取准确的检索关键词。
                2. 调用知识检索工具。
                3. 仅依据检索结果回答，并标明依据。
                4. 没有可靠结果时明确说明无法确认，不要编造。
                """;
    }
}
```

主要类型位于：

```java
import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillBuilder;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceProvider;
```

## 3. Skill 内容写法

Skill 的作用是告诉 AI 如何使用业务能力，只写以下内容：

```markdown
# Skill 名称

## 何时使用
说明适用场景和不适用场景。

## 使用方法
1. 需要先收集哪些信息。
2. 按什么顺序调用工具。
3. 如何处理工具返回结果。

## 注意事项
- 哪些操作需要用户确认。
- 信息不足或工具失败时如何处理。
- 哪些结论不能推断或编造。
```

不要在 Skill 中写 Java 实现、接口地址、缓存、注册中心等框架细节，也不要存放密钥或敏感数据。

## 4. 常用注册方式

`AgentSkillProvider` 只需要实现 `configure(AgentSkillBuilder builder)`。Provider 可以只注册 Skill、只注册独立工具，或者同时注册两者。

注册不依附 Skill、始终可供 AI 调用的独立工具：

```java
@Override
public void configure(AgentSkillBuilder builder) {
    builder.tool(toolBean);
}
```

注册整个 `@Tool` Bean：

```java
skill.tool(toolBean)
```

只开放 Bean 中指定的工具，白名单填写 `@Tool.name()`：

```java
skill.tool(toolBean, "QueryOrder", "GetOrderDetail")
```

注册已有的 `AgentTool`：

```java
skill.tool(agentTool)
```

需要指定版本、权限和附属资源时：

```java
builder.skill("order", skill -> skill
        .description("查询和处理订单")
        .version("2.0.0")
        .content(this::skillContent)
        .requiredPermission("order:read")
        .resources(resourceProvider)
        .tool(orderTool));
```

`version` 默认是 `1.0.0`，附属资源默认为空，无需显式配置。

需要长篇规则、模板或参考资料时，实现 `SkillResourceProvider`：

- `list()` 声明资源信息。
- `readText()` 或 `readBinary()` 按相对路径懒加载资源。
- 资源路径使用 `references/rules.md` 这类规范化相对路径。
- 用户相关资源应根据 `SkillResourceContext` 再次校验权限。
- `SkillResourceManifest` 位于 `skill.dynamic.model` 包。

## 5. 必须遵守的约束

- 业务代码只填写局部 `skillId`，例如 `knowledge_search`；框架会自动注册为 `{applicationName}.knowledge_search`。
- 不要在业务代码中读取、硬编码或手动拼接 `applicationName`，传入已带应用名前缀的 ID 会启动失败。
- 同一应用内的局部 `skillId` 必须唯一，框架补齐应用名前缀后形成全局唯一 ID。
- `description` 简洁说明 Skill 能解决什么问题，帮助 AI 判断是否使用。
- Skill 或附属资源内容变化时更新 `version`。
- 工具名称、描述和参数说明必须清晰、稳定。
- 查询工具设置 `readOnly = true`；写操作遵循审批机制。
- 工具内部必须执行服务端权限、参数和数据范围校验。
- Skill 文本只能指导 AI，不能代替服务端安全校验。
- `requiredPermission` 没有额外要求时传 `null`；需要时填写稳定的业务权限标识。

动态注册和分布式调用等实现原理统一放在 `docs/智能助手动态技能注册与工具加载设计.md`，编写业务 Skill 时不需要展开。

## 6. 开发 MCP Tool Endpoint

需要通过 MCP 协议向外部客户端提供工具时，在 Spring Bean 类上增加 `@McpServerEndpoint`，方法继续使用 AgentScope 的 `@Tool` 和 `@ToolParam`：

```java
@McpServerEndpoint(
        value = "knowledge",
        name = "知识库 MCP",
        version = "1.0.0",
        instructions = "提供业务知识检索能力")
@RequiredArgsConstructor
public class KnowledgeMcpEndpoint {

    private final KnowledgeService knowledgeService;

    @Tool(
            name = "SearchKnowledge",
            description = "根据关键词检索业务知识",
            readOnly = true)
    public Mono<String> search(
            @ToolParam(name = "query", description = "检索关键词") String query,
            RuntimeContext runtimeContext) {
        return knowledgeService.search(
                runtimeContext.getUserId(), query);
    }
}
```

Endpoint 所属模块由类所在包对应的 `BusinessModuleInfoProvider` 自动识别。访问地址为：

```text
单体应用：POST /{modulePrefix}/mcp/{@McpServerEndpoint.value}
微服务内部：POST /mcp/{@McpServerEndpoint.value}
微服务经网关：POST /{modulePrefix}/mcp/{@McpServerEndpoint.value}
```

假设上例位于 `security` 业务模块，单体应用或经网关访问时地址为
`POST /security/mcp/knowledge`，直接调用 security 微服务时地址为
`POST /mcp/knowledge`。基础路径可统一配置：

```yaml
ratel:
  ai:
    mcp:
      enabled: true
      base-path: /mcp
      request-timeout: 30s
      session-id-header: X-Agent-Session-Id
```

约束：

- `value` 在当前业务模块中必须唯一，只能使用字母、数字、下划线和短横线，且以字母开头。
- 微服务应用必须且只能提供一个 `BusinessModuleInfoProvider`；单体应用会按 Endpoint 类所在业务包匹配 Provider，无法匹配或匹配多个时启动失败。
- Endpoint 至少声明一个 `@Tool` 方法。
- `tools` 属性可按 `@Tool.name()` 设置工具白名单，留空表示暴露类上的全部工具。
- 工具需要用户信息时直接声明 `RuntimeContext` 参数；框架从已认证请求取得 `userId`，从 `X-Agent-Session-Id` 请求头取得 `sessionId`。
- MCP Endpoint 使用 RouterFunction，不会被当作普通 Controller 收集权限，但工具内部仍必须执行权限、数据范围和参数校验。
- `@McpServerEndpoint` 不会把工具注册到 Redis 或智能体 Toolkit；需要供内部智能体使用时，必须另行通过 `AgentSkillProvider` 显式注册。
- 当前只支持 Stateless Streamable HTTP 的 Tool 能力，不支持 Resource、ResourceTemplate、Prompt 和有状态 Session。

常用类型：

```java
import org.quyq.gwsu.common.ai.mcp.annotation.McpServerEndpoint;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
```

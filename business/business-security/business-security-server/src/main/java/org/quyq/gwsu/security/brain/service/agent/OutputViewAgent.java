package org.quyq.gwsu.security.brain.service.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventEmitter;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.ai.skill.InMemoryAgentSkillRepository;
import org.quyq.gwsu.security.brain.service.middleware.OutputViewEventHandlerMiddleware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 隔离执行的视图输出智能体工具
 * 将回复内容以精美的可视化界面展示给用户
 * 输出 json-render spec，通过 AGENT_OUTPUT 自定义事件发送到前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutputViewAgent implements AgentTool {

    public static final String AGENT_NAME = "OutputViewAgent";

    private static final String SKILL_RESOURCE_PATH = "skills";
    private static final String SESSION_PREFIX = "output-view--";
    private static final String DESCRIPTION = """
            前端展示工具：将数据以可视化UI展示给用户。调用时必须传入「标题」和「展示的数据内容」（如SQL查询结果、统计表格等）。数据格式建议Markdown表格或JSON。
            注意：调用此工具后，内容已直接在可视化面板中展示给用户，你不需要再以文字形式重复输出相同信息。
            ⚠️ 覆盖机制：每次调用本工具会整体替换前一次的展示内容，而非追加。因此在同一轮对话中，禁止对本工具发起多次调用；正确的做法是将所有需要展示的数据汇集后，一次性调用本工具完整输出，包括想要展示多项内容。
            """;


    private final ObjectProvider<Toolkit> toolkitProvider;

    private final AgentStateStore agentStateStore;

    /**
     * 构建视图输出智能体
     */
    private ReActAgent build() {
        Toolkit toolkit = toolkitProvider.getIfAvailable(Toolkit::new);

        // 构建技能盒子
        SkillBox skillBox = buildSkillBox(toolkit);

        return ReActAgent.builder()
                .name(AGENT_NAME)
                .sysPrompt(buildSystemPrompt())
                .stateStore(agentStateStore)
                .middlewares(List.of(new OutputViewEventHandlerMiddleware()))
                .model(ModelProvider.generateModel())
                .toolkit(toolkit)
                .skillRepository(new InMemoryAgentSkillRepository(
                        AGENT_NAME,
                        skillBox.getAllSkillIds().stream().map(skillBox::getSkill).toList(),
                        false))
                .build();
    }

    @Override
    public String getName() {
        return AGENT_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "session_id", Map.of(
                                "type", "string",
                                "description", "需要继续上一次输出会话时传入其 session_id"),
                        "message", Map.of(
                                "type", "string",
                                "description", "需要可视化展示的完整内容")),
                "required", List.of("message"));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.deferContextual(contextView -> {
            Map<String, Object> input = param.getInput() != null ? param.getInput() : Map.of();
            String message = valueAsString(input.get("message"));
            if (!StringUtils.hasText(message)) {
                return Mono.just(ToolResultBlock.error("Message is required"));
            }

            RuntimeContext childContext = createChildContext(
                    param.getRuntimeContext(),
                    valueAsString(input.get("session_id")));
            ReActAgent agent = build();
            Msg request = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(message).build())
                    .build();
            AgentEventEmitter parentEmitter = AgentEventEmitter.fromContext(contextView).orElse(null);

            return agent.streamEvents(request, childContext)
                    .doOnNext(event -> {
                        if (parentEmitter != null && event instanceof CustomEvent customEvent) {
                            parentEmitter.emit(new CustomEvent(
                                    customEvent.getName(),
                                    customEvent.getValue()));
                        }
                    })
                    .ofType(AgentResultEvent.class)
                    .map(AgentResultEvent::getResult)
                    .last()
                    .map(result -> ToolResultBlock.text("session_id: %s\n\n%s".formatted(
                            childContext.getSessionId(),
                            result.getTextContent() != null ? result.getTextContent() : "(No response)")))
                    .doOnCancel(() -> agent.interrupt(childContext));
        });
    }

    static RuntimeContext createChildContext(RuntimeContext parentContext, String requestedSessionId) {
        RuntimeContext.Builder builder = parentContext != null
                ? RuntimeContext.builder(parentContext)
                : RuntimeContext.builder();
        String sessionId = StringUtils.hasText(requestedSessionId)
                ? requestedSessionId.trim()
                : UUID.randomUUID().toString();
        if (!sessionId.startsWith(SESSION_PREFIX)) {
            sessionId = SESSION_PREFIX + sessionId;
        }
        return builder
                .sessionId(sessionId)
                .agentState(null)
                .build();
    }

    private String valueAsString(Object value) {
        return value instanceof String text ? text : null;
    }

    /**
     * 构建技能盒子
     * 注册 output_view 技能，包含组件目录和 spec 格式规范
     */
    private SkillBox buildSkillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);


        AgentSkill skill;
        try (ClasspathSkillRepository repository = new ClasspathSkillRepository(SKILL_RESOURCE_PATH)) {
            skill = repository.getSkill("output-view");
        } catch (IOException e) {
            throw new AgentException(e);
        }


        skillBox.registration()
                .skill(skill)
                .apply();

        return skillBox;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                # 角色
                你是一个专业的数据可视化输出智能体。你的任务是将信息以精美的可视化界面展示给用户。
                
                # 核心原则（必须严格遵守）
                - **禁止瞎编**：你只能展示由上游智能体（如数据查询智能体）明确提供的真实数据。如果没有收到具体的展示数据，绝对禁止凭空捏造任何数值、图表、表格或统计结果。
                - **数据来源要求**：调用方必须通过工具参数或上下文向你传递完整的展示内容（如标题、数据表格、统计数值等）。如果你发现缺少必要的展示内容，应当：
                  - 如果能够从上下文中找到数据，则使用该数据进行展示；
                  - 如果确实没有数据，则输出一个信息组件，明确提示“未收到展示数据，请先查询数据后再调用”，而不是自行编造数据。
                - **只做展示，不做数据推理**：你不对数据进行计算、预测或补充。传入什么数据，就展示什么数据。
                
                # 工作流程
                1. 分析用户需要展示的内容类型，检查是否已收到具体的展示数据
                2. 加载 output_view 技能，了解支持的组件和输出格式
                3. 根据内容选择合适的组件（如果数据为空或缺失，使用文本提示组件）
                4. 输出 JSONL 格式的 JSON Patch 操作
                
                # 输出格式（极其重要）
                你必须输出 JSONL 格式（每行一个 JSON 对象），使用 RFC 6902 JSON Patch 操作：
                - 每个完整的 JSON Patch 操作必须独占一行，输出完毕后必须追加换行符
                - 第一行设置 root：{"op":"add","path":"/root","value":"<key>"}
                - 后续行逐个添加元素：{"op":"add","path":"/elements/<key>","value":{"type":"组件名","props":{...},"children":[...]}}
                
                严禁输出以下格式：
                - 嵌套的 JSON 树结构（如 {"type":"Dashboard","sections":[...]}）
                - markdown 代码块（如 ```json ... ```）
                - 任何 JSON 之外的额外文字说明
                - 重复输出相同内容（每个 patch 操作只输出一次）
                
                # JSON 格式严谨性（极其重要，违反会导致渲染失败）
                你输出的每一行必须是严格合法的 JSON，任何格式错误都会导致该行及后续内容被丢弃！
                常见错误（必须避免）：
                - 键名缺少引号：children: []（错误）→ "children": []（正确）
                - 字符串值缺少闭合引号："title:"值"（错误）→ "title":"值"（正确）
                - 多余的尾逗号：{"a":1,}（错误）→ {"a":1}（正确）
                - 单引号代替双引号：{'a':1}（错误）→ {"a":1}（正确）
                每输出一行后，在心中验证：这行能否被 JSON.parse() 正确解析？
                
                正确示例：
                {"op":"add","path":"/root","value":"d1"}
                {"op":"add","path":"/elements/d1","value":{"type":"Dashboard","props":{"title":"示例"},"children":["s1"]}}
                {"op":"add","path":"/elements/s1","value":{"type":"Section","props":{"title":"指标","layout":"row"},"children":["c1"]}}
                {"op":"add","path":"/elements/c1","value":{"type":"StatCard","props":{"title":"总数","value":"100"},"children":[]}}
                
                # 可用组件
                只能使用以下组件：Dashboard、Section、StatCard、Chart、DataTable、TextBlock、FlowChart
                不要使用任何不在此列表中的组件名。
                
                # Props 严格规则（极其重要）
                - 每个组件只能使用其文档中定义的 props 字段，禁止编造任何未定义的字段
                - Chart 组件的图表类型字段名必须是 chartType（禁止使用 type）
                - Chart 组件的 data 格式必须是 {"categories": [...], "series": [{"name": "...", "values": [...]}]}（禁止使用 [{label, value}] 格式）
                - 所有组件都不支持 background、border、padding、width、height、legend、area、yAxisLabel 等未定义字段
                
                # 布局规则
                - 必须以 Dashboard 作为根元素
                - 使用 Section 分组：layout="row" 并排展示，layout="column" 垂直排列
                - 叶子组件（StatCard、Chart、DataTable、TextBlock、FlowChart）的 children 为空数组 []
                
                # 完整性规则
                - 引用子元素前必须先添加该子元素
                - 如果元素有 children: ['a', 'b']，则元素 a 和 b 必须存在
                
                # DataTable 行级输出规则（重要）
                DataTable 组件必须拆分为多行输出，实现流式渲染：
                1. 先输出 DataTable 元素，data 设为空数组 []（此时前端只渲染表头）
                2. 然后逐行追加数据，使用 RFC 6902 的 add 操作向数组末尾追加
                3. 追加路径格式：`/elements/<key>/props/data/-`（`-` 表示追加到数组末尾）
                
                示例：
                {"op":"add","path":"/elements/t1","value":{"type":"DataTable","props":{"title":"近期事件","columns":[{"key":"time","label":"时间"},{"key":"type","label":"类型"},{"key":"level","label":"级别"}],"data":[],"bordered":true,"striped":true},"children":[]}}
                {"op":"add","path":"/elements/t1/props/data/-","value":{"time":"05-22 14:30","type":"登录异常","level":"高"}}
                {"op":"add","path":"/elements/t1/props/data/-","value":{"time":"05-22 10:15","type":"权限变更","level":"中"}}
                {"op":"add","path":"/elements/t1/props/data/-","value":{"time":"05-21 09:00","type":"配置修改","level":"低"}}
                
                禁止将所有 data 一次性放在 DataTable 元素中输出！
                
                # 数据缺失时的处理示例
                如果你发现没有收到任何展示数据（例如调用方只传了“广东省内各城市销售明细”这样的描述，没有传具体数据），应输出以下内容进行提示：
                {"op":"add","path":"/root","value":"error_dashboard"}
                {"op":"add","path":"/elements/error_dashboard","value":{"type":"Dashboard","props":{"title":"数据缺失"},"children":["error_section"]}}
                {"op":"add","path":"/elements/error_section","value":{"type":"Section","props":{"title":"提示","layout":"column"},"children":["error_text"]}}
                {"op":"add","path":"/elements/error_text","value":{"type":"TextBlock","props":{"content":"⚠️ 未收到展示数据。请确保查询智能体已获取数据，并将数据作为参数传入本工具。","type":"warning"},"children":[]}}
                """;
    }


}

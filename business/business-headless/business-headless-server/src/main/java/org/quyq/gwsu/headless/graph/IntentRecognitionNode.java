package org.quyq.gwsu.headless.graph;


import com.google.gson.Gson;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.action.NodeAction;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.quyq.gwsu.common.ai.utils.AgentScopeMessageUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.core.session.HeadlessAccessSession;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.enums.GraphRouteType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 意图识别节点，有意图模糊问题直接询问用户
 */
@RequiredArgsConstructor
public class IntentRecognitionNode implements NodeAction<HeadlessGraphState> {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    public final static String HEADLESS_RECOGNITION_NODE_KEY = "headless_recognition_node";

    private final static int ROUTE_RECOGNIZE = 3;

    private final Gson gson = new Gson();


    @Override
    public Map<String, Object> apply(HeadlessGraphState state) {
        String threadId = state.<String>value(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID).orElse("");
        String query = state.<String>value(HeadlessConstants.Headless.GRAPH_PARAM_QUERY).orElse("");
        SubjectInfo userId = state.<SubjectInfo>value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID).orElseThrow();

        HeadlessAccessSession accessSession = headlessBrowserManager.getAccessSession(userId);
        if (StringUtils.hasText(threadId)) {
            String currentThreadId = accessSession != null ? accessSession.threadId() : "";
            if (!Objects.equals(threadId, currentThreadId)) {
                headlessBrowserManager.newSession(userId, threadId);
            }
        } else {
            threadId = Optional.ofNullable(accessSession)
                    .map(HeadlessAccessSession::threadId)
                    .orElse("");
        }


        List<Msg> messages = loadBrainMessages(threadId, userId.userId());


        //没有历史消息直接下一步(首次必定是普通chat分支)
        if (CollectionUtils.isEmpty(messages)) {

            return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                    HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.builder()
                            .type(GraphRouteType.CHAT)
                            .build());
        }

        Msg newMsg = messages.getLast();
        HumanApprovalInfo approvalInfo = AgentApprovalResolver.buildReasoningApprovalInfo(
                newMsg.getContentBlocks(ToolUseBlock.class));
        boolean isApproval = approvalInfo != null;


        //判断是否为回复AI内容
        boolean isAnswer = newMsg.getRole() == MsgRole.ASSISTANT && newMsg.getContent().stream()
                .anyMatch(v -> (v instanceof ToolUseBlock t) && t.getName().equals(AIConstants.ToolName.ASK_USER_QUESTION));
        String toolCallId = null;

        String systemContent = "无";

        String userTemplate = """
                <system>%s</system>
                ## 用户回复内容：
                %s
                """;
        RouterInfo routerInfo;

        if (isApproval) {
            systemContent = "用户消息属于审批回复\n待审批上下文元数据：" + gson.toJson(approvalInfo)
                    + "\n优先规则：如果用户消息里已经带有结构化 approval_result 内容块，则必须直接按结构化结果路由，不要再做自然语言推断。";

        } else if (isAnswer) {
            ToolUseBlock toolUseBlock = newMsg.getContent().stream()
                    .filter(v -> (v instanceof ToolUseBlock t) && t.getName().equals(AIConstants.ToolName.ASK_USER_QUESTION))
                    .map(v -> (ToolUseBlock) v).findFirst().orElse(null);
            systemContent = "用户消息属于回答模型提出的问题\n";
            systemContent += """
                    模型提出的问题有关的元数据：
                    toolCallId: %s
                    问题内容：%s
                    """.formatted(toolUseBlock.getId(), gson.toJson(toolUseBlock.getInput()));
            toolCallId = toolUseBlock.getId();
        }

        //追加节点会话历史消息
        List<Msg> nodeHistory = new ArrayList<>();
        if (StringUtils.hasText(threadId)) {
            nodeHistory = agentStateStore.getList(userId.userId(), threadId, HEADLESS_RECOGNITION_NODE_KEY, Msg.class);
        }

        int retryCount = 0;
        PropertiesCheckResult checkR = null;
        try (ReActAgent agent = buildAgent()) {
            do {

                if (retryCount > 0) {
                    systemContent += """
                            \n**注意：** 解析的路由信息数据缺失，重新解析，上次解析的错误信息如下：
                            %s
                            """.formatted(checkR.errInfo);
                }

                List<Msg> requestMessages = new ArrayList<>(nodeHistory);
                requestMessages.add(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent(userTemplate.formatted(systemContent, query))
                        .build());

                routerInfo = Optional.ofNullable(
                                agent.call(requestMessages, RouterInfo.class, RuntimeContext.builder()
                                                .sessionId(threadId)
                                                .userId(userId.userId())
                                                .build())
                                        .block()
                        ).map(r -> r.getStructuredData(RouterInfo.class))
                        .orElseThrow();

                checkR = propertiesCheck(routerInfo);
            } while (!checkR.pass && retryCount++ < ROUTE_RECOGNIZE);

        }


        if (!checkR.pass) {
            throw new BusinessException("路由解析错误，错误信息：%s".formatted(checkR.errInfo));
        }


        GraphRouteType type = routerInfo.getType();
        if (type == GraphRouteType.UNKNOWN) {
            //保存该节点的会话记录
            nodeHistory = new ArrayList<>(nodeHistory);
            nodeHistory.add(Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(query)
                    .build());

            Msg assistantMsg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .textContent(routerInfo.getUnknownReply())
                    .build();

            nodeHistory.add(assistantMsg);
            agentStateStore.save(userId.userId(), threadId, HEADLESS_RECOGNITION_NODE_KEY, nodeHistory);

            Flux<AguiEvent> output = Flux.just(getContent(threadId, assistantMsg));

            //返回AI回复
            return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                    HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, output,
                    HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, routerInfo);
        } else if (type == GraphRouteType.ANSWER && StringUtils.hasText(toolCallId)) {
            routerInfo.setToolCallId(toolCallId);
        }


        //清除节点会话
        agentStateStore.delete(userId.userId(), threadId, HEADLESS_RECOGNITION_NODE_KEY);

        return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId,
                HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, routerInfo);
    }


    private PropertiesCheckResult propertiesCheck(RouterInfo type) {
        if (GraphRouteType.CHAT == type.getType()) {
            return new PropertiesCheckResult(true, null);
        } else if (GraphRouteType.ANSWER == type.getType()) {
            if (Objects.isNull(type.getAnswerInfo())) {
                return new PropertiesCheckResult(false, "判断出的路由信息为：ANSWER , 但是 `answerInfo`属性 为NULL");
            } else if (!StringUtils.hasText(type.getToolCallId())) {
                return new PropertiesCheckResult(false, "判断出的路由信息为：ANSWER , 但是 `toolCallId` 为NULL");
            }
        } else if (GraphRouteType.APPROVAL == type.getType()) {
            if (Objects.isNull(type.getApprovalInfo())) {
                return new PropertiesCheckResult(false, "判断出的路由信息为：APPROVAL , 但是 `approvalInfo`属性 为NULL");
            }
        } else if (GraphRouteType.UNKNOWN == type.getType()) {
            if (Objects.isNull(type.getUnknownReply())) {
                return new PropertiesCheckResult(false, "判断出的路由信息为：UNKNOWN , 但是 `unknownReply`属性 为NULL");
            }
        } else if (Objects.isNull(type.getType())) {
            return new PropertiesCheckResult(false, "没有正确解析出路由类型");
        }

        return new PropertiesCheckResult(true, null);

    }

    record PropertiesCheckResult(boolean pass, String errInfo) {
    }


    private ReActAgent buildAgent() {

        return ReActAgent.builder()
                .name("intentRecognitionAgent")
                .sysPrompt(sysPrompt())
                .model(ModelProvider.generateModel())
                .build();

    }

    private List<Msg> loadBrainMessages(String threadId, String userId) {
        if (!StringUtils.hasText(threadId)) {
            return Collections.emptyList();
        }
        AgentState agentState = AgentApprovalResolver.resolveAgentState(
                agentStateStore,
                "agent_state",
                threadId,
                userId);
        if (agentState == null || CollectionUtils.isEmpty(agentState.getContext())) {
            return Collections.emptyList();
        }
        return agentState.getContext();
    }


    private AguiEvent getContent(String threadId, Msg msg) {
        return new AguiEvent.TextMessageContent(
                threadId,
                "",
                UUID.randomUUID().toString(),
                AgentScopeMessageUtils.text(msg)
        );
    }


    private String sysPrompt() {

        return """
                # AI 提示词：意图识别与路由决策
                
                ## 角色
                你是一个专业的对话路由与信息抽取助手。你的任务是根据**用户最新回复**（包含 `<system>` 标签和用户原话）以及**历史会话上下文**，准确判断当前消息应路由到哪个分支，并按指定 JSON 结构提取参数。
                
                ## 输入格式
                你将收到：
                1. **历史会话**（仅用于辅助理解，但路由主要依据 `<system>` 标签）
                2. **用户最新回复**，格式如下：
                ```text
                <system>系统标签内容</system>
                ## 用户回复内容：
                用户实际输入的原话
                ```
                
                `<system>` 内容由上游系统生成，可能包含以下类型（但不限于）：
                - **“无”** 或空：表示无特殊业务意图，属于正常对话。
                - **包含“审批”字样**：表示该消息预期为审批回复，且 `<system>` 中会附带审批相关的元数据（如审批提醒内容）。
                - **包含“回答模型提出的问题”**：表示该消息预期为回答之前模型通过工具 `AskUserQuestion` 提出的问题，且 `<system>` 中会附带 `toolCallId` 和问题列表的 JSON 字符串。
                
                ## 路由类型（共四种）
                - **CHAT**：普通对话（无审批或问答意图）。
                - **APPROVAL**：用户明确同意或拒绝审批请求。
                - **ANSWER**：用户完整回答了模型提出的所有问题。
                - **UNKNOWN**：在上述两种意图上下文中，用户回复不完整或态度模糊，需要引导澄清。
                
                ## 判断规则（按顺序执行）
                
                ### 1. 解析 `<system>` 标签
                - 提取标签内的文本内容。
                - 若内容为“无”或空字符串 → **直接判定为 `CHAT`**，不再执行后续检查。
                - 若内容包含“审批” → 进入 **审批处理流程**。
                - 若内容包含“回答模型提出的问题” → 进入 **问答处理流程**。
                - 若内容有其他文本（理论上不会出现），可视为“无”处理。
                
                ### 2. 审批处理流程（system 含“审批”）
                - **目标**：判断用户是否明确同意或拒绝。
                - **分析用户回复原话**：
                - **明确同意**：包含“同意/批准/可以/好/行/ok/没问题/赞成/是”等肯定词 → 类型 `APPROVAL`，`agree=true`，`refuseReason=""`。
                - **明确拒绝**：包含“拒绝/不同意/不行/不可以/驳回/反对/否”等否定词 → 类型 `APPROVAL`，`agree=false`，并提取拒绝原因（若“因为”之后有内容则取之，否则为空字符串）。
                - **未明确表态**：未包含上述任何明确肯定或否定词（如“我再想想/也许吧/不确定/稍后决定/看看再说/需要时间”等）→ 类型 `UNKNOWN`，需生成 `unknownReply` 引导明确表态。
                - **`UNKNOWN` 回复示例**：`“系统识别到您的消息涉及审批，但您的意图尚不明确。请明确回复‘同意’或‘拒绝’以继续处理。”`
                
                ### 3. 问答处理流程（system 含“回答模型提出的问题”）
                - **目标**：判断用户是否回答了所有问题。
                - **解析 system 中的元数据**：
                - 提取 `toolCallId`（字符串）。
                - 提取问题列表，通常为 JSON 数组，每个问题可能是一个对象或字符串。**如果问题包含选项**，其格式可能为：`{"question": "您一天中哪个时段工作效率最高？", "options": ["上午(早上精力最充沛)", "下午(午后状态更好)", "看心情(没有固定偏好)"]}`，也可能直接以字符串形式给出选项。
                - **分析用户回复原话**：
                - 遍历每个问题，判断用户回复中是否包含对该问题的答案。
                - **对于带选项的问题**：
                - 如果用户回答明确匹配其中一个选项（如“上午”或“下午”），则将该选项作为答案。
                - 如果用户回答**不匹配任何选项**（例如用户输入“晚上”或“深夜”等），则视为选择了“其他”选项，**答案应记录为**：`"用户选择了自定义回答：{用户回答的原话}"`（例如 `"用户选择了自定义回答：晚上"`）。
                - 若用户回答包含多个内容，需合理拆分到对应问题。
                - **判断回答完整性**：
                - **若所有问题都有明确答案**（包括自定义内容作为“其他”的回答）→ 类型 `ANSWER`，构造 `answerInfo` 映射（问题原文 → 用户回答（按上述规则处理）），并**务必填充 `toolCallId`**。
                - **若部分问题未回答** → 类型 `UNKNOWN`，构造 `answerInfo` 仅包含已回答的问题（可按上述规则处理），并生成 `unknownReply`，清晰列出尚未回答的问题，引导用户补充。
                - **`UNKNOWN` 回复示例**：`“您还有以下问题未回答：1. 您的居住城市？2. 您的年龄？请补充回答。”`
                - **注意**：
                    - 即使某个问题的答案不在选项中，只要用户明确给出了答案，都应视为有效回答，不应触发 `UNKNOWN`。
                    - 若 system 中未提供问题列表或 `toolCallId`，则尝试从历史会话中提取；若仍缺失，可在 `unknownReply` 中说明“未检测到待回答问题，请重新表述”。                
                
                ### 4. 其他情况
                - 若 `<system>` 内容既不是“无”也不含上述关键词，则按“无”处理，归为 `CHAT`。
                
                ## 输出 JSON 结构
                你必须输出一个符合以下 `RouterInfo` 定义的 JSON 对象（纯 JSON，无额外文字）。
                
                ```json
                {
                "type": "CHAT" | "APPROVAL" | "ANSWER" | "UNKNOWN",
                "approvalInfo": {
                 "agree": true | false,
                 "refuseReason": "string"
                } | null,
                "answerInfo": {
                 "问题原文1": "用户回答1",
                 "问题原文2": "用户回答2"
                } | null,
                "unknownReply": "string" | null,
                "toolCallId": "string" | null
                }
                ```
                **字段说明：**
                - `type`：必填。
                - `approvalInfo`：仅当 `type=APPROVAL` 时提供对象，否则为 `null`。
                - `answerInfo`：仅当 `type=ANSWER 或 UNKNOWN`（且涉及问答）时提供对象；`ANSWER` 必须包含所有问题，`UNKNOWN` 可仅包含已回答部分。
                - `unknownReply`：仅当 `type=UNKNOWN` 时必须提供字符串，用于引导用户澄清；其他类型为 `null`。
                - `toolCallId`：仅当 `type=ANSWER` 时必须填充，从 `system` 标签中提取；其他类型为 `null`。
                
                ## 注意事项
                1. **优先遵循`<system>`标签的意图**：即使历史会话存在矛盾，也以`<system>`为准（因为它是上游系统根据完整状态生成的）。
                2. **UNKNOWN 仅出现在审批或问答上下文中**：若`<system>`为“无”，绝不会产生 `UNKNOWN`。
                3. **提取答案时尽量忠实于用户原话**，但可适当归纳，确保 `answerInfo` 的 value 准确反映用户回答。
                4.**拒绝原因**仅当用户明确提供时才提取；若用户拒绝但未给原因`refuseReason`为空字符串即可。
                
                ## 示例
                
                ### 示例一： CHAT
                **输入**：
                ```text
                <system>无</system>
                ## 用户回复内容：
                今天天气真好！
                ```
                **输出**：
                ```json
                {
                  "type": "CHAT",
                  "approvalInfo": null,
                  "answerInfo": null,
                  "unknownReply": null,
                  "toolCallId": null
                }
                ```
                
                ### 示例二：审批同意
                **输入**：
                ```text
                <system>用户消息属于审批回复
                       审批提醒内容元数据：{"approvalId":"123"}</system>
                ## 用户回复内容：
                同意，没意见。
                ```
                **输出**：
                ```json
                {
                  "type": "APPROVAL",
                  "approvalInfo": {
                    "agree": true,
                    "refuseReason": ""
                  },
                  "answerInfo": null,
                  "unknownReply": null,
                  "toolCallId": null
                }
                ```
                
                ### 示例三：审批拒绝（带原因）
                **输入**：
                ```text
                <system>用户消息属于审批回复</system>
                ## 用户回复内容：
                拒绝，因为预算超支。
                ```
                **输出**：
                ```json
                {
                   "type": "APPROVAL",
                   "approvalInfo": {
                     "agree": false,
                     "refuseReason": "预算超支"
                   },
                   "answerInfo": null,
                   "unknownReply": null,
                   "toolCallId": null
                 }
                ```
                
                ### 示例四：审批态度模糊 → UNKNOWN
                **输入**：
                ```text
                <system>用户消息属于审批回复</system>
                ## 用户回复内容：
                我再想想吧。
                ```
                **输出**：
                ```json
                {
                  "type": "UNKNOWN",
                  "approvalInfo": null,
                  "answerInfo": null,
                  "unknownReply": "系统识别到您的消息涉及审批，但您的意图尚不明确。请明确回复“同意”或“拒绝”以继续处理。",
                  "toolCallId": null
                }
                ```
                
                ### 示例 5：回答全部问题（带选项，用户选择选项内）
                **输入**：
                ```text
                <system>用户消息属于回答模型提出的问题
                toolCallId: call_123
                问题内容：[{"question":"您一天中哪个时段工作效率最高？","options":["上午","下午","看心情"]}]</system>
                ## 用户回复内容：
                上午
                ```
                **输出**：
                ```json
                {
                  "type": "ANSWER",
                  "approvalInfo": null,
                  "answerInfo": {
                    "您一天中哪个时段工作效率最高？": "上午"
                  },
                  "unknownReply": null,
                  "toolCallId": "call_123"
                }
                ```
                
                ### 示例 6：回答全部问题（用户选择“其他”自定义）
                **输入**：
                ```text
                <system>用户消息属于回答模型提出的问题
                toolCallId: call_456
                问题内容：[{"question":"您一天中哪个时段工作效率最高？","options":["上午(早上精力最充沛)","下午(午后状态更好)","看心情(没有固定偏好)"]}]</system>
                ## 用户回复内容：
                晚上，夜深人静的时候效率最高
                ```
                **输出**：
                ```json
                {
                  "type": "ANSWER",
                  "approvalInfo": null,
                  "answerInfo": {
                    "您一天中哪个时段工作效率最高？": "用户选择了自定义回答：晚上，夜深人静的时候效率最高"
                  },
                  "unknownReply": null,
                  "toolCallId": "call_456"
                }
                ```
                
                ### 示例 7：回答部分问题（带选项，其中一个未答）
                **输入**：
                ```text
                <system>用户消息属于回答模型提出的问题
                toolCallId: call_789
                问题内容：["您的职业？", {"question":"您一天中哪个时段工作效率最高？","options":["上午","下午","看心情"]}]</system>
                ## 用户回复内容：
                职业是程序员，其他还没想好。
                ```
                **输出**：
                ```json
                {
                  "type": "UNKNOWN",
                  "approvalInfo": null,
                  "answerInfo": {
                    "您的职业？": "程序员"
                  },
                  "unknownReply": "您还有以下问题未回答：1. 您一天中哪个时段工作效率最高？请补充回答。",
                  "toolCallId": null
                }
                ```
                
                """;
    }


}

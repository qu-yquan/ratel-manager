package org.quyq.gwsu.security.brain.service.middleware;


import dev.langchain4j.model.input.PromptTemplate;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import io.agentscope.core.agui.model.AguiMessage;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.brain.service.prompt.UploadedFilePromptBuilder;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/7/10
 * @description
 */
@RequiredArgsConstructor
public class SystemPromptMiddleware implements MiddlewareBase {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final SecurityUtils securityUtils;
    private final SessionUtils sessionUtils;
    private final ObjectMapper objectMapper;


    @Override
    @SuppressWarnings("unchecked")
    public Mono<String> onSystemPrompt(Agent agent,
                                       RuntimeContext runtimeContext,
                                       String systemPrompt) {
        Map<String, Object> forwardedProps = new HashMap<>(
                Optional.ofNullable(runtimeContext.get(AIConstants.Param.FORWARDED_PROPS_KEY, Map.class))
                        .orElse(Collections.emptyMap())
        );
        forwardedProps.compute(AIConstants.Param.FORWARDED_PROPS_CURRENT_PATH_KEY,
                (key, value) -> Optional.ofNullable(value).orElse(""));
        forwardedProps.put("headlessContent", buildHeadlessContent(runtimeContext,
                (String) forwardedProps.get(AIConstants.Param.FORWARDED_PROPS_OPERATION_MODE_KEY)));
        forwardedProps.put("fileInfos", buildUploadedFilePrompt(agent, runtimeContext));
        String renderedPrompt = systemPrompt;
        if (StringUtils.hasText(systemPrompt) && !forwardedProps.isEmpty()) {
            renderedPrompt = PromptTemplate.from(systemPrompt).apply(forwardedProps).text();
        }
        String subjectSystemPrompt = buildSubjectSystemPrompt();
        if (StringUtils.hasText(subjectSystemPrompt)) {
            renderedPrompt = StringUtils.hasText(renderedPrompt)
                    ? renderedPrompt + "\n" + subjectSystemPrompt
                    : subjectSystemPrompt;
        }
        return Mono.just(renderedPrompt);
    }

    private String buildHeadlessContent(RuntimeContext runtimeContext, String operationMode){
        AIRunnerInstanceWrapper wrapper = runtimeContext != null ? runtimeContext.get(AIRunnerInstanceWrapper.class) : null;
        String headlessContent = "当前界面操作模式（human：人类操作模式 | ai：AI操作模式）：%s".formatted(operationMode);
        if (wrapper != null && wrapper.headless()) {
            headlessContent = "**特别注意**：您当前已经处于“AI操作模式” ，可以直接调用操作界面相关工具 ，禁止调用`EnterAiMode`和`ExitAiMode`工具";
        }
        return headlessContent;
    }

    private String buildUploadedFilePrompt(Agent agent, RuntimeContext runtimeContext) {
        AIRunnerInstanceWrapper wrapper = runtimeContext != null ? runtimeContext.get(AIRunnerInstanceWrapper.class) : null;
        List<AguiMessage> currentMessages = wrapper != null && wrapper.input() != null
                ? wrapper.input().getMessages()
                : List.of();
        AgentState agentState = RuntimeContext.resolveAgentState(runtimeContext, agent);
        List<Msg> historyMessages = agentState != null
                ? agentState.contextMutable()
                : List.of();
        return UploadedFilePromptBuilder.build(currentMessages, historyMessages);
    }

    /**
     * 、
     * 生成当前登录主体信息提示词
     *
     * @return
     */
    private String buildSubjectSystemPrompt() {
        Optional<Subject<Visitor>> subjectOpt = securityUtils.getSubject();
        if (subjectOpt.isEmpty()) {
            return null;
        }

        VisitorType visitorType = sessionUtils.getVisitorType();
        Subject<Visitor> subject = subjectOpt.get();
        String admin = subject.isAdmin() ? "是" : "否";
        String userType = VisitorType.USER == visitorType ? "平台用户" : "第三方客户端";
        String userInfo = "无";
        String clientInfo = "无";
        Optional<UserInfo> userInfoOpt = subject.userInfo();
        if (userInfoOpt.isPresent()) {
            userInfo = objectMapper.writeValueAsString(userInfoOpt.get());
        }
        Optional<ClientInfo> clientInfoOpt = subject.clientInfo();
        if (clientInfoOpt.isPresent()) {
            clientInfo = objectMapper.writeValueAsString(clientInfoOpt.get());
        }

        return """
                #当前系统时间
                %s
                # 当前登录主体信息：
                ## 主体类型：%s ,
                ## 是否超级管理员：%s ,
                ## 登录用户信息
                %s
                ## 所属三方平台信息
                %s
                """.formatted(dateTimeFormatter.format(LocalDateTime.now()), userType, admin, userInfo, clientInfo);
    }

}

package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 请求进入 AI 操作模式。
 */
@Component
public class EnterAiModeTool extends ToolBase {

    private static final String TOOL_NAME = "EnterAiMode";
    private static final String APPROVAL_TIP = "智能助手请求控制界面，是否同意？";

    private final WebToolUtils webToolUtils;
    private final ViewOperationCallerGuard viewOperationCallerGuard;

    public EnterAiModeTool(WebToolUtils webToolUtils, ViewOperationCallerGuard viewOperationCallerGuard) {
        super(ToolBase.builder()
                .name(TOOL_NAME)
                .description("""
                        请求进入`AI操作模式`，获取界面控制权。
                        调用此工具后，界面将锁定为AI操作模式，用户无法手动操作界面。
                        使用场景：当你需要对界面进行操作（点击、输入、选择、滚动、路由跳转）时，必须先调用此工具获取控制权。
                        注意：获取页面状态(GetPageState)不需要进入AI操作模式。
                        操作完成后必须调用ExitAiMode退出AI操作模式，将控制权交还给用户。""")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                )));
        this.webToolUtils = webToolUtils;
        this.viewOperationCallerGuard = viewOperationCallerGuard;
    }

    @Override
    public Mono<PermissionDecision> checkPermissions(Map<String, Object> input, PermissionContextState contextState) {
        return Mono.just(PermissionDecision.ask(APPROVAL_TIP));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        viewOperationCallerGuard.check(param.getAgent());
        RuntimeContext runtimeContext = param.getRuntimeContext();
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.fromCallable(() -> webToolUtils.webExecuteTool(wrapper, TOOL_NAME, Map.of()));
    }
}

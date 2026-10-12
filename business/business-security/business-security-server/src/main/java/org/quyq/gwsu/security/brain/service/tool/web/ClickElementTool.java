package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 点击页面元素工具。
 */
@Slf4j
@Component
public class ClickElementTool extends ToolBase {

    public static final String TOOL_NAME = "ClickElement";
    private static final String DEFAULT_APPROVAL_TIP = "该操作需要人工审批确认，是否继续？";
    private static final String PAGE_STATE_REQUIRED_TIP = "点击元素前必须先调用 GetPageState 获取最新页面状态";

    private final WebToolUtils webToolUtils;
    private final WebPageApprovalStateService webPageApprovalStateService;
    private final ViewOperationCallerGuard viewOperationCallerGuard;

    public ClickElementTool(WebToolUtils webToolUtils,
                            WebPageApprovalStateService webPageApprovalStateService,
                            ViewOperationCallerGuard viewOperationCallerGuard) {
        super(ToolBase.builder()
                .name(TOOL_NAME)
                .description("""
                        通过元素索引点击界面上的元素。执行完整的W3C指针事件序列。
                        需要先调用GetPageState获取元素索引。
                        工具使用前提：界面操作模式必须是`AI操作模式`。
                        
                        参数：
                        - index：元素索引编号
                        - operationDescription：本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户""")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "index", Map.of(
                                        "type", "integer",
                                        "description", "要点击的元素索引编号，从GetPageState结果中获取"
                                ),
                                "operationDescription", Map.of(
                                        "type", "string",
                                        "description", "本次点击的简要描述：例如：查看用户列表，保存用户信息，删除用户"
                                )
                        ),
                        "required", List.of("index", "operationDescription")
                )));
        this.webToolUtils = webToolUtils;
        this.webPageApprovalStateService = webPageApprovalStateService;
        this.viewOperationCallerGuard = viewOperationCallerGuard;
    }

    @Override
    public Mono<PermissionDecision> checkPermissions(Map<String, Object> input, PermissionContextState contextState) {
        return Mono.deferContextual(contextView -> {
            RuntimeContext runtimeContext = contextView.getOrDefault(AgentBase.RUNTIME_CONTEXT_KEY, null);
            if (runtimeContext == null) {
                log.info(TOOL_NAME + " context is null");
                return Mono.just(PermissionDecision.deny(PAGE_STATE_REQUIRED_TIP));
            }
            Set<Integer> approvalIndexes = webPageApprovalStateService.load(runtimeContext);
            if (approvalIndexes == null) {
                log.info(TOOL_NAME + " approvalIndexes is null");
                return Mono.just(PermissionDecision.deny(PAGE_STATE_REQUIRED_TIP));
            }

            Integer index = resolveIndex(input);
            if (index != null && approvalIndexes.contains(index)) {
                return Mono.just(PermissionDecision.builder()
                        .behavior(PermissionBehavior.ASK)
                        .message(buildApprovalTip(input))
                        .build());
            }
            return Mono.just(PermissionDecision.allow(TOOL_NAME));
        });
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        viewOperationCallerGuard.check(param.getAgent());
        AIRunnerInstanceWrapper wrapper = param.getRuntimeContext().get(AIRunnerInstanceWrapper.class);
        Object index = param.getInput().get("index");
        return Mono.fromCallable(() -> webToolUtils.webExecuteTool(wrapper, TOOL_NAME, Map.of("index", index)));
    }

    public static String buildApprovalTip(Map<String, Object> input) {
        Object operationDescription = input.get("operationDescription");
        if (operationDescription instanceof String desc && StringUtils.hasText(desc)) {
            return "敏感操作审批：" + desc;
        }
        return DEFAULT_APPROVAL_TIP;
    }

    private static Integer resolveIndex(Map<String, Object> input) {
        if (input == null) {
            return null;
        }
        Object index = input.get("index");
        if (index instanceof Number number) {
            return number.intValue();
        }
        if (index instanceof String value && StringUtils.hasText(value)) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

package org.quyq.gwsu.security.brain.controller;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentStateStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.AguiController;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import org.quyq.gwsu.common.ai.agui.model.AguiMessage;
import org.quyq.gwsu.common.ai.agui.model.CopilotKitInfo;
import org.quyq.gwsu.common.ai.agui.model.RunAgentInput;
import org.quyq.gwsu.common.ai.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.quyq.gwsu.security.api.brain.dto.BrainHistoryQueryDTO;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionSliceVo;
import org.quyq.gwsu.security.api.config.dto.ConfigSaveDTO;
import org.quyq.gwsu.security.api.config.enums.ConfigValueType;
import org.quyq.gwsu.security.brain.service.IBrainHistoryService;
import org.quyq.gwsu.security.brain.service.IBrainService;
import org.quyq.gwsu.security.brain.service.history.BrainHistorySessionIndexService;
import org.quyq.gwsu.security.dict.service.ISecurityConfigService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CopilotKit Runtime 端点控制器
 *
 * @author Quyq
 * @date 2026/4/22
 */
@LoginAllowAccess
@RestController
@RequestMapping("brain")
@Tag(name = "智能助手模块")
@Slf4j
public class BrainController implements DisposableBean {

    private static final String DEFAULT_AGENT_ID_HEADER = "X-Agent-Id";
    private static final String MODEL_LLM_CONFIG_KEY = "model_llm_config";

    private final AguiController aguiController;
    private final IBrainService brainService;
    private final IBrainHistoryService brainHistoryService;
    private final SecurityUtils securityUtils;
    private final ISecurityConfigService configService;


    public BrainController(AguiRequestProcessor processor, IBrainService brainService, AgentStateStore agentStateStore, SecurityUtils securityUtils,
                           SessionUtils sessionUtils,
                           IBrainHistoryService brainHistoryService, WebToolUtils webToolUtils,
                           ISecurityConfigService configService,
                           BrainHistorySessionIndexService brainHistorySessionIndexService) {
        this.brainService = brainService;
        this.brainHistoryService = brainHistoryService;
        this.securityUtils = securityUtils;
        this.configService = configService;
        this.aguiController = new AguiController(processor, webToolUtils, securityUtils, sessionUtils, 600000L) {
            @Override
            protected CopilotKitInfo handleInfo() {
                return new CopilotKitInfo()
                        .addAgent(new CopilotKitInfo.Agents(IBrainService.AGENT_ID, "平台中央大脑"));
            }

            @Override
            protected void afterRunCompleted(RunAgentInput input, String userId,
                                             RuntimeContext runtimeContext) {
                brainHistorySessionIndexService.refreshSessionIndex(input.threadId(), userId);
            }
        };

        this.aguiController.setAgentStateStore(agentStateStore);

    }


    /**
     * 中央大脑统一入口
     */
    @Operation(summary = "中央大脑（智能助手）聊天入口")
    @PostMapping(value = "run/copilotKit",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object handleCopilotKitRequest(@RequestBody ChatDTO request,
                                          @RequestHeader(value = DEFAULT_AGENT_ID_HEADER, required = false) String headerAgentId) {

        return aguiController.handleCopilotKitRequest(request, headerAgentId);
    }

    @Operation(summary = "保存 LLM 模型配置")
    @PostMapping("model/llm-config")
    public R<Boolean> saveLlmModelConfig(@RequestBody ConfigSaveDTO dto) {
        ConfigVO existing = configService.getByKey(MODEL_LLM_CONFIG_KEY);
        if (existing != null) {
            dto.setId(existing.getId());
        }
        dto.setConfigKey(MODEL_LLM_CONFIG_KEY);
        dto.setValueType(ConfigValueType.JSON);
        if (dto.getConfigName() == null || dto.getConfigName().isBlank()) {
            dto.setConfigName("LLM 模型配置");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            dto.setDescription("LLM 模型提供商、连接参数及生成参数配置");
        }

        Boolean saved = configService.saveOrUpdateConfig(dto);
        if (Boolean.TRUE.equals(saved)) {
            brainService.refreshSingletonAgent();
        }
        return R.ok(saved);
    }

    @Operation(summary = "分页查询历史会话列表")
    @PostMapping("history/sessions")
    public R<BrainHistorySessionSliceVo> pageHistorySessions(@RequestBody BrainHistoryQueryDTO query) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.pageHistorySessions(query, userId));
    }

    @Operation(summary = "查询会话消息列表")
    @GetMapping("history/sessions/{sessionId}/messages")
    public R<List<AguiMessage>> getSessionMessages(@PathVariable String sessionId) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.getSessionMessages(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("history/sessions/{sessionId}")
    public R<Boolean> deleteSession(@PathVariable String sessionId) {
        String userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        return R.ok(brainHistoryService.deleteSession(sessionId, userId));
    }

    @Operation(summary = "前端工具执行结果回调")
    @PostMapping("tool/callback")
    public R<Void> toolCallback(@RequestBody WebToolCallbackRequest request) {
        return aguiController.handleToolCallback(request);
    }

    @Operation(summary = "查询会话审批状态")
    @GetMapping("approval/status/{threadId}")
    public R<HumanApprovalInfo> getApprovalStatus(@PathVariable String threadId) {
        return aguiController.handleApprovalStatus(threadId);
    }

    @Override
    public void destroy() throws Exception {
        aguiController.destroy();
    }
}

package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRuntimeContextDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillToolInvokeDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillRegistryProperties;
import org.quyq.gwsu.common.ai.skill.dynamic.support.DynamicSkillRestClientFactory;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public final class RemoteSkillToolInvoker {

    private static final ParameterizedTypeReference<R<ToolResultBlock>> RESULT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient.Builder restClientBuilder;
    private final SkillRegistryProperties properties;

    public RemoteSkillToolInvoker(
            RestClient.Builder restClientBuilder,
            SkillRegistryProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    public Mono<ToolResultBlock> invoke(
            String applicationName,
            String skillId,
            RegisteredToolManifest tool,
            ToolCallParam param) {
        return Mono.fromCallable(() -> {
            String toolCallId = param.getToolUseBlock() == null
                    ? null : param.getToolUseBlock().getId();
            try {
                R<ToolResultBlock> response = DynamicSkillRestClientFactory.create(
                                restClientBuilder, applicationName, properties)
                        .post()
                        .uri("/internal/ai/tools/invoke")
                        .body(new SkillToolInvokeDTO(
                                skillId, tool.exposedName(), toolCallId, param.getInput(),
                                SkillRuntimeContextDTO.from(param.getRuntimeContext())))
                        .retrieve()
                        .body(RESULT_TYPE);
                if (response == null || !response.isSuccess() || response.data() == null) {
                    throw new IllegalStateException("远程动态技能工具调用失败：" + tool.exposedName());
                }
                return response.data();
            }catch (Exception e){
                log.error(e.getMessage(), e);
                throw e;
            }

        }).subscribeOn(Schedulers.boundedElastic());
    }
}

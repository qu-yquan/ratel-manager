package org.quyq.gwsu.common.ai.skill.dynamic.web;

import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceContext;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceContentVO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceReadDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillToolInvokeDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.LocalSkillResourceRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillResourcePathValidator;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.LocalSkillToolRegistry;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 动态 Skill 内部函数式端点处理器。
 *
 * <p>不使用 {@code @RestController}，避免进入业务接口权限收集和操作日志切面。</p>
 */
@RequiredArgsConstructor
public final class DynamicSkillInternalHandler {

    static final ParameterizedTypeReference<R<ToolResultBlock>> TOOL_RESULT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final LocalSkillToolRegistry localToolRegistry;
    private final LocalSkillResourceRegistry localResourceRegistry;
    private final SkillCatalogHolder catalogHolder;
    private final ObjectMapper objectMapper;

    public ServerResponse invoke(ServerRequest request) throws Exception {
        SkillToolInvokeDTO input = request.body(SkillToolInvokeDTO.class);
        if (StringUtils.hasText(input.skillId())) {
            SkillRegistrationManifest manifest = catalogHolder.current()
                    .requiredSkill(input.skillId())
                    .manifest();
            boolean bound = manifest.tools().stream()
                    .anyMatch(tool -> tool.exposedName().equals(input.toolName()));
            if (!bound) {
                throw new IllegalArgumentException("工具未绑定到指定 Skill");
            }
        } else {
            catalogHolder.current().requiredStandaloneTool(input.toolName());
        }
        AgentTool tool = localToolRegistry.requiredTool(input.toolName());
        ToolUseBlock useBlock = ToolUseBlock.builder()
                .id(input.toolCallId())
                .name(input.toolName())
                .input(input.arguments())
                .build();
        ToolResultBlock result = tool.callAsync(ToolCallParam.builder()
                        .toolUseBlock(useBlock)
                        .input(input.arguments())
                        .runtimeContext(input.runtimeContext().toRuntimeContext())
                        .build())
                .block();
        return json(R.ok(result), TOOL_RESULT_RESPONSE_TYPE);
    }

    public ServerResponse readResource(ServerRequest request) throws Exception {
        SkillResourceReadDTO input = request.body(SkillResourceReadDTO.class);
        SkillResourcePathValidator.validate(input.path());
        SkillRegistrationManifest registration = catalogHolder.current()
                .requiredSkill(input.skillId())
                .manifest();
        SkillResourceManifest manifest = registration.resources().stream()
                .filter(value -> value.path().equals(input.path()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("资源未在 Manifest 中注册"));
        SkillResourceProvider provider = localResourceRegistry.requiredProvider(input.skillId());
        SkillResourceContext resourceContext = new SkillResourceContext(
                input.runtimeContext().toRuntimeContext());
        String content;
        String encoding;
        if (manifest.binary()) {
            byte[] value = provider.readBinary(input.path(), resourceContext)
                    .orElseThrow(() -> new IllegalArgumentException("动态技能资源不存在"));
            content = Base64.getEncoder().encodeToString(value);
            encoding = "BASE64";
        } else {
            content = provider.readText(input.path(), resourceContext)
                    .orElseThrow(() -> new IllegalArgumentException("动态技能资源不存在"));
            encoding = StandardCharsets.UTF_8.name();
        }
        return json(R.ok(new SkillResourceContentVO(
                manifest.path(), manifest.mediaType(), manifest.binary(),
                encoding, content, manifest.digest())));
    }

    private ServerResponse json(Object body) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private <T> ServerResponse json(T body, ParameterizedTypeReference<T> bodyType) {
        JavaType javaType = objectMapper.getTypeFactory()
                .constructType(bodyType.getType());

        String value = objectMapper.writerFor(javaType)
                .writeValueAsString(body);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(value);
    }
}

package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.config.properties.ModelLlmConfigDTO;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** LLM 配置读取及热更新快照序列化所需的原生镜像反射声明。 */
public final class ModelRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> type : new Class<?>[]{
                ModelLlmConfigDTO.class,
                ModelLlmConfigDTO.OpenaiConfigDTO.class,
                ModelLlmConfigDTO.DashscopeConfigDTO.class,
                ModelLlmConfigDTO.GeminiConfigDTO.class,
                ModelLlmConfigDTO.AnthropicConfigDTO.class,
                ModelLlmConfigDTO.GenerateOptionsDTO.class,
                ModelLlmConfigDTO.MultimodalOptionsDTO.class}) {
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}

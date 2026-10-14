package org.quyq.gwsu.common.ai.model;

import io.agentscope.core.model.Model;
import org.quyq.gwsu.common.ai.config.properties.ModelLlmConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

/**
 * @author Quyq
 * @date 2026/5/31
 * @description 模型提供
 */
public class ModelProvider {

    public static final String MODEL_LLM_CONFIG = "model_llm_config";

    private static final Model MODEL = new ReloadableModel(
            ModelProvider::currentConfig, ModelProviderType::createModelFromConfig);

    /**
     * 获取稳定的模型代理。真实模型在调用时解析，配置变化不需要重建智能体。
     *
     * @return 共享的热更新模型代理
     */
    public static Model generateModel() {
        return MODEL;
    }

    /**
     * 当前配置的模型是否支持多模态输入。
     *
     * @return 支持返回 true，否则返回 false
     */
    public static boolean supportMultimodal() {
        ModelLlmConfigDTO config = currentConfig();
        return Boolean.TRUE.equals(config.getSupportMultimodal());
    }

    /** 获取最新保存的 LLM 配置，真实模型在下次调用时更新。 */
    public static ModelLlmConfigDTO currentConfig() {
        return ConfigInfoUtils.getByObject(MODEL_LLM_CONFIG, ModelLlmConfigDTO.class);
    }
}

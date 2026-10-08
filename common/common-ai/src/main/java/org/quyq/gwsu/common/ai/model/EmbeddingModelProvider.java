package org.quyq.gwsu.common.ai.model;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.quyq.gwsu.common.ai.config.properties.ModelEmbeddingConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 向量化模型提供入口。
 */
public class EmbeddingModelProvider {

    public static final String MODEL_EMBEDDING_CONFIG = "model_embedding_config";

    private static volatile ModelEmbeddingConfigDTO CONFIG;

    private static volatile EmbeddingModel MODEL;

    public static Optional<EmbeddingModel> generateModel() {
        ModelEmbeddingConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_EMBEDDING_CONFIG, ModelEmbeddingConfigDTO.class);
        if (!isEnabled(newConfig) || !isConfigReady(newConfig)) {
            CONFIG = newConfig;
            MODEL = null;
            return Optional.empty();
        }
        if (configChange(newConfig)) {
            createModel(newConfig);
        }
        return Optional.ofNullable(MODEL);
    }

    private static void createModel(ModelEmbeddingConfigDTO config) {
        synchronized (EmbeddingModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = EmbeddingModelProviderType.createModelFromConfig(config);
            }
        }
    }

    private static boolean configChange(ModelEmbeddingConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);
    }

    private static boolean isEnabled(ModelEmbeddingConfigDTO config) {
        return config != null && !Boolean.FALSE.equals(config.getEnabled());
    }

    private static boolean isConfigReady(ModelEmbeddingConfigDTO config) {
        if (config == null || !StringUtils.hasText(config.getProvider())) {
            return false;
        }
        return switch (config.getProvider().trim().toLowerCase()) {
            case "dashscope" -> config.getDashscope() != null
                    && StringUtils.hasText(config.getDashscope().getApiKey())
                    && StringUtils.hasText(config.getDashscope().getModelName());
            case "openai" -> config.getOpenai() != null
                    && StringUtils.hasText(config.getOpenai().getApiKey())
                    && StringUtils.hasText(config.getOpenai().getModelName());
            case "ollama" -> config.getOllama() != null
                    && StringUtils.hasText(config.getOllama().getModelName());
            default -> false;
        };
    }
}

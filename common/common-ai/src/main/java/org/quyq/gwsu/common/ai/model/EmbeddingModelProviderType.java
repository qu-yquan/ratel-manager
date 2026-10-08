package org.quyq.gwsu.common.ai.model;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.config.properties.ModelEmbeddingConfigDTO;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * 向量化模型提供商工厂。
 */
public enum EmbeddingModelProviderType {
    DASHSCOPE("dashscope") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.DashscopeEmbeddingConfigDTO c = config.getDashscope();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("DashScope embedding API Key must be configured");
            }
            QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            Integer dimensions = positiveDimensions(c.getModelName(), c.getDimensions());
            if (dimensions != null) {
                builder.dimension(dimensions);
            }
            return builder.build();
        }
    },
    OPENAI("openai") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.OpenaiEmbeddingConfigDTO c = config.getOpenai();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("OpenAI embedding API Key must be configured");
            }
            OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            Integer dimensions = positiveDimensions(c.getModelName(), c.getDimensions());
            if (dimensions != null) {
                builder.dimensions(dimensions);
            }
            return builder.build();
        }
    },
    OLLAMA("ollama") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.OllamaEmbeddingConfigDTO c = config.getOllama();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getModelName())) {
                throw new AgentException("Ollama embedding model name must be configured");
            }
            OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            Integer dimensions = positiveDimensions(c.getModelName(), c.getDimensions());
            if (dimensions != null) {
                builder.dimensions(dimensions);
            }
            return builder.build();
        }
    };

    private final String id;

    EmbeddingModelProviderType(String id) {
        this.id = id;
    }

    protected abstract EmbeddingModel createModel(ModelEmbeddingConfigDTO config);

    public static EmbeddingModel createModelFromConfig(ModelEmbeddingConfigDTO config) {
        if (config == null || config.getProvider() == null) {
            throw new IllegalStateException("Embedding config or provider must not be null");
        }
        String provider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        for (EmbeddingModelProviderType type : values()) {
            if (type.id.equals(provider)) {
                return type.createModel(config);
            }
        }
        throw new IllegalStateException("Unsupported embedding config provider: " + provider);
    }

    private static Integer positiveDimensions(String modelName, Integer dimensions) {
        if ("bge-m3".equals(modelName)) {
            return null;
        }
        return dimensions != null && dimensions > 0 ? dimensions : null;
    }
}

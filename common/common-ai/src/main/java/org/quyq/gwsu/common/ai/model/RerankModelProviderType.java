package org.quyq.gwsu.common.ai.model;

import dev.langchain4j.community.model.dashscope.QwenScoringModel;
import dev.langchain4j.community.model.xinference.XinferenceScoringModel;
import dev.langchain4j.model.jina.JinaScoringModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.config.properties.ModelRerankConfigDTO;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * 重排模型提供商工厂。
 */
public enum RerankModelProviderType {
    DASHSCOPE("dashscope") {
        @Override
        protected ScoringModel createModel(ModelRerankConfigDTO config) {
            ModelRerankConfigDTO.DashscopeRerankConfigDTO c = config.getDashscope();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("DashScope rerank API Key must be configured");
            }
            QwenScoringModel.QwenScoringModelBuilder builder = QwenScoringModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            if (StringUtils.hasText(c.getInstruct())) {
                builder.instruct(c.getInstruct());
            }
            return builder.build();
        }
    },
    JINA("jina") {
        @Override
        protected ScoringModel createModel(ModelRerankConfigDTO config) {
            ModelRerankConfigDTO.JinaRerankConfigDTO c = config.getJina();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("Jina rerank API Key must be configured");
            }
            JinaScoringModel.JinaScoringModelBuilder builder = JinaScoringModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            return builder.build();
        }
    },
    XINFERENCE("xinference") {
        @Override
        protected ScoringModel createModel(ModelRerankConfigDTO config) {
            ModelRerankConfigDTO.XinferenceRerankConfigDTO c = config.getXinference();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getBaseUrl()) || !StringUtils.hasText(c.getModelName())) {
                throw new AgentException("Xinference rerank base URL and model name must be configured");
            }
            XinferenceScoringModel.XinferenceScoringModelBuilder builder = XinferenceScoringModel.builder()
                    .baseUrl(c.getBaseUrl())
                    .modelName(c.getModelName());
            if (StringUtils.hasText(c.getApiKey())) {
                builder.apiKey(c.getApiKey());
            }
            return builder.build();
        }
    };

    private final String id;

    RerankModelProviderType(String id) {
        this.id = id;
    }

    protected abstract ScoringModel createModel(ModelRerankConfigDTO config);

    public static ScoringModel createModelFromConfig(ModelRerankConfigDTO config) {
        if (config == null || config.getProvider() == null) {
            throw new IllegalStateException("Rerank config or provider must not be null");
        }
        String provider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        for (RerankModelProviderType type : values()) {
            if (type.id.equals(provider)) {
                return type.createModel(config);
            }
        }
        throw new IllegalStateException("Unsupported rerank config provider: " + provider);
    }
}

package org.quyq.gwsu.common.ai.model;

import dev.langchain4j.model.scoring.ScoringModel;
import org.quyq.gwsu.common.ai.config.properties.ModelRerankConfigDTO;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 重排模型提供入口。
 */
public class RerankModelProvider {

    public static final String MODEL_RERANK_CONFIG = "model_rerank_config";

    private static volatile ModelRerankConfigDTO CONFIG;

    private static volatile ScoringModel MODEL;

    public static Optional<ConfiguredRerankModel> generateModel() {
        ModelRerankConfigDTO newConfig = ConfigInfoUtils.getByObject(MODEL_RERANK_CONFIG, ModelRerankConfigDTO.class);
        if (!isEnabled(newConfig) || !isConfigReady(newConfig)) {
            CONFIG = newConfig;
            MODEL = null;
            return Optional.empty();
        }
        if (configChange(newConfig)) {
            createModel(newConfig);
        }
        ScoringModel model = MODEL;
        return model == null ? Optional.empty() : Optional.of(new ConfiguredRerankModel(model, resolveTopN(CONFIG)));
    }

    private static void createModel(ModelRerankConfigDTO config) {
        synchronized (RerankModelProvider.class) {
            if (configChange(config)) {
                CONFIG = config;
                MODEL = RerankModelProviderType.createModelFromConfig(config);
            }
        }
    }

    private static boolean configChange(ModelRerankConfigDTO newConfig) {
        if (Objects.isNull(CONFIG) || Objects.isNull(MODEL)) {
            return true;
        }
        return !Objects.equals(newConfig, CONFIG);
    }

    private static boolean isEnabled(ModelRerankConfigDTO config) {
        return config != null && !Boolean.FALSE.equals(config.getEnabled());
    }

    private static boolean isConfigReady(ModelRerankConfigDTO config) {
        if (config == null || !StringUtils.hasText(config.getProvider())) {
            return false;
        }
        return switch (config.getProvider().trim().toLowerCase()) {
            case "dashscope" -> ready(config.getDashscope(), true, false);
            case "jina" -> ready(config.getJina(), true, false);
            case "xinference" -> ready(config.getXinference(), false, true);
            default -> false;
        };
    }

    private static boolean ready(ModelRerankConfigDTO.BaseRemoteRerankConfigDTO config,
                                 boolean apiKeyRequired,
                                 boolean baseUrlRequired) {
        return config != null
                && StringUtils.hasText(config.getModelName())
                && (!apiKeyRequired || StringUtils.hasText(config.getApiKey()))
                && (!baseUrlRequired || StringUtils.hasText(config.getBaseUrl()));
    }

    private static int resolveTopN(ModelRerankConfigDTO config) {
        if (config == null) {
            return 10;
        }
        Integer topN = config.getTopN();
        if ((topN == null || topN <= 0) && config.getDashscope() != null) {
            topN = config.getDashscope().getTopN();
        }
        return topN == null || topN <= 0 ? 10 : Math.min(topN, 100);
    }

    public record ConfiguredRerankModel(ScoringModel model, int topN) {
    }
}

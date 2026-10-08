package org.quyq.gwsu.common.ai.config.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 向量化模型配置 DTO，与前端 model_embedding_config JSON 结构一一映射。
 */
@Data
public class ModelEmbeddingConfigDTO {

    /**
     * 是否启用向量化模型。
     */
    private Boolean enabled = true;

    /**
     * 当前激活的向量化模型提供商。
     *
     * <p>支持值：dashscope、openai、ollama
     */
    private String provider;

    private DashscopeEmbeddingConfigDTO dashscope = new DashscopeEmbeddingConfigDTO();

    private OpenaiEmbeddingConfigDTO openai = new OpenaiEmbeddingConfigDTO();

    private OllamaEmbeddingConfigDTO ollama = new OllamaEmbeddingConfigDTO();

    @Data
    public static class BaseRemoteEmbeddingConfigDTO {

        private String apiKey;

        private String modelName;

        private String baseUrl;

        private Integer dimensions;

        private Integer batchSize = 16;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DashscopeEmbeddingConfigDTO extends BaseRemoteEmbeddingConfigDTO {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OpenaiEmbeddingConfigDTO extends BaseRemoteEmbeddingConfigDTO {
    }

    @Data
    public static class OllamaEmbeddingConfigDTO {

        private String modelName;

        private String baseUrl;

        private Integer dimensions;

        private Integer batchSize = 16;
    }
}

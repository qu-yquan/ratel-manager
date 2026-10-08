package org.quyq.gwsu.common.ai.config.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 重排模型配置 DTO，与前端 model_rerank_config JSON 结构一一映射。
 */
@Data
public class ModelRerankConfigDTO {

    /**
     * 是否启用重排模型。
     */
    private Boolean enabled = true;

    /**
     * 当前激活的重排模型提供商。
     *
     * <p>支持值：dashscope、jina、xinference
     */
    private String provider;

    /**
     * 重排完成后由业务代码截取的最大结果数，不下发给模型服务。
     */
    private Integer topN;

    private DashscopeRerankConfigDTO dashscope = new DashscopeRerankConfigDTO();

    private JinaRerankConfigDTO jina = new JinaRerankConfigDTO();

    private XinferenceRerankConfigDTO xinference = new XinferenceRerankConfigDTO();

    @Data
    public static class BaseRemoteRerankConfigDTO {

        private String apiKey;

        private String modelName;

        private String baseUrl;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DashscopeRerankConfigDTO extends BaseRemoteRerankConfigDTO {

        private String instruct;

        /**
         * 兼容旧配置。新代码只使用根节点 topN，并在收到全量分数后本地截取。
         */
        @Deprecated
        private Integer topN = 10;

        /**
         * 兼容旧配置，不再下发给 DashScope。
         */
        @Deprecated
        private Boolean returnDocuments = true;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class JinaRerankConfigDTO extends BaseRemoteRerankConfigDTO {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class XinferenceRerankConfigDTO extends BaseRemoteRerankConfigDTO {
    }
}

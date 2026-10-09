package org.quyq.gwsu.kit.knowledge.engine.chunk;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.EmbeddingModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 知识 Chunk 向量化服务。
 */
@Slf4j
@Component
public class KnowledgeChunkEmbeddingService {

    private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.1D;

    private static final EncodingRegistry TOKEN_ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();

    private static final Encoding TOKEN_ENCODING = TOKEN_ENCODING_REGISTRY.getEncoding(EncodingType.CL100K_BASE);

    private final KnowledgeProperties properties;

    private final Supplier<Optional<EmbeddingModel>> embeddingModelSupplier = EmbeddingModelProvider::generateModel;

    public KnowledgeChunkEmbeddingService(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public boolean embedChunks(List<KnowledgeChunkDocument> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        try {
            Optional<EmbeddingModel> modelOptional = embeddingModelSupplier.get();
            if (modelOptional.isEmpty()) {
                log.warn("知识库未启用或未配置向量化模型，跳过文档向量化步骤");
                return false;
            }
            EmbeddingModel model = modelOptional.get();
            List<TextSegment> segments = chunks.stream()
                    .map(chunk -> TextSegment.from(StringUtils.hasText(chunk.getContent()) ? chunk.getContent() : ""))
                    .toList();
            List<float[]> embeddings = embedBatches(model, segments);
            if (embeddings.size() != chunks.size()) {
                throw new BusinessException(KitErrorCode.E03011);
            }
            String embeddingModel = model.getClass().getSimpleName();
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i)
                        .setEmbedding(embeddings.get(i))
                        .setEmbeddingModel(embeddingModel);
            }
            return true;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    public Optional<float[]> embedQuery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Optional.empty();
        }
        try {
            Optional<EmbeddingModel> modelOptional = embeddingModelSupplier.get();
            if (modelOptional.isEmpty()) {
                log.warn("知识库未启用或未配置向量化模型，查询将仅使用倒排索引检索");
                return Optional.empty();
            }
            return Optional.of(modelOptional.get().embed(keyword).content().vector());
        } catch (RuntimeException ex) {
            log.warn("知识库查询向量化失败，将仅使用全文检索", ex);
            return Optional.empty();
        }
    }

    private List<float[]> embedBatches(EmbeddingModel model, List<TextSegment> segments) {
        List<float[]> embeddings = new ArrayList<>(segments.size());
        for (List<TextSegment> batch : planBatches(segments)) {
            List<Embedding> batchEmbeddings = model.embedAll(batch).content();
            if (batchEmbeddings.size() != batch.size()) {
                throw new BusinessException(KitErrorCode.E03011);
            }
            batchEmbeddings.stream().map(Embedding::vector).forEach(embeddings::add);
        }
        return embeddings;
    }

    List<List<TextSegment>> planBatches(List<TextSegment> segments) {
        int configuredLimit = Math.max(1, properties.getEmbeddingBatchTokenCount());
        int tokenLimit = Math.max(1, (int) Math.floor(configuredLimit * (1D - TOKEN_COUNT_RESERVE_PERCENTAGE)));
        List<List<TextSegment>> batches = new ArrayList<>();
        List<TextSegment> currentBatch = new ArrayList<>();
        int currentTokens = 0;
        for (TextSegment segment : segments) {
            int segmentTokens = Math.max(1, TOKEN_ENCODING.countTokens(segment.text()));
            if (!currentBatch.isEmpty() && currentTokens + segmentTokens > tokenLimit) {
                batches.add(List.copyOf(currentBatch));
                currentBatch.clear();
                currentTokens = 0;
            }
            currentBatch.add(segment);
            currentTokens += segmentTokens;
        }
        if (!currentBatch.isEmpty()) {
            batches.add(List.copyOf(currentBatch));
        }
        return batches;
    }
}

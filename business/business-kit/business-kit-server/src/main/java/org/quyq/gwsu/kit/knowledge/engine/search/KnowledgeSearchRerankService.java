package org.quyq.gwsu.kit.knowledge.engine.search;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.RerankModelProvider;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 知识库检索结果重排服务。
 */
@Slf4j
@Component
public class KnowledgeSearchRerankService {

    private final Supplier<Optional<RerankModelProvider.ConfiguredRerankModel>> modelSupplier;

    public KnowledgeSearchRerankService() {
        this(RerankModelProvider::generateModel);
    }

    KnowledgeSearchRerankService(
            Supplier<Optional<RerankModelProvider.ConfiguredRerankModel>> modelSupplier) {
        this.modelSupplier = modelSupplier;
    }

    public List<KnowledgeSearchResultVO> rerank(String keyword, List<KnowledgeSearchResultVO> results, int size) {
        if (!StringUtils.hasText(keyword) || CollectionUtils.isEmpty(results) || results.size() <= 1) {
            return results;
        }
        try {
            List<TextSegment> segments = results.stream()
                    .map(result -> TextSegment.from(StringUtils.hasText(result.getContent()) ? result.getContent() : ""))
                    .toList();
            Optional<RerankModelProvider.ConfiguredRerankModel> configuredModelOptional = modelSupplier.get();
            if (configuredModelOptional.isEmpty()) {
                log.warn("知识库未启用或未配置重排模型，检索结果将跳过重排");
                return results.stream().limit(size).toList();
            }
            RerankModelProvider.ConfiguredRerankModel configuredModel = configuredModelOptional.get();
            Response<List<Double>> response = configuredModel.model().scoreAll(segments, keyword);
            List<Double> scores = response.content();
            if (scores == null || scores.size() != results.size()) {
                log.warn("重排模型返回分数数量异常，expected={}, actual={}",
                        results.size(), scores == null ? 0 : scores.size());
                return fallback(results, size);
            }
            int effectiveTopN = Math.min(Math.max(1, size), configuredModel.topN());
            List<IndexedSearchResult> indexedResults = new ArrayList<>(results.size());
            for (int index = 0; index < results.size(); index++) {
                Double score = scores.get(index);
                if (score == null || !Double.isFinite(score)) {
                    log.warn("重排模型返回无效分数，index={}, score={}", index, score);
                    return fallback(results, size);
                }
                indexedResults.add(new IndexedSearchResult(index, results.get(index), score));
            }
            List<KnowledgeSearchResultVO> reranked = indexedResults.stream()
                    .sorted(Comparator.comparing(IndexedSearchResult::score).reversed()
                            .thenComparingInt(IndexedSearchResult::index))
                    .limit(effectiveTopN)
                    .map(item -> item.result().setScore(item.score()))
                    .toList();
            return reranked.isEmpty() ? fallback(results, size) : reranked;
        } catch (RuntimeException ex) {
            log.warn("知识库检索结果重排失败，将使用 ES 原始排序", ex);
            return fallback(results, size);
        }
    }

    private List<KnowledgeSearchResultVO> fallback(List<KnowledgeSearchResultVO> results, int size) {
        return results.stream().limit(Math.max(1, size)).toList();
    }

    private record IndexedSearchResult(int index, KnowledgeSearchResultVO result, double score) {
    }
}

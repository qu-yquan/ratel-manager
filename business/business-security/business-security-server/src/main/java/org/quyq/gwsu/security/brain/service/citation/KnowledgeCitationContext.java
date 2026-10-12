package org.quyq.gwsu.security.brain.service.citation;

import io.agentscope.core.agent.RuntimeContext;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.security.brain.vo.BrainHistoryMessageMetadataVO;
import org.quyq.gwsu.security.brain.vo.KnowledgeReferenceLocationVO;
import org.quyq.gwsu.security.brain.vo.KnowledgeReferenceVO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单次 Agent 运行的知识引用收集器。
 *
 * <p>父子 RuntimeContext 会共享该对象实例，因此子智能体工具产生的引用能够由父智能体解析。</p>
 */
public final class KnowledgeCitationContext {

    private final Map<String, String> citationKeyByHit = new LinkedHashMap<>();
    private final Map<String, KnowledgeSearchResultVO> resultByCitationKey = new LinkedHashMap<>();
    private final Map<String, BrainHistoryMessageMetadataVO> metadataByMessageId = new LinkedHashMap<>();
    private final Set<String> delegatedCitationKeys = new LinkedHashSet<>();
    private int sequence;

    public static KnowledgeCitationContext getOrCreate(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return new KnowledgeCitationContext();
        }
        synchronized (runtimeContext) {
            KnowledgeCitationContext existing = runtimeContext.get(KnowledgeCitationContext.class);
            if (existing != null) {
                return existing;
            }
            KnowledgeCitationContext created = new KnowledgeCitationContext();
            runtimeContext.put(KnowledgeCitationContext.class, created);
            return created;
        }
    }

    public synchronized String register(KnowledgeSearchResultVO result) {
        String hitIdentity = hitIdentity(result);
        String existingKey = citationKeyByHit.get(hitIdentity);
        if (existingKey != null) {
            return existingKey;
        }
        String citationKey = "K" + ++sequence;
        citationKeyByHit.put(hitIdentity, citationKey);
        resultByCitationKey.put(citationKey, result);
        return citationKey;
    }

    public synchronized KnowledgeSearchResultVO find(String citationKey) {
        return resultByCitationKey.get(citationKey);
    }

    /**
     * 根据输出文本里实际出现的引用 key 生成其可用文档数据。
     * 未在本轮检索上下文注册、或没有源文档的 key 会被忽略。
     */
    public synchronized List<KnowledgeReferenceVO> referencesForCitationKeys(List<String> citationKeys) {
        Map<String, KnowledgeReferenceVO> referencesByDocument = new LinkedHashMap<>();
        Map<String, Set<String>> locationKeysByDocument = new LinkedHashMap<>();

        if (citationKeys == null) {
            return List.of();
        }
        for (String citationKey : citationKeys) {
            KnowledgeSearchResultVO result = resultByCitationKey.get(citationKey);
            if (result == null || !StringUtils.hasText(result.getSourceDocumentId())) {
                continue;
            }

            String documentId = result.getSourceDocumentId();
            KnowledgeReferenceVO reference = referencesByDocument.computeIfAbsent(documentId,
                    ignored -> new KnowledgeReferenceVO()
                            .setDocumentId(documentId)
                            .setFileId(result.getSourceFileId())
                            .setFileName(resolveFileName(result))
                            .setFileFormat(result.getSourceFileFormat())
                            .setLocations(new ArrayList<>()));
            if (result.getScore() != null
                    && (reference.getBestScore() == null || result.getScore() > reference.getBestScore())) {
                reference.setBestScore(result.getScore());
            }

            String locationIdentity = StringUtils.hasText(result.getPageBlockId())
                    ? "block:" + result.getPageBlockId()
                    : "chunk:" + result.getChunkId();
            Set<String> locationKeys = locationKeysByDocument.computeIfAbsent(documentId,
                    ignored -> new LinkedHashSet<>());
            if (locationKeys.add(locationIdentity)) {
                reference.getLocations().add(new KnowledgeReferenceLocationVO()
                        .setCitationKey(citationKey)
                        .setChunkId(result.getChunkId())
                        .setPageId(result.getPageId())
                        .setPageVersionId(result.getPageVersionId())
                        .setPageBlockId(result.getPageBlockId())
                        .setHeadingPath(result.getHeadingPath())
                        .setBlockOrder(result.getBlockOrder()));
            }
        }

        referencesByDocument.values().forEach(reference -> reference.setChunkCount(reference.getLocations().size()));
        return List.copyOf(referencesByDocument.values());
    }

    /**
     * 记录子智能体最终结果实际使用过的引用编号。
     *
     * <p>只接受当前运行中已由知识检索工具注册的编号，避免子智能体文本中的伪造编号进入引用元数据。</p>
     */
    public synchronized void recordDelegatedCitationKeys(List<String> citationKeys) {
        if (citationKeys == null) {
            return;
        }
        citationKeys.stream()
                .filter(resultByCitationKey::containsKey)
                .forEach(delegatedCitationKeys::add);
    }

    public synchronized List<String> delegatedCitationKeysSnapshot() {
        return List.copyOf(delegatedCitationKeys);
    }

    public synchronized void recordMessageMetadata(String messageId, BrainHistoryMessageMetadataVO metadata) {
        if (StringUtils.hasText(messageId) && metadata != null) {
            metadataByMessageId.put(messageId, metadata);
        }
    }

    public synchronized Map<String, BrainHistoryMessageMetadataVO> messageMetadataSnapshot() {
        return Map.copyOf(metadataByMessageId);
    }

    private String hitIdentity(KnowledgeSearchResultVO result) {
        if (result == null) {
            return "null:" + sequence;
        }
        if (StringUtils.hasText(result.getPageBlockId())) {
            return "block:" + result.getPageBlockId();
        }
        if (StringUtils.hasText(result.getChunkId())) {
            return "chunk:" + result.getChunkId();
        }
        return "document:%s:%s:%s".formatted(
                result.getSourceDocumentId(), result.getHeadingPath(), result.getBlockOrder());
    }

    private String resolveFileName(KnowledgeSearchResultVO result) {
        if (StringUtils.hasText(result.getSourceFileName())) {
            return result.getSourceFileName();
        }
        if (StringUtils.hasText(result.getTitle())) {
            return result.getTitle();
        }
        return "未命名知识文档";
    }
}

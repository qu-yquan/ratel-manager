package org.quyq.gwsu.security.brain.service.citation;

import io.agentscope.core.agent.RuntimeContext;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.security.brain.vo.BrainHistoryMessageMetadataVO;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次 Agent 运行的知识引用收集器。
 *
 * <p>父子 RuntimeContext 会共享该对象实例，因此子智能体工具产生的引用能够由父智能体解析。</p>
 */
public final class KnowledgeCitationContext {

    private final Map<String, String> citationKeyByHit = new LinkedHashMap<>();
    private final Map<String, KnowledgeSearchResultVO> resultByCitationKey = new LinkedHashMap<>();
    private final Map<String, BrainHistoryMessageMetadataVO> metadataByMessageId = new LinkedHashMap<>();
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
}

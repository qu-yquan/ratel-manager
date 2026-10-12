package org.quyq.gwsu.security.brain.service.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.TextBlockStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.security.brain.service.citation.KnowledgeCitationContext;
import org.quyq.gwsu.security.brain.vo.BrainHistoryMessageMetadataVO;
import org.quyq.gwsu.security.brain.vo.KnowledgeReferenceVO;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将最终回答中的知识引用编号转换为结构化引用元数据。
 *
 * <p>该中间件只消费检索工具已经完成权限和状态过滤的结果，不再次访问数据库校验。</p>
 */
public class KnowledgeCitationMiddleware implements MiddlewareBase {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(K\\d+)]");
    private static final String KNOWLEDGE_REFERENCES_KEY = "knowledgeReferences";

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext ctx,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        KnowledgeCitationContext citationContext = KnowledgeCitationContext.getOrCreate(ctx);
        AtomicReference<String> liveMessageId = new AtomicReference<>();
        return next.apply(input).concatMap(event -> {
            captureLiveMessageId(event, liveMessageId);
            return enrich(event, citationContext, liveMessageId.get());
        });
    }

    private Flux<AgentEvent> enrich(
            AgentEvent event,
            KnowledgeCitationContext citationContext,
            String liveMessageId) {
        if (!(event instanceof AgentResultEvent resultEvent)) {
            return Flux.just(event);
        }
        Msg result = resultEvent.getResult();
        if (result == null || result.getRole() != MsgRole.ASSISTANT || !StringUtils.hasText(result.getTextContent())) {
            return Flux.just(event);
        }

        List<String> citationKeys = extractCitationKeys(result.getTextContent());
        if (event.getSource() != null) {
            // 子智能体已经完成事实筛选，其最终结果中的引用比全部检索候选更精确。
            citationContext.recordDelegatedCitationKeys(citationKeys);
            return Flux.just(event);
        }
        if (citationKeys.isEmpty()) {
            // 根智能体转述时可能遗漏标记，使用子智能体实际引用过的编号保证结构化引用不丢失。
            citationKeys = citationContext.delegatedCitationKeysSnapshot();
        }
        List<KnowledgeReferenceVO> references = aggregateReferences(citationKeys, citationContext);
        if (references.isEmpty()) {
            return Flux.just(event);
        }

        BrainHistoryMessageMetadataVO messageMetadata = new BrainHistoryMessageMetadataVO()
                .setKnowledgeReferences(references);
        citationContext.recordMessageMetadata(result.getId(), messageMetadata);

        Map<String, Object> metadata = new LinkedHashMap<>(result.getMetadata());
        metadata.put(KNOWLEDGE_REFERENCES_KEY, references);
        Msg enrichedMessage = result.withMetadata(metadata);
        AgentResultEvent enrichedEvent = new AgentResultEvent(
                resultEvent.getId(), resultEvent.getCreatedAt(), enrichedMessage);
        enrichedEvent.withSource(resultEvent.getSource());
        enrichedEvent.withMetadata(resultEvent.getMetadata());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", StringUtils.hasText(liveMessageId) ? liveMessageId : result.getId());
        payload.put("references", references);
        CustomEvent referenceEvent = new CustomEvent(
                AIConstants.AguiCustomEvent.KNOWLEDGE_REFERENCES, payload);
        return Flux.just(referenceEvent, enrichedEvent);
    }

    /**
     * AgentScope AG-UI 使用 {@code replyId-blockId} 作为实时助手消息 ID，它与最终 Msg.id 不一定相同。
     * 这里只跟踪根智能体的文本事件，避免子智能体输出覆盖父智能体消息 ID。
     */
    private void captureLiveMessageId(AgentEvent event, AtomicReference<String> liveMessageId) {
        if (event.getSource() != null) {
            return;
        }
        String messageId = null;
        if (event instanceof TextBlockStartEvent startEvent) {
            messageId = buildLiveMessageId(startEvent.getReplyId(), startEvent.getBlockId());
        } else if (event instanceof TextBlockDeltaEvent deltaEvent) {
            messageId = buildLiveMessageId(deltaEvent.getReplyId(), deltaEvent.getBlockId());
        }
        if (StringUtils.hasText(messageId)) {
            liveMessageId.set(messageId);
        }
    }

    private String buildLiveMessageId(String replyId, String blockId) {
        if (!StringUtils.hasText(replyId) || !StringUtils.hasText(blockId)) {
            return null;
        }
        return replyId + "-" + blockId;
    }

    private List<String> extractCitationKeys(String text) {
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return List.copyOf(keys);
    }

    private List<KnowledgeReferenceVO> aggregateReferences(
            List<String> citationKeys,
            KnowledgeCitationContext citationContext) {
        return citationContext.referencesForCitationKeys(citationKeys);
    }
}

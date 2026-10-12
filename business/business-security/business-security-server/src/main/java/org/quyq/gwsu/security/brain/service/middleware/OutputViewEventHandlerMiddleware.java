package org.quyq.gwsu.security.brain.service.middleware;


import cn.hutool.json.JSONUtil;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventEmitter;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.CustomEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.security.brain.service.citation.KnowledgeCitationContext;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Quyq
 * @date 2026/7/10
 * @description
 */
@Slf4j
public class OutputViewEventHandlerMiddleware implements MiddlewareBase {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(K\\d+)]");

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, RuntimeContext ctx, AgentInput input, Function<AgentInput, Flux<AgentEvent>> next) {
        StringBuilder jsonBuffer = new StringBuilder();
        AtomicInteger braceDepth = new AtomicInteger(0);
        AtomicBoolean inString = new AtomicBoolean(false);
        AtomicBoolean escape = new AtomicBoolean(false);

        return Flux.deferContextual(contextView -> {
            AgentEventEmitter emitter = AgentEventEmitter.fromContext(contextView)
                    .or(() -> AgentEventEmitter.fromForwardingContext(contextView))
                    .orElse(null);
            if (emitter != null) {
                emitter.emit(new CustomEvent("AGENT_OUTPUT_START", Map.of("text", "start")));
            }
            return next.apply(input)
                    .doOnNext(event -> {
                        if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                            TextBlockDeltaEvent deltaEvent = (TextBlockDeltaEvent) event;
                            String text = deltaEvent.getDelta();
                            if (text != null && !text.isEmpty()) {
                                handleChunk(
                                        text,
                                        ctx,
                                        emitter,
                                        jsonBuffer,
                                        braceDepth,
                                        inString,
                                        escape);
                            }
                        } else if (event.getType() == AgentEventType.AGENT_END) {
                            handleEnd(ctx, emitter, jsonBuffer, braceDepth, inString, escape);
                        }
                    });
        });
    }

    private void handleChunk(
            String text,
            RuntimeContext rc,
            AgentEventEmitter emitter,
            StringBuilder jsonBuffer,
            AtomicInteger braceDepth,
            AtomicBoolean inString,
            AtomicBoolean escape) {
        if (emitter == null) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inString.get()) {
                if (escape.get()) {
                    escape.set(false);
                    jsonBuffer.append(c);
                    continue;
                }
                if (c == '\\') {
                    escape.set(true);
                    jsonBuffer.append(c);
                    continue;
                }
                if (c == '"') {
                    inString.set(false);
                    jsonBuffer.append(c);
                    continue;
                }
                if (c == '\n') {
                    log.warn("Unclosed string detected, resetting parser. Buffer: {}", jsonBuffer);
                    resetParser(jsonBuffer, braceDepth, inString, escape);
                    continue;
                }
                jsonBuffer.append(c);
                continue;
            }

            if (braceDepth.get() == 0) {
                if (c == '{') {
                    braceDepth.set(1);
                    jsonBuffer.append(c);
                }
                continue;
            }

            if (c == '{') {
                braceDepth.incrementAndGet();
                jsonBuffer.append(c);
            } else if (c == '}') {
                braceDepth.decrementAndGet();
                jsonBuffer.append(c);
                if (braceDepth.get() == 0) {
                    String json = jsonBuffer.toString();
                    jsonBuffer.setLength(0);
                    trySendPatch(json, rc, emitter);
                }
            } else if (c == '"') {
                inString.set(true);
                jsonBuffer.append(c);
            } else if (c == '\n') {
                jsonBuffer.append(c);
            } else {
                jsonBuffer.append(c);
            }
        }
    }

    private void handleEnd(
            RuntimeContext rc,
            AgentEventEmitter emitter,
            StringBuilder jsonBuffer,
            AtomicInteger braceDepth,
            AtomicBoolean inString,
            AtomicBoolean escape) {
        if (emitter == null) {
            return;
        }

        if (braceDepth.get() > 0) {
            log.warn("Incomplete JSON at end of stream, discarding. BraceDepth: {}, Buffer: {}", braceDepth, jsonBuffer);
            resetParser(jsonBuffer, braceDepth, inString, escape);
        }

        emitter.emit(new CustomEvent("AGENT_OUTPUT_END", java.util.Map.of("text", "done")));
    }

    private void trySendPatch(String json, RuntimeContext runtimeContext, AgentEventEmitter emitter) {
        if (!JSONUtil.isTypeJSON(json)) {
            log.warn("Malformed JSON patch, skipping: {}", json);
            return;
        }

        log.info("输出面板内容：{}", json);
        List<String> citationKeys = extractCitationKeys(json);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", json);
        payload.put("references", KnowledgeCitationContext.getOrCreate(runtimeContext)
                .referencesForCitationKeys(citationKeys));
        emitter.emit(new CustomEvent("AGENT_OUTPUT", payload));
    }

    private List<String> extractCitationKeys(String text) {
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return List.copyOf(keys);
    }

    private void resetParser(StringBuilder jsonBuffer, AtomicInteger braceDepth, AtomicBoolean inString, AtomicBoolean escape) {
        jsonBuffer.setLength(0);
        braceDepth.set(0);
        inString.set(false);
        escape.set(false);
    }

}

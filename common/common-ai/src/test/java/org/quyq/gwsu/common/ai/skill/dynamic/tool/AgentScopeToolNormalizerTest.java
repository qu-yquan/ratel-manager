package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentToolSource;
import org.quyq.gwsu.common.ai.skill.dynamic.model.ApprovalPolicy;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentScopeToolNormalizerTest {

    private final AgentScopeToolNormalizer normalizer = new AgentScopeToolNormalizer();

    @Test
    void shouldNormalizeAnnotatedBeanWithStableExposedName() {
        List<NormalizedSkillTool> tools = normalizer.normalize(
                "kit", new AgentToolSource.AnnotatedBean(new KnowledgeSearchTool()));

        assertEquals(1, tools.size());
        NormalizedSkillTool tool = tools.getFirst();
        assertEquals("SearchKnowledge", tool.originalName());
        assertEquals("KnowledgeSearch", tool.ownerClassShortName());
        assertEquals("kit_KnowledgeSearch_SearchKnowledge", tool.exposedName());
        assertEquals(tool.exposedName(), tool.localTool().getName());
        assertFalse(tool.manifest().readOnly());
        assertEquals(ApprovalPolicy.REQUIRED, tool.manifest().approvalPolicy());
    }

    @Test
    void shouldRejectUnknownEnabledTool() {
        AgentToolSource source = new AgentToolSource.AnnotatedBean(
                new KnowledgeSearchTool(), Set.of("Missing"));

        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("kit", source));
    }

    private static final class KnowledgeSearchTool {

        @Tool(name = "SearchKnowledge", description = "检索知识")
        public Mono<String> search(
                @ToolParam(name = "query", description = "检索词") String query) {
            return Mono.just(query);
        }
    }
}

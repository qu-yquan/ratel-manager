package org.quyq.gwsu.common.ai.skill.dynamic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentSkillProviderTest {

    @Test
    void shouldConfigureSkillAndStandaloneToolWithBuilderDsl() {
        Object skillTool = new Object();
        Object standaloneTool = new Object();
        AgentSkillProvider provider = builder -> builder
                .skill("knowledge", skill -> skill
                        .description("查询业务知识")
                        .content(() -> "# 业务知识")
                        .tool(skillTool, "SearchKnowledge"))
                .tool(standaloneTool);
        AgentSkillBuilder builder = new AgentSkillBuilder("gwsu-kit");

        provider.configure(builder);
        AgentSkillBuilder.Configuration configuration = builder.build();

        assertEquals(1, configuration.skills().size());
        SkillRegistration registration = configuration.skills().getFirst();
        assertEquals("gwsu-kit.knowledge", registration.skill().skillId());
        assertEquals("1.0.0", registration.skill().version());
        assertEquals("# 业务知识", registration.skill().contentLoader().get());
        AgentToolSource.AnnotatedBean registeredSkillTool =
                (AgentToolSource.AnnotatedBean) registration.toolSources().getFirst();
        assertSame(skillTool, registeredSkillTool.bean());
        assertEquals(java.util.Set.of("SearchKnowledge"), registeredSkillTool.enabledTools());
        AgentToolSource.AnnotatedBean registeredStandaloneTool =
                (AgentToolSource.AnnotatedBean) configuration.tools().getFirst();
        assertSame(standaloneTool, registeredStandaloneTool.bean());
    }
}

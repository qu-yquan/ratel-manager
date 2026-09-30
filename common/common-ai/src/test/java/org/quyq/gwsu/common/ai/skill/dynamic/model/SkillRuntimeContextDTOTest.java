package org.quyq.gwsu.common.ai.skill.dynamic.model;

import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillRuntimeContextDTOTest {

    @Test
    void shouldOnlyTransferUserIdAndSessionId() {
        RuntimeContext source = RuntimeContext.builder()
                .userId("user-1001")
                .sessionId("session-2001")
                .put("workspaceId", "workspace-3001")
                .build();

        SkillRuntimeContextDTO dto = SkillRuntimeContextDTO.from(source);
        RuntimeContext restored = dto.toRuntimeContext();

        assertEquals("user-1001", restored.getUserId());
        assertEquals("session-2001", restored.getSessionId());
        assertNull(restored.get("workspaceId"));
    }

    @Test
    void shouldCreateEmptyContextWhenSourceIsNull() {
        RuntimeContext restored = SkillRuntimeContextDTO.from(null).toRuntimeContext();

        assertNull(restored.getUserId());
        assertNull(restored.getSessionId());
    }
}

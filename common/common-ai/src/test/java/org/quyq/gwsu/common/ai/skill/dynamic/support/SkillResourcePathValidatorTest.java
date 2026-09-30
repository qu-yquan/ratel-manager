package org.quyq.gwsu.common.ai.skill.dynamic.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillResourcePathValidatorTest {

    @Test
    void shouldAcceptNormalizedRelativePath() {
        assertEquals("references/rules.md",
                SkillResourcePathValidator.validate("references/rules.md"));
    }

    @Test
    void shouldRejectTraversalAndAbsolutePath() {
        assertThrows(IllegalArgumentException.class,
                () -> SkillResourcePathValidator.validate("../secret.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> SkillResourcePathValidator.validate("/etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> SkillResourcePathValidator.validate("references\\rules.md"));
    }
}

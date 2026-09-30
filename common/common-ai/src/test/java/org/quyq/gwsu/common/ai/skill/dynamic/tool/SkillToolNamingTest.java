package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.core.exception.ArgumentException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillToolNamingTest {

    @Test
    void shouldValidateSkillApplicationNameAndResolveClassShortName() {
        assertEquals("gwsu-system.user_management",
                SkillToolNaming.qualifySkillId("gwsu-system", "user_management"));
        SkillToolNaming.validateSkillId("gwsu-system", "gwsu-system.user_management");
        SkillToolNaming.validateSkillId("gwsu.system", "gwsu.system.user_management");
        assertEquals("UserQuery", SkillToolNaming.classShortName(new UserQueryTool()));
        assertEquals("gwsu-system_UserQuery_Query",
                SkillToolNaming.exposedName("gwsu-system", "UserQuery", "Query"));
    }

    @Test
    void shouldRejectSkillIdWithoutApplicationName() {
        assertThrows(ArgumentException.class,
                () -> SkillToolNaming.qualifySkillId(
                        "gwsu-system", "gwsu-system.user_management"));
        assertThrows(ArgumentException.class,
                () -> SkillToolNaming.validateSkillId("gwsu-system", "user_management"));
        assertThrows(ArgumentException.class,
                () -> SkillToolNaming.validateSkillId("gwsu-system", "gwsu-system."));
    }

    private static final class UserQueryTool {
    }
}

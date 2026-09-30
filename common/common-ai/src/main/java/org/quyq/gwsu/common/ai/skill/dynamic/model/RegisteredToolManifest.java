package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.Map;

public record RegisteredToolManifest(
        String originalName,
        String exposedName,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Boolean strict,
        boolean readOnly,
        ApprovalPolicy approvalPolicy
) {
    public RegisteredToolManifest {
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
        outputSchema = outputSchema == null ? Map.of() : Map.copyOf(outputSchema);
    }
}

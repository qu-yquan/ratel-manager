package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.Objects;

public record StandaloneToolManifest(
        String applicationName,
        RegisteredToolManifest tool
) {
    public StandaloneToolManifest {
        Objects.requireNonNull(applicationName, "applicationName 不能为空");
        Objects.requireNonNull(tool, "tool 不能为空");
    }
}

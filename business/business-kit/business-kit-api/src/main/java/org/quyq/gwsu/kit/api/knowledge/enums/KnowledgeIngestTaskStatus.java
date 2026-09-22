package org.quyq.gwsu.kit.api.knowledge.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 知识文档导入任务状态。
 */
public enum KnowledgeIngestTaskStatus {

    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SUPERSEDED;

    public boolean canTransitTo(KnowledgeIngestTaskStatus target) {
        Set<KnowledgeIngestTaskStatus> targets = switch (this) {
            case PENDING -> EnumSet.of(RUNNING, SUPERSEDED);
            case RUNNING -> EnumSet.of(SUCCEEDED, FAILED, SUPERSEDED);
            case FAILED -> EnumSet.of(PENDING);
            case SUCCEEDED, SUPERSEDED -> EnumSet.noneOf(KnowledgeIngestTaskStatus.class);
        };
        return targets.contains(target);
    }
}

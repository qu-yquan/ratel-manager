package org.quyq.gwsu.kit.knowledge.task;

/**
 * 导入任务已被新的重新导入任务取代。
 */
public class KnowledgeIngestSupersededException extends RuntimeException {

    public KnowledgeIngestSupersededException(String taskId) {
        super("知识导入任务已被取代: " + taskId);
    }
}

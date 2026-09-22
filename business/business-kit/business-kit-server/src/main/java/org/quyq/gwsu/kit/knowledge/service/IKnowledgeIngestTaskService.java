package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;

/**
 * 知识文档导入任务服务。
 */
public interface IKnowledgeIngestTaskService extends IService<KitKnowledgeIngestTask> {

    String createTask(String sourceDocumentId);

    void ensureNoActiveTask(String sourceDocumentId);

    String retry(String taskId);
}

package org.quyq.gwsu.kit.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 知识导入应用编排服务。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestApplicationService {

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    private final IKnowledgeIngestTaskService ingestTaskService;

    private final KnowledgeIngestDispatcher ingestDispatcher;

    private final KnowledgeDocumentEventService eventService;

    private final KnowledgeDirectoryService directoryService;

    @Transactional(rollbackFor = Exception.class)
    public String saveDocumentAndSubmit(KnowledgeDocumentSaveDTO dto) {
        String sourceDocumentId = sourceDocumentService.saveDocument(dto);
        String taskId = ingestTaskService.createTask(sourceDocumentId);
        eventService.append(sourceDocumentId, taskId, "UPLOADED", "文档已加入知识库并提交解析");
        ingestDispatcher.dispatchAfterCommit(taskId);
        return taskId;
    }

    @Transactional(rollbackFor = Exception.class)
    public String retryAndSubmit(String taskId) {
        var originalTask = ingestTaskService.getById(taskId);
        if (originalTask == null) throw new org.quyq.gwsu.common.core.exception.BusinessException("导入任务不存在");
        directoryService.requireEditableDocument(originalTask.getSourceDocumentId());
        String retryTaskId = ingestTaskService.retry(taskId);
        var task = ingestTaskService.getById(retryTaskId);
        eventService.append(task.getSourceDocumentId(), retryTaskId, "RETRY_SUBMITTED", "已重新提交导入任务");
        ingestDispatcher.dispatchAfterCommit(retryTaskId);
        return retryTaskId;
    }
}

package org.quyq.gwsu.kit.knowledge.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.kit.knowledge.task.KnowledgeIngestExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;

/**
 * 知识导入异步派发器。
 */
@Slf4j
@Component
public class KnowledgeIngestDispatcher {

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    private final KnowledgeIngestExecutor ingestExecutor;

    private final KnowledgeIngestFailureService failureService;

    private final KnowledgeDocumentEventService eventService;

    public KnowledgeIngestDispatcher(KnowledgeIngestExecutor ingestExecutor,
                                     KnowledgeIngestFailureService failureService,
                                     KnowledgeDocumentEventService eventService) {
        this.ingestExecutor = ingestExecutor;
        this.failureService = failureService;
        this.eventService = eventService;
    }

    public void dispatchAfterCommit(String taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(taskId);
                }
            });
            return;
        }
        dispatch(taskId);
    }

    public void dispatch(String taskId) {
        try {
            executorService.submit(() -> {
                try {
                    ingestExecutor.execute(taskId);
                } catch (Throwable ex) {
                    log.error("知识导入任务执行失败, taskId={}", taskId, ex);
                }
            });
        } catch (RuntimeException dispatchFailure) {
            log.error("知识导入任务派发失败, taskId={}", taskId, dispatchFailure);
            String documentId = null;
            try {
                documentId = failureService.markFailed(taskId, dispatchFailure);
            } catch (Throwable statusFailure) {
                log.error("知识导入派发失败状态写入失败, taskId={}", taskId, statusFailure);
            }
            if (documentId != null) {
                try {
                    eventService.append(documentId, taskId, "INGEST_FAILED",
                            KnowledgeIngestFailureService.failureMessage(dispatchFailure));
                } catch (Throwable eventFailure) {
                    log.error("知识导入派发失败事件记录失败, taskId={}, documentId={}", taskId, documentId, eventFailure);
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }
}

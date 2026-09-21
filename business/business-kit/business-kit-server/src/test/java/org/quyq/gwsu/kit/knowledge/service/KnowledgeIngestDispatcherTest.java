package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.knowledge.task.KnowledgeIngestExecutor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeIngestDispatcherTest {
    @Test
    void rejectedDispatchMarksTaskFailedAndRecordsEvent() {
        KnowledgeIngestExecutor executor = mock(KnowledgeIngestExecutor.class);
        KnowledgeIngestFailureService failureService = mock(KnowledgeIngestFailureService.class);
        KnowledgeDocumentEventService eventService = mock(KnowledgeDocumentEventService.class);
        KnowledgeIngestDispatcher dispatcher = new KnowledgeIngestDispatcher(executor, failureService, eventService);
        dispatcher.destroy();
        when(failureService.markFailed(eq("task"), any(Throwable.class))).thenReturn("document");

        dispatcher.dispatch("task");

        verify(failureService).markFailed(eq("task"), any(Throwable.class));
        verify(eventService).append(eq("document"), eq("task"), eq("INGEST_FAILED"), anyString());
        verifyNoInteractions(executor);
    }
}

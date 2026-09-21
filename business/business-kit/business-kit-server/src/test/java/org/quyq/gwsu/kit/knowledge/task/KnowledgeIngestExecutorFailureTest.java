package org.quyq.gwsu.kit.knowledge.task;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeIngestSanitizer;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentationService;
import org.quyq.gwsu.kit.knowledge.engine.page.HighFidelityKnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceSegmentMapper;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentEventService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeIngestFailureService;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPagePublishService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeIngestExecutorFailureTest {
    private final KnowledgeIngestTaskMapper taskMapper = mock(KnowledgeIngestTaskMapper.class);
    private final KnowledgeSourceDocumentMapper documentMapper = mock(KnowledgeSourceDocumentMapper.class);
    private final KnowledgeDocumentEventService eventService = mock(KnowledgeDocumentEventService.class);
    private final KnowledgeIngestFailureService failureService = mock(KnowledgeIngestFailureService.class);
    private final KnowledgeIngestExecutor executor = new KnowledgeIngestExecutor(
            taskMapper, documentMapper, mock(KnowledgePageVersionMapper.class),
            mock(KnowledgePageBlockMapper.class), mock(KnowledgePageSourceRefMapper.class),
            mock(KnowledgeProperties.class), mock(KnowledgeDocumentParser.class),
            mock(KnowledgeIngestSanitizer.class), mock(KnowledgeSourceSegmentationService.class),
            mock(KnowledgeSourceSegmentMapper.class), mock(HighFidelityKnowledgePageGenerator.class),
            mock(KnowledgeSourceDocumentPagePublishService.class), mock(KnowledgeChunkBuilder.class),
            mock(KnowledgeChunkEmbeddingService.class), mock(KnowledgeChunkIndexRepository.class),
            eventService, failureService);

    @Test
    void lookupFailureStillRecordsFailureAndKeepsOriginalException() {
        IllegalStateException original = new IllegalStateException("读取任务失败");
        when(taskMapper.selectOne(any())).thenThrow(original);
        when(failureService.markFailed(eq("task"), same(original))).thenReturn("document");
        doThrow(new IllegalStateException("事件表不可用")).when(eventService)
                .append(eq("document"), eq("task"), eq("INGEST_FAILED"), anyString());

        assertSame(original, assertThrows(IllegalStateException.class, () -> executor.execute("task")));

        verify(failureService).markFailed("task", original);
        verify(eventService).append(eq("document"), eq("task"), eq("INGEST_FAILED"), anyString());
    }

    @Test
    void eventIsRecordedEvenIfFailureStatusWriteAlsoFails() {
        when(taskMapper.selectOne(any())).thenReturn(new KitKnowledgeIngestTask().setSourceDocumentId("document"));
        when(failureService.markFailed(eq("task"), any(Throwable.class)))
                .thenThrow(new IllegalStateException("状态库不可用"));

        assertThrows(BusinessException.class, () -> executor.execute("task"));

        verify(eventService).append(eq("document"), eq("task"), eq("INGEST_FAILED"), anyString());
    }
}

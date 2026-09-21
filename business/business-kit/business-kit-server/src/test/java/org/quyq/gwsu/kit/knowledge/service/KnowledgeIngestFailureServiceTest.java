package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeIngestFailureServiceTest {
    private final KnowledgeIngestTaskMapper taskMapper = mock(KnowledgeIngestTaskMapper.class);
    private final KnowledgeSourceDocumentMapper documentMapper = mock(KnowledgeSourceDocumentMapper.class);
    private final KnowledgeIngestFailureService service = new KnowledgeIngestFailureService(taskMapper, documentMapper);

    @Test
    void recordsTaskAndDocumentFailureWithBoundedRootCause() {
        when(taskMapper.selectOne(any())).thenReturn(new KitKnowledgeIngestTask().setSourceDocumentId("document"));
        when(taskMapper.update(any(KitKnowledgeIngestTask.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(documentMapper.update(any(KitKnowledgeSourceDocument.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        Throwable failure = new IllegalStateException("向量化失败", new IOException("连接超时"));

        assertEquals("document", service.markFailed("task", failure));

        var taskUpdate = org.mockito.ArgumentCaptor.forClass(KitKnowledgeIngestTask.class);
        var documentUpdate = org.mockito.ArgumentCaptor.forClass(KitKnowledgeSourceDocument.class);
        verify(taskMapper).update(taskUpdate.capture(), any(LambdaUpdateWrapper.class));
        verify(documentMapper).update(documentUpdate.capture(), any(LambdaUpdateWrapper.class));
        assertEquals(KnowledgeIngestTaskStatus.FAILED, taskUpdate.getValue().getTaskStatus());
        assertEquals(KnowledgeDocumentStatus.FAILED, documentUpdate.getValue().getDocumentStatus());
        assertTrue(KnowledgeIngestFailureService.failureMessage(failure).contains("连接超时"));
        assertTrue(KnowledgeIngestFailureService.failureMessage(new RuntimeException("x".repeat(3000))).length() <= 900);
    }
}

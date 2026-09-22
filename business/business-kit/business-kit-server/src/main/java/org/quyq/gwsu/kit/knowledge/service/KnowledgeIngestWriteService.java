package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceSegment;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentDraft;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceSegmentMapper;
import org.quyq.gwsu.kit.knowledge.task.KnowledgeIngestSupersededException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 在文档行锁保护下提交导入阶段产物，避免已作废任务覆盖新任务结果。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestWriteService {

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeSourceSegmentMapper sourceSegmentMapper;

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final KnowledgeDocumentEventService eventService;

    @Transactional(rollbackFor = Exception.class)
    public void replaceSourceSegments(String taskId,
                                      String sourceDocumentId,
                                      List<KnowledgeSourceSegmentDraft> segments) {
        requireActiveDocument(taskId, sourceDocumentId);
        sourceSegmentMapper.delete(new LambdaUpdateWrapper<KitKnowledgeSourceSegment>()
                .eq(KitKnowledgeSourceSegment::getSourceDocumentId, sourceDocumentId)
                .eq(KitKnowledgeSourceSegment::getDeleted, false));
        if (CollectionUtils.isEmpty(segments)) {
            return;
        }
        for (KnowledgeSourceSegmentDraft segment : segments) {
            sourceSegmentMapper.insert(new KitKnowledgeSourceSegment()
                    .setSourceDocumentId(sourceDocumentId)
                    .setSegmentNo(segment.segmentNo())
                    .setSegmentType(segment.segmentType())
                    .setHeadingPath(segment.headingPath())
                    .setSourceLocator(segment.sourceLocator())
                    .setContent(segment.content()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(String taskId,
                         String sourceDocumentId,
                         String pageId,
                         String pageVersionId,
                         List<KnowledgeChunkDocument> chunks) {
        requireActiveDocument(taskId, sourceDocumentId);
        chunkIndexRepository.replacePageVersion(pageId, pageVersionId, chunks);

        int taskUpdated = ingestTaskMapper.update(new KitKnowledgeIngestTask()
                        .setTaskStatus(KnowledgeIngestTaskStatus.SUCCEEDED)
                        .setFinishedAt(LocalDateTime.now()),
                new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                        .eq(KitKnowledgeIngestTask::getId, taskId)
                        .eq(KitKnowledgeIngestTask::getTaskStatus, KnowledgeIngestTaskStatus.RUNNING)
                        .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (taskUpdated != 1) {
            throw new KnowledgeIngestSupersededException(taskId);
        }

        int documentUpdated = sourceDocumentMapper.update(new KitKnowledgeSourceDocument()
                        .setDocumentStatus(KnowledgeDocumentStatus.PROCESSED)
                        .setProcessedAt(LocalDateTime.now()),
                new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                        .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                        .eq(KitKnowledgeSourceDocument::getActiveTaskId, taskId)
                        .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (documentUpdated != 1) {
            throw new KnowledgeIngestSupersededException(taskId);
        }
        eventService.append(sourceDocumentId, taskId, "INGEST_SUCCEEDED", "文档解析与向量化完成");
    }

    private KitKnowledgeSourceDocument requireActiveDocument(String taskId, String sourceDocumentId) {
        KitKnowledgeSourceDocument document = sourceDocumentMapper.selectByIdForUpdate(sourceDocumentId);
        if (document == null || !Objects.equals(taskId, document.getActiveTaskId())) {
            throw new KnowledgeIngestSupersededException(taskId);
        }
        return document;
    }
}

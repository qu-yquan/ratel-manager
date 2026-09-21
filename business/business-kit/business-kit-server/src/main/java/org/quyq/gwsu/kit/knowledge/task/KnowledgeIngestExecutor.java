package org.quyq.gwsu.kit.knowledge.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuildRequest;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeIngestSanitizer;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentDraft;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentationService;
import org.quyq.gwsu.kit.knowledge.engine.ingest.ParsedKnowledgeDocument;
import org.quyq.gwsu.kit.knowledge.engine.ingest.SegmentedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.engine.ingest.SanitizedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePageDraft;
import org.quyq.gwsu.kit.knowledge.engine.page.HighFidelityKnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceSegment;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceSegmentMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPagePublishService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentEventService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeIngestFailureService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识源文档导入执行器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KnowledgeIngestExecutor {

    private static final Gson GSON = new Gson();

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeProperties knowledgeProperties;

    private final KnowledgeDocumentParser documentParser;

    private final KnowledgeIngestSanitizer ingestSanitizer;

    private final KnowledgeSourceSegmentationService segmentationService;

    private final KnowledgeSourceSegmentMapper sourceSegmentMapper;

    private final HighFidelityKnowledgePageGenerator highFidelityPageGenerator;

    private final KnowledgeSourceDocumentPagePublishService pagePublishService;

    private final KnowledgeChunkBuilder chunkBuilder;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final KnowledgeDocumentEventService eventService;

    private final KnowledgeIngestFailureService failureService;

    public void execute(String taskId) {
        KitKnowledgeIngestTask task = null;
        try {
            task = loadTask(taskId);
            markTaskRunning(task);
            KitKnowledgeSourceDocument sourceDocument = loadSourceDocument(task);

            updateStage(task.getId(), KnowledgeIngestStage.PARSE);
            ParsedKnowledgeDocument parsedDocument = documentParser.parse(sourceDocument.getFileId());
            updateSourceStatus(sourceDocument.getId(), new KitKnowledgeSourceDocument().setParsedAt(LocalDateTime.now()));
            eventService.append(sourceDocument.getId(), taskId, "PARSE_SUCCEEDED", "文件解析成功");
            updateSourceParsedImages(sourceDocument.getId(), parsedDocument.imageFileIds());
            updateSourceImageOcrParsed(sourceDocument.getId(), parsedDocument.imageOcrParsed());

            updateStage(task.getId(), KnowledgeIngestStage.SANITIZE_SOURCE);
            SanitizedKnowledgeSource sanitizedSource = ingestSanitizer.sanitize(parsedDocument);
            updateSourceProcessMessage(sourceDocument.getId(), sanitizedSource.warnings());

            updateStage(task.getId(), KnowledgeIngestStage.ANALYZE_SOURCE);
            SegmentedKnowledgeSource segmentedSource = segmentationService.segment(parsedDocument, sanitizedSource);
            replaceSourceSegments(sourceDocument.getId(), segmentedSource.segments());

            updateStage(task.getId(), KnowledgeIngestStage.GENERATE_PAGE);
            log.info("开始高保真生成知识页面: taskId={}, sourceDocumentId={}, fileName={}, parsedSourceLanguage={}, segmentedSourceLanguage={}, segmentCount={}, warnings={}",
                    task.getId(),
                    sourceDocument.getId(),
                    parsedDocument.fileName(),
                    parsedDocument.sourceLanguage(),
                    segmentedSource.sourceLanguage(),
                    segmentedSource.segments().size(),
                    sanitizedSource.warnings());
            GeneratedKnowledgePageDraft generatedPage = highFidelityPageGenerator.generate(
                    parsedDocument.fileName(),
                    knowledgeProperties.getWikiOutputLanguage(),
                    segmentedSource);
            log.info("知识页面生成结果: taskId={}, sourceDocumentId={}, titlePreview={}, markdownLength={}",
                    task.getId(),
                    sourceDocument.getId(),
                    abbreviate(generatedPage.title(), 120),
                    safeLength(generatedPage.markdownContent()));

            updateStage(task.getId(), KnowledgeIngestStage.MERGE_PAGE);
            String pageVersionId = pagePublishService.publish(sourceDocument, generatedPage);

            updateStage(task.getId(), KnowledgeIngestStage.BUILD_CHUNK);
            KitKnowledgePageVersion pageVersion = loadPageVersion(pageVersionId);
            List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(pageVersionId);
            List<KitKnowledgePageSourceRef> sourceRefs = loadSourceRefs(blocks);
            List<KnowledgeChunkDocument> chunks = chunkBuilder.build(new KnowledgeChunkBuildRequest(
                    pageVersion.getPageId(),
                    generatedPage.title(),
                    pageVersion,
                    blocks,
                    sourceRefs));

            updateStage(task.getId(), KnowledgeIngestStage.EMBED_CHUNK);
            boolean embeddingCompleted = chunkEmbeddingService.embedChunks(chunks);
            updateSourceEmbeddingCompleted(sourceDocument.getId(), embeddingCompleted);

            updateStage(task.getId(), KnowledgeIngestStage.INDEX_ES);
            chunkIndexRepository.replacePageVersion(pageVersion.getPageId(), pageVersion.getId(), chunks);
            markSucceeded(task, sourceDocument);
            eventService.append(sourceDocument.getId(), taskId, "INGEST_SUCCEEDED", "文档解析与向量化完成");
        } catch (Throwable failure) {
            String documentId = task == null ? null : task.getSourceDocumentId();
            try {
                documentId = failureService.markFailed(taskId, failure);
            } catch (Throwable statusFailure) {
                log.error("知识导入失败状态写入失败, taskId={}", taskId, statusFailure);
            }
            if (StringUtils.hasText(documentId)) {
                try {
                    eventService.append(documentId, taskId, "INGEST_FAILED",
                            KnowledgeIngestFailureService.failureMessage(failure));
                } catch (Throwable eventFailure) {
                    log.error("知识导入失败事件记录失败, taskId={}, documentId={}", taskId, documentId, eventFailure);
                }
            }
            if (failure instanceof Error error) throw error;
            if (failure instanceof RuntimeException exception) throw exception;
            throw new IllegalStateException(failure);
        }
    }

    private KitKnowledgeIngestTask loadTask(String taskId) {
        KitKnowledgeIngestTask task = ingestTaskMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(task)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        return task;
    }

    private KitKnowledgeSourceDocument loadSourceDocument(KitKnowledgeIngestTask task) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, task.getSourceDocumentId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        return sourceDocument;
    }

    private KitKnowledgePageVersion loadPageVersion(String pageVersionId) {
        KitKnowledgePageVersion pageVersion = pageVersionMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePageVersion>()
                .eq(KitKnowledgePageVersion::getId, pageVersionId)
                .eq(KitKnowledgePageVersion::getDeleted, false));
        if (Objects.isNull(pageVersion)) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        return pageVersion;
    }

    private List<KitKnowledgePageSourceRef> loadSourceRefs(List<KitKnowledgePageBlock> blocks) {
        if (CollectionUtils.isEmpty(blocks)) {
            return List.of();
        }
        return pageSourceRefMapper.selectByPageBlockIds(blocks.stream()
                .map(KitKnowledgePageBlock::getId)
                .toList());
    }

    private void markTaskRunning(KitKnowledgeIngestTask task) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.RUNNING)
                .setStartedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSING)
                .setProcessMessage(null)
                .setImageOcrParsed(false)
                .setEmbeddingCompleted(false));
    }

    private void updateStage(String taskId, KnowledgeIngestStage stage) {
        updateTask(taskId, new KitKnowledgeIngestTask().setCurrentStage(stage));
    }

    private void markSucceeded(KitKnowledgeIngestTask task, KitKnowledgeSourceDocument sourceDocument) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.SUCCEEDED)
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(sourceDocument.getId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSED)
                .setProcessedAt(LocalDateTime.now()));
    }

    private void updateTask(String taskId, KitKnowledgeIngestTask update) {
        ingestTaskMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
    }

    private void updateSourceStatus(String sourceDocumentId, KitKnowledgeSourceDocument update) {
        sourceDocumentMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
    }

    private void updateSourceEmbeddingCompleted(String sourceDocumentId, boolean embeddingCompleted) {
        updateSourceStatus(sourceDocumentId, new KitKnowledgeSourceDocument()
                .setEmbeddingCompleted(embeddingCompleted));
    }

    private void updateSourceParsedImages(String sourceDocumentId, List<String> imageFileIds) {
        updateSourceStatus(sourceDocumentId, new KitKnowledgeSourceDocument()
                .setImageFileIdsJson(GSON.toJson(imageFileIds == null ? List.of() : imageFileIds)));
    }

    private void updateSourceImageOcrParsed(String sourceDocumentId, boolean imageOcrParsed) {
        updateSourceStatus(sourceDocumentId, new KitKnowledgeSourceDocument()
                .setImageOcrParsed(imageOcrParsed));
    }

    private void updateSourceProcessMessage(String sourceDocumentId, List<String> warnings) {
        String message = warnings == null || warnings.isEmpty()
                ? null
                : String.join("；", warnings);
        updateSourceStatus(sourceDocumentId, new KitKnowledgeSourceDocument()
                .setProcessMessage(message));
    }

    private void replaceSourceSegments(String sourceDocumentId, List<KnowledgeSourceSegmentDraft> segments) {
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

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...[truncated]";
    }
}

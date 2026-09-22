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
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeIngestSanitizer;
import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentationService;
import org.quyq.gwsu.kit.knowledge.engine.ingest.ParsedKnowledgeDocument;
import org.quyq.gwsu.kit.knowledge.engine.ingest.SegmentedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.engine.ingest.SanitizedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePageDraft;
import org.quyq.gwsu.kit.knowledge.engine.page.HighFidelityKnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPagePublishService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentEventService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeIngestFailureService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeIngestWriteService;
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

    private final HighFidelityKnowledgePageGenerator highFidelityPageGenerator;

    private final KnowledgeSourceDocumentPagePublishService pagePublishService;

    private final KnowledgeChunkBuilder chunkBuilder;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeDocumentEventService eventService;

    private final KnowledgeIngestFailureService failureService;

    private final KnowledgeIngestWriteService writeService;

    public void execute(String taskId) {
        KitKnowledgeIngestTask task = null;
        try {
            task = loadTask(taskId);
            markTaskRunning(task);
            KitKnowledgeSourceDocument sourceDocument = loadSourceDocument(task);

            updateStage(task.getId(), KnowledgeIngestStage.PARSE);
            ParsedKnowledgeDocument parsedDocument = documentParser.parse(sourceDocument.getFileId());
            updateSourceStatus(taskId, sourceDocument.getId(),
                    new KitKnowledgeSourceDocument().setParsedAt(LocalDateTime.now()));
            eventService.append(sourceDocument.getId(), taskId, "PARSE_SUCCEEDED", "文件解析成功");
            updateSourceParsedImages(taskId, sourceDocument.getId(), parsedDocument.imageFileIds());
            updateSourceImageOcrParsed(taskId, sourceDocument.getId(), parsedDocument.imageOcrParsed());

            updateStage(task.getId(), KnowledgeIngestStage.SANITIZE_SOURCE);
            SanitizedKnowledgeSource sanitizedSource = ingestSanitizer.sanitize(parsedDocument);
            updateSourceProcessMessage(taskId, sourceDocument.getId(), sanitizedSource.warnings());

            updateStage(task.getId(), KnowledgeIngestStage.ANALYZE_SOURCE);
            SegmentedKnowledgeSource segmentedSource = segmentationService.segment(parsedDocument, sanitizedSource);
            writeService.replaceSourceSegments(taskId, sourceDocument.getId(), segmentedSource.segments());

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
            String pageVersionId = pagePublishService.publish(taskId, sourceDocument.getId(), generatedPage);

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
            updateSourceEmbeddingCompleted(taskId, sourceDocument.getId(), embeddingCompleted);

            updateStage(task.getId(), KnowledgeIngestStage.INDEX_ES);
            writeService.complete(taskId, sourceDocument.getId(), pageVersion.getPageId(), pageVersion.getId(), chunks);
        } catch (KnowledgeIngestSupersededException superseded) {
            log.info("知识导入任务已被重新导入取代，停止旧任务: taskId={}", taskId);
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
                .eq(KitKnowledgeSourceDocument::getActiveTaskId, task.getId())
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
        int taskUpdated = ingestTaskMapper.update(new KitKnowledgeIngestTask()
                        .setTaskStatus(KnowledgeIngestTaskStatus.RUNNING)
                        .setStartedAt(LocalDateTime.now()),
                new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                        .eq(KitKnowledgeIngestTask::getId, task.getId())
                        .eq(KitKnowledgeIngestTask::getTaskStatus, KnowledgeIngestTaskStatus.PENDING)
                        .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (taskUpdated != 1) {
            throw new KnowledgeIngestSupersededException(task.getId());
        }
        updateSourceStatus(task.getId(), task.getSourceDocumentId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSING)
                .setProcessMessage(null)
                .setImageOcrParsed(false)
                .setEmbeddingCompleted(false));
    }

    private void updateStage(String taskId, KnowledgeIngestStage stage) {
        int updated = ingestTaskMapper.update(new KitKnowledgeIngestTask().setCurrentStage(stage),
                new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getTaskStatus, KnowledgeIngestTaskStatus.RUNNING)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (updated != 1) {
            throw new KnowledgeIngestSupersededException(taskId);
        }
    }

    private void updateSourceStatus(String taskId,
                                    String sourceDocumentId,
                                    KitKnowledgeSourceDocument update) {
        int updated = sourceDocumentMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getActiveTaskId, taskId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (updated != 1) {
            throw new KnowledgeIngestSupersededException(taskId);
        }
    }

    private void updateSourceEmbeddingCompleted(String taskId,
                                                String sourceDocumentId,
                                                boolean embeddingCompleted) {
        updateSourceStatus(taskId, sourceDocumentId, new KitKnowledgeSourceDocument()
                .setEmbeddingCompleted(embeddingCompleted));
    }

    private void updateSourceParsedImages(String taskId,
                                          String sourceDocumentId,
                                          List<String> imageFileIds) {
        updateSourceStatus(taskId, sourceDocumentId, new KitKnowledgeSourceDocument()
                .setImageFileIdsJson(GSON.toJson(imageFileIds == null ? List.of() : imageFileIds)));
    }

    private void updateSourceImageOcrParsed(String taskId,
                                            String sourceDocumentId,
                                            boolean imageOcrParsed) {
        updateSourceStatus(taskId, sourceDocumentId, new KitKnowledgeSourceDocument()
                .setImageOcrParsed(imageOcrParsed));
    }

    private void updateSourceProcessMessage(String taskId,
                                            String sourceDocumentId,
                                            List<String> warnings) {
        String message = warnings == null || warnings.isEmpty()
                ? null
                : String.join("；", warnings);
        updateSourceStatus(taskId, sourceDocumentId, new KitKnowledgeSourceDocument()
                .setProcessMessage(message));
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

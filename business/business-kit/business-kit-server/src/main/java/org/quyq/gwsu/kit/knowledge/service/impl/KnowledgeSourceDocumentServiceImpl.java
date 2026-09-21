package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestAnalysisCheckpoint;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceSegment;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestAnalysisCheckpointMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDirectoryService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentEventService;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceSegmentMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 知识源文档服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSourceDocumentServiceImpl
        extends ServiceImpl<KnowledgeSourceDocumentMapper, KitKnowledgeSourceDocument>
        implements IKnowledgeSourceDocumentService {

    private static final Gson GSON = new Gson();

    private final KnowledgeDirectoryService directoryService;

    private final KnowledgeDocumentEventService eventService;

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeIngestAnalysisCheckpointMapper checkpointMapper;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final KnowledgeSourceDocumentPageSyncService pageSyncService;

    private final KnowledgeSourceSegmentMapper sourceSegmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDocument(KnowledgeDocumentSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getFileId())) {
            throw new BusinessException("请上传文件");
        }
        if (!StringUtils.hasText(dto.getFileName()) || dto.getFileName().trim().length() > 200) {
            throw new BusinessException("文件名不能为空且长度不能超过200");
        }
        KitKnowledgeSourceDocument directory = StringUtils.hasText(dto.getParentId())
                ? directoryService.requireDirectory(dto.getParentId()) : null;
        if (directory == null ? !directoryService.canReadRoot()
                : !directoryService.canRead(directory, directoryService.grantedDirectoryIds())) {
            throw new BusinessException("没有目标目录的访问权限");
        }
        var fileInfo = FileUtils.getFileInfo(dto.getFileId());
        if (fileInfo == null) throw new BusinessException("上传文件不存在");
        LambdaQueryWrapper<KitKnowledgeSourceDocument> duplicateQuery = new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getNodeType, KnowledgeDirectoryService.DOCUMENT)
                .eq(KitKnowledgeSourceDocument::getName, dto.getFileName().trim())
                .eq(KitKnowledgeSourceDocument::getDeleted, false);
        if (directory == null) duplicateQuery.isNull(KitKnowledgeSourceDocument::getParentId);
        else duplicateQuery.eq(KitKnowledgeSourceDocument::getParentId, directory.getId());
        if (count(duplicateQuery) > 0) {
            throw new BusinessException("该目录已存在同名文档");
        }
        Long fileSize;
        try {
            fileSize = StringUtils.hasText(fileInfo.getFileSize()) ? Long.parseLong(fileInfo.getFileSize()) : null;
        } catch (NumberFormatException ex) {
            throw new BusinessException("文件大小格式不正确");
        }
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument()
                .setParentId(directory == null ? null : directory.getId())
                .setDirectoryPath(directory == null ? "" : directory.getDirectoryPath())
                .setNodeType(KnowledgeDirectoryService.DOCUMENT)
                .setName(dto.getFileName().trim())
                .setSortNo(0)
                .setFileId(dto.getFileId())
                .setFileName(dto.getFileName().trim())
                .setFileSize(fileSize)
                .setFileFormat(fileInfo.getFileSuffix())
                .setDocumentStatus(KnowledgeDocumentStatus.UPLOADED)
                .setTargetPageId(null)
                .setProcessMessage(null)
                .setImageFileIdsJson(null)
                .setImageOcrParsed(false)
                .setEmbeddingCompleted(false)
                .setEnabled(true);

        save(document);

        return document.getId();
    }

    @Override
    public KnowledgeDocumentVO getDocument(String documentId) {
        KitKnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new org.quyq.gwsu.common.core.exception.BusinessException(KitErrorCode.E03001);
        }
        directoryService.requireReadableDocument(documentId);
        return toDocumentVO(document, groupLatestTasks(List.of(document.getId())).get(document.getId()));
    }

    @Override
    public void updateEnabled(String documentId, boolean enabled) {
        directoryService.requireReadableDocument(documentId);
        long updated = baseMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getEnabled, enabled));
        if (updated == 0) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        if (!enabled) {
            chunkIndexRepository.deleteBySourceDocumentId(documentId);
            eventService.append(documentId, null, "DOCUMENT_DISABLED", "文档已禁用");
            return;
        }
        pageSyncService.reindexCurrentPagesBySourceDocumentId(documentId);
        eventService.append(documentId, null, "DOCUMENT_ENABLED", "文档已启用");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeDocumentDerivedData(String documentId) {
        KitKnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        purgeDocumentArtifacts(documentId, false, false);
        removeDerivedKnowledgeFiles(document.getImageFileIdsJson());
        baseMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getTargetPageId, null)
                .set(KitKnowledgeSourceDocument::getImageFileIdsJson, null)
                .set(KitKnowledgeSourceDocument::getImageOcrParsed, false)
                .set(KitKnowledgeSourceDocument::getEmbeddingCompleted, false)
                .set(KitKnowledgeSourceDocument::getParsedAt, null)
                .set(KitKnowledgeSourceDocument::getProcessedAt, null)
                .set(KitKnowledgeSourceDocument::getProcessMessage, null));
    }

    @Override
    public void deleteDocumentData(String documentId) {
        KitKnowledgeSourceDocument document = baseMapper.selectById(documentId);
        if (document == null || !KnowledgeDirectoryService.DOCUMENT.equals(document.getNodeType())) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        purgeDocumentArtifacts(documentId, true, true);
        removeDerivedKnowledgeFiles(document.getImageFileIdsJson());
        eventService.deleteByDocumentId(documentId);
        if (StringUtils.hasText(document.getFileId())) {
            FileUtils.delete(document.getFileId());
        }
    }

    private KnowledgeDocumentVO toDocumentVO(KitKnowledgeSourceDocument document,
                                             KitKnowledgeIngestTask latestTask) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO()
                .setId(document.getId())
                .setParentId(document.getParentId())
                .setFileSize(document.getFileSize())
                .setFileFormat(document.getFileFormat())
                .setFileId(document.getFileId())
                .setFileName(document.getFileName())
                .setDocumentStatus(document.getDocumentStatus())
                .setProcessMessage(document.getProcessMessage())
                .setImageOcrParsed(Boolean.TRUE.equals(document.getImageOcrParsed()))
                .setEmbeddingCompleted(Boolean.TRUE.equals(document.getEmbeddingCompleted()))
                .setEnabled(!Boolean.FALSE.equals(document.getEnabled()))
                .setParsedAt(document.getParsedAt())
                .setProcessedAt(document.getProcessedAt());
        if (Objects.nonNull(latestTask)) {
            vo.setLatestTaskId(latestTask.getId())
                    .setLatestTaskStatus(latestTask.getTaskStatus())
                    .setLatestTaskStage(latestTask.getCurrentStage())
                    .setLatestTaskRetryCount(latestTask.getRetryCount())
                    .setLatestTaskErrorMessage(latestTask.getErrorMessage())
                    .setLatestTaskStartedAt(latestTask.getStartedAt())
                    .setLatestTaskFinishedAt(latestTask.getFinishedAt());
        }
        vo.copyBaseProperties(document);
        return vo;
    }

    private Map<String, KitKnowledgeIngestTask> groupLatestTasks(List<String> sourceDocumentIds) {
        if (CollectionUtils.isEmpty(sourceDocumentIds)) {
            return Collections.emptyMap();
        }
        List<KitKnowledgeIngestTask> tasks = ingestTaskMapper.selectList(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .in(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentIds)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .orderByDesc(KitKnowledgeIngestTask::getModifyTime)
                .orderByDesc(KitKnowledgeIngestTask::getCreateTime));
        Map<String, KitKnowledgeIngestTask> latestTaskMap = new LinkedHashMap<>();
        for (KitKnowledgeIngestTask task : tasks) {
            latestTaskMap.putIfAbsent(task.getSourceDocumentId(), task);
        }
        return latestTaskMap;
    }

    private void removeDerivedKnowledgeFiles(String imageFileIdsJson) {
        if (!StringUtils.hasText(imageFileIdsJson)) {
            return;
        }
        try {
            List<String> imageFileIds = GSON.fromJson(imageFileIdsJson, new TypeToken<List<String>>() {
            }.getType());
            if (CollectionUtils.isEmpty(imageFileIds)) {
                return;
            }
            imageFileIds.stream()
                    .filter(StringUtils::hasText)
                    .forEach(FileUtils::delete);
        } catch (Exception ignored) {
            // ignore invalid json and keep retry cleanup resilient
        }
    }

    private void purgeDocumentArtifacts(String documentId,
                                        boolean deleteTaskRecords,
                                        boolean deleteCheckpoints) {
        List<String> ingestTaskIds = ingestTaskMapper.selectIdsBySourceDocumentId(documentId);

        if (!CollectionUtils.isEmpty(ingestTaskIds) && deleteCheckpoints) {
            checkpointMapper.delete(new LambdaQueryWrapper<KitKnowledgeIngestAnalysisCheckpoint>()
                    .in(KitKnowledgeIngestAnalysisCheckpoint::getIngestTaskId, ingestTaskIds)
                    .eq(KitKnowledgeIngestAnalysisCheckpoint::getDeleted, false));
        }
        if (!CollectionUtils.isEmpty(ingestTaskIds) && deleteTaskRecords) {
            ingestTaskMapper.delete(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                    .in(KitKnowledgeIngestTask::getId, ingestTaskIds)
                    .eq(KitKnowledgeIngestTask::getDeleted, false));
        }

        sourceSegmentMapper.delete(new LambdaQueryWrapper<KitKnowledgeSourceSegment>()
                .eq(KitKnowledgeSourceSegment::getSourceDocumentId, documentId)
                .eq(KitKnowledgeSourceSegment::getDeleted, false));

        pageSyncService.removeSourceDocumentFromCurrentPages(documentId);
        chunkIndexRepository.deleteBySourceDocumentId(documentId);
    }
}

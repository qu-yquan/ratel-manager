package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestAnalysisCheckpoint;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestAnalysisCheckpointMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeIngestTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

/**
 * 知识文档导入任务服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestTaskServiceImpl
        extends ServiceImpl<KnowledgeIngestTaskMapper, KitKnowledgeIngestTask>
        implements IKnowledgeIngestTaskService {

    private static final List<KnowledgeIngestTaskStatus> ACTIVE_STATUSES = List.of(
            KnowledgeIngestTaskStatus.PENDING,
            KnowledgeIngestTaskStatus.RUNNING);

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeIngestAnalysisCheckpointMapper checkpointMapper;

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrResetTask(String sourceDocumentId, boolean incrementRetryCount) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        ensureNoActiveTask(sourceDocumentId);
        sourceDocumentService.purgeDocumentDerivedData(sourceDocumentId);
        KitKnowledgeIngestTask existingTask = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentId)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .orderByDesc(KitKnowledgeIngestTask::getModifyTime)
                .orderByDesc(KitKnowledgeIngestTask::getCreateTime)
                .last("limit 1"));
        if (Objects.isNull(existingTask)) {
            KitKnowledgeIngestTask task = new KitKnowledgeIngestTask()
                    .setSourceDocumentId(sourceDocumentId)
                    .setTaskStatus(KnowledgeIngestTaskStatus.PENDING)
                    .setRetryCount(0);
            save(task);
            return task.getId();
        }
        int nextRetryCount = Objects.requireNonNullElse(existingTask.getRetryCount(), 0);
        if (incrementRetryCount) {
            nextRetryCount += 1;
        }
        checkpointMapper.delete(new LambdaQueryWrapper<KitKnowledgeIngestAnalysisCheckpoint>()
                .eq(KitKnowledgeIngestAnalysisCheckpoint::getIngestTaskId, existingTask.getId())
                .eq(KitKnowledgeIngestAnalysisCheckpoint::getDeleted, false));
        update(new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, existingTask.getId())
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .set(KitKnowledgeIngestTask::getTaskStatus, KnowledgeIngestTaskStatus.PENDING)
                .set(KitKnowledgeIngestTask::getCurrentStage, null)
                .set(KitKnowledgeIngestTask::getRetryCount, nextRetryCount)
                .set(KitKnowledgeIngestTask::getErrorMessage, null)
                .set(KitKnowledgeIngestTask::getStartedAt, null)
                .set(KitKnowledgeIngestTask::getFinishedAt, null));
        sourceDocumentMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getDocumentStatus, KnowledgeDocumentStatus.UPLOADED)
                .set(KitKnowledgeSourceDocument::getProcessMessage, null)
                .set(KitKnowledgeSourceDocument::getImageOcrParsed, false)
                .set(KitKnowledgeSourceDocument::getParsedAt, null)
                .set(KitKnowledgeSourceDocument::getProcessedAt, null)
                .set(KitKnowledgeSourceDocument::getEmbeddingCompleted, false));
        return existingTask.getId();
    }

    @Override
    public void ensureNoActiveTask(String sourceDocumentId) {
        long activeCount = count(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentId)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .in(KitKnowledgeIngestTask::getTaskStatus, ACTIVE_STATUSES));
        if (activeCount > 0) {
            throw new BusinessException(KitErrorCode.E03004);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retry(String taskId) {
        KitKnowledgeIngestTask task = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(task)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        return createOrResetTask(task.getSourceDocumentId(), true);
    }
}

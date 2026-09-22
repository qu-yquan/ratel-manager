package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeIngestTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(String sourceDocumentId) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectByIdForUpdate(sourceDocumentId);
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        ensureNoActiveTask(sourceDocumentId);
        sourceDocumentService.purgeDocumentDerivedData(sourceDocumentId);
        KitKnowledgeIngestTask task = createPendingTask(sourceDocumentId, 0);
        activateTask(sourceDocumentId, task.getId());
        return task.getId();
    }

    private KitKnowledgeIngestTask createPendingTask(String sourceDocumentId, int retryCount) {
        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask()
                .setSourceDocumentId(sourceDocumentId)
                .setTaskStatus(KnowledgeIngestTaskStatus.PENDING)
                .setRetryCount(retryCount);
        save(task);
        return task;
    }

    private void activateTask(String sourceDocumentId, String taskId) {
        int updated = sourceDocumentMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getActiveTaskId, taskId)
                .set(KitKnowledgeSourceDocument::getDocumentStatus, KnowledgeDocumentStatus.UPLOADED)
                .set(KitKnowledgeSourceDocument::getProcessMessage, null)
                .set(KitKnowledgeSourceDocument::getImageOcrParsed, false)
                .set(KitKnowledgeSourceDocument::getParsedAt, null)
                .set(KitKnowledgeSourceDocument::getProcessedAt, null)
                .set(KitKnowledgeSourceDocument::getEmbeddingCompleted, false));
        if (updated != 1) {
            throw new BusinessException(KitErrorCode.E03001);
        }
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
        KitKnowledgeIngestTask requestedTask = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(requestedTask)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        String sourceDocumentId = requestedTask.getSourceDocumentId();
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectByIdForUpdate(sourceDocumentId);
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }

        KitKnowledgeIngestTask activeTask = StringUtils.hasText(sourceDocument.getActiveTaskId())
                ? getById(sourceDocument.getActiveTaskId())
                : null;
        int retryCount = Math.max(
                Objects.requireNonNullElse(requestedTask.getRetryCount(), 0),
                activeTask == null ? 0 : Objects.requireNonNullElse(activeTask.getRetryCount(), 0)) + 1;

        update(new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentId)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .in(KitKnowledgeIngestTask::getTaskStatus, ACTIVE_STATUSES)
                .set(KitKnowledgeIngestTask::getTaskStatus, KnowledgeIngestTaskStatus.SUPERSEDED)
                .set(KitKnowledgeIngestTask::getErrorMessage, "任务已被重新导入取代")
                .set(KitKnowledgeIngestTask::getFinishedAt, LocalDateTime.now()));

        KitKnowledgeIngestTask retryTask = createPendingTask(sourceDocumentId, retryCount);
        sourceDocumentService.purgeDocumentDerivedData(sourceDocumentId);
        activateTask(sourceDocumentId, retryTask.getId());
        return retryTask.getId();
    }
}

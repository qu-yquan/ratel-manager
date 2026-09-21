package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 在独立事务中落库导入失败状态，避免业务阶段的事务回滚清除失败结果。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeIngestFailureService {
    private static final int MESSAGE_MAX_LENGTH = 900;

    private final KnowledgeIngestTaskMapper taskMapper;
    private final KnowledgeSourceDocumentMapper documentMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public String markFailed(String taskId, Throwable cause) {
        KitKnowledgeIngestTask task = taskMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (task == null) throw new BusinessException("知识导入任务不存在，无法记录失败状态");

        String message = failureMessage(cause);
        LocalDateTime now = LocalDateTime.now();
        int taskUpdated = taskMapper.update(new KitKnowledgeIngestTask()
                        .setTaskStatus(KnowledgeIngestTaskStatus.FAILED)
                        .setErrorMessage(message)
                        .setFinishedAt(now), new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (taskUpdated != 1) throw new BusinessException("知识导入任务失败状态写入失败");

        int documentUpdated = documentMapper.update(new KitKnowledgeSourceDocument()
                        .setDocumentStatus(KnowledgeDocumentStatus.FAILED)
                        .setProcessMessage(message), new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, task.getSourceDocumentId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (documentUpdated != 1) {
            log.warn("知识文档已不存在，导入任务标记为失败, taskId={}, documentId={}", taskId, task.getSourceDocumentId());
        }
        return task.getSourceDocumentId();
    }

    public static String failureMessage(Throwable cause) {
        if (cause == null) return "知识文档处理失败，原因未知";
        StringBuilder message = new StringBuilder();
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        Throwable current = cause;
        while (current != null && visited.put(current, Boolean.TRUE) == null && message.length() < MESSAGE_MAX_LENGTH) {
            if (!message.isEmpty()) message.append("；原因：");
            message.append(current.getClass().getSimpleName());
            if (StringUtils.hasText(current.getMessage())) message.append("：").append(current.getMessage());
            current = current.getCause();
        }
        if (message.length() <= MESSAGE_MAX_LENGTH) return message.toString();
        return message.substring(0, MESSAGE_MAX_LENGTH - 1) + "…";
    }
}

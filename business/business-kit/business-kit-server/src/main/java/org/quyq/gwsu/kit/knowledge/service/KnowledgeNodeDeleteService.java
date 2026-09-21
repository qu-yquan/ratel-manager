package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeDirectoryRole;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDirectoryPermission;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeDirectoryRoleMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeNodeDeleteService {
    private static final int DELETE_BATCH_SIZE = 500;

    private final KnowledgeSourceDocumentMapper nodeMapper;
    private final KnowledgeDirectoryRoleMapper roleMapper;
    private final KnowledgeDirectoryService directoryService;
    private final IKnowledgeSourceDocumentService documentService;
    private final IKnowledgeIngestTaskService ingestTaskService;

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<String> ids) {
        if (CollectionUtils.isEmpty(ids) || ids.size() > 500 || ids.stream().anyMatch(id -> !StringUtils.hasText(id))) {
            throw new BusinessException("请选择要删除的目录或文档，单次最多500项");
        }
        Set<String> managers = directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE);
        Set<String> uploads = directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD);
        Map<String, KitKnowledgeSourceDocument> targets = new LinkedHashMap<>();
        for (String id : ids) {
            KitKnowledgeSourceDocument node = nodeMapper.selectById(id);
            if (node == null || Boolean.TRUE.equals(node.getDeleted())
                    || (KnowledgeDirectoryService.DIRECTORY.equals(node.getNodeType())
                        ? !directoryService.canRead(node, managers)
                        : !directoryService.canEditDocument(node, managers, uploads))) {
                throw new BusinessException("知识目录或文档不存在，或无删除权限");
            }
            targets.put(id, node);
        }

        Map<String, KitKnowledgeSourceDocument> all = new LinkedHashMap<>(targets);
        for (KitKnowledgeSourceDocument node : targets.values()) {
            if (!KnowledgeDirectoryService.DIRECTORY.equals(node.getNodeType())) continue;
            List<KitKnowledgeSourceDocument> descendants = nodeMapper.selectList(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                    .likeRight(KitKnowledgeSourceDocument::getDirectoryPath, node.getDirectoryPath())
                    .eq(KitKnowledgeSourceDocument::getDeleted, false));
            descendants.forEach(child -> all.put(child.getId(), child));
        }

        List<String> directoryIds = all.values().stream()
                .filter(node -> KnowledgeDirectoryService.DIRECTORY.equals(node.getNodeType()))
                .map(KitKnowledgeSourceDocument::getId).toList();
        List<String> documentIds = all.values().stream()
                .filter(node -> KnowledgeDirectoryService.DOCUMENT.equals(node.getNodeType()))
                .map(KitKnowledgeSourceDocument::getId).toList();

        // 在任何删除动作前检查整批文档，避免导入任务继续写回已删除节点。
        documentIds.forEach(ingestTaskService::ensureNoActiveTask);
        documentIds.forEach(documentService::deleteDocumentData);
        for (int start = 0; start < directoryIds.size(); start += DELETE_BATCH_SIZE) {
            List<String> batch = directoryIds.subList(start, Math.min(start + DELETE_BATCH_SIZE, directoryIds.size()));
            roleMapper.delete(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                    .in(KitKnowledgeDirectoryRole::getDirectoryId, batch)
                    .eq(KitKnowledgeDirectoryRole::getDeleted, false));
        }
        deleteNodes(all.keySet());
    }

    private void deleteNodes(Collection<String> ids) {
        List<String> nodeIds = List.copyOf(ids);
        for (int start = 0; start < nodeIds.size(); start += DELETE_BATCH_SIZE) {
            List<String> batch = nodeIds.subList(start, Math.min(start + DELETE_BATCH_SIZE, nodeIds.size()));
            nodeMapper.delete(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                    .in(KitKnowledgeSourceDocument::getId, batch)
                    .eq(KitKnowledgeSourceDocument::getDeleted, false));
        }
    }
}

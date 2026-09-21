package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDirectorySaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeRoleScopeSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeNodeVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeDirectoryRole;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeDirectoryRoleMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeDirectoryService {
    public static final String DIRECTORY = "DIRECTORY";
    public static final String DOCUMENT = "DOCUMENT";
    public static final String ROOT_DIRECTORY_ID = "ROOT";

    private final KnowledgeSourceDocumentMapper nodeMapper;
    private final KnowledgeDirectoryRoleMapper roleMapper;
    private final SecurityUtils securityUtils;

    public KitKnowledgeSourceDocument requireDirectory(String id) {
        KitKnowledgeSourceDocument directory = nodeMapper.selectById(id);
        if (directory == null || Boolean.TRUE.equals(directory.getDeleted()) || !DIRECTORY.equals(directory.getNodeType())) {
            throw new BusinessException("知识目录不存在");
        }
        return directory;
    }

    public KitKnowledgeSourceDocument requireReadableDocument(String id) {
        KitKnowledgeSourceDocument document = nodeMapper.selectById(id);
        if (document == null || Boolean.TRUE.equals(document.getDeleted()) || !DOCUMENT.equals(document.getNodeType())
                || !canRead(document, grantedDirectoryIds())) {
            throw new BusinessException("知识文档不存在或无访问权限");
        }
        return document;
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveDirectory(KnowledgeDirectorySaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName()) || dto.getName().trim().length() > 200) {
            throw new BusinessException("目录名称不能为空且长度不能超过200");
        }
        String name = dto.getName().trim();
        if (StringUtils.hasText(dto.getId())) {
            KitKnowledgeSourceDocument old = requireDirectory(dto.getId());
            if (!canRead(old, grantedDirectoryIds())) throw new BusinessException("没有目录的访问权限");
            assertUnique(old.getParentId(), name, old.getId());
            nodeMapper.updateById(new KitKnowledgeSourceDocument().setId(old.getId()).setName(name));
            return old.getId();
        }
        KitKnowledgeSourceDocument parent = StringUtils.hasText(dto.getParentId()) ? requireDirectory(dto.getParentId()) : null;
        if (parent == null && !canReadRoot()) throw new BusinessException("没有根目录的访问权限");
        if (parent != null && !canRead(parent, grantedDirectoryIds())) {
            throw new BusinessException("没有父目录的访问权限");
        }
        assertUnique(dto.getParentId(), name, null);
        KitKnowledgeSourceDocument directory = new KitKnowledgeSourceDocument()
                .setParentId(parent == null ? null : parent.getId())
                .setNodeType(DIRECTORY)
                .setName(name)
                .setSortNo(0)
                .setDirectoryPath("")
                .setEnabled(true);
        nodeMapper.insert(directory);
        directory.setDirectoryPath((parent == null ? "/" : parent.getDirectoryPath()) + directory.getId() + "/");
        if (directory.getDirectoryPath().length() > 2000) throw new BusinessException("目录层级过深");
        nodeMapper.updateById(directory);
        return directory.getId();
    }

    private void assertUnique(String parentId, String name, String exceptId) {
        LambdaQueryWrapper<KitKnowledgeSourceDocument> query = new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getNodeType, DIRECTORY)
                .eq(KitKnowledgeSourceDocument::getName, name)
                .eq(KitKnowledgeSourceDocument::getDeleted, false);
        if (StringUtils.hasText(parentId)) query.eq(KitKnowledgeSourceDocument::getParentId, parentId);
        else query.isNull(KitKnowledgeSourceDocument::getParentId);
        if (StringUtils.hasText(exceptId)) query.ne(KitKnowledgeSourceDocument::getId, exceptId);
        if (nodeMapper.selectCount(query) > 0) throw new BusinessException("同级目录名称已存在");
    }

    public List<KnowledgeNodeVO> tree(boolean management) {
        List<KitKnowledgeSourceDocument> directories = nodeMapper.selectList(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getNodeType, DIRECTORY)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .orderByAsc(KitKnowledgeSourceDocument::getSortNo)
                .orderByAsc(KitKnowledgeSourceDocument::getName));
        Set<String> grants = management ? Set.of("*") : grantedDirectoryIds();
        Set<String> visible = new HashSet<>();
        if (management || grants.contains("*")) {
            directories.forEach(directory -> visible.add(directory.getId()));
        } else {
            for (KitKnowledgeSourceDocument directory : directories) {
                if (grants.contains(directory.getId())) visible.addAll(pathIds(directory.getDirectoryPath()));
            }
            directories.stream().filter(directory -> canRead(directory, grants))
                    .forEach(directory -> visible.add(directory.getId()));
        }
        Map<String, KitKnowledgeSourceDocument> directoryById = new HashMap<>();
        directories.forEach(directory -> directoryById.put(directory.getId(), directory));
        Map<String, Long> counts = new HashMap<>();
        if (!visible.isEmpty()) {
            for (Map<String, Object> row : nodeMapper.countDocumentsByParent()) {
                Object parentId = row.get("parent_id");
                if (parentId == null) continue;
                KitKnowledgeSourceDocument parent = directoryById.get(String.valueOf(parentId));
                if (parent == null || (!management && !canRead(parent, grants))) continue;
                long directCount = ((Number) row.get("document_count")).longValue();
                for (KitKnowledgeSourceDocument cursor = parent; cursor != null;
                     cursor = directoryById.get(cursor.getParentId())) {
                    counts.merge(cursor.getId(), directCount, Long::sum);
                }
            }
        }
        return directories.stream().filter(directory -> visible.contains(directory.getId()))
                .map(directory -> toVO(directory).setDocumentCount(counts.getOrDefault(directory.getId(), 0L)))
                .toList();
    }

    public IPage<KnowledgeNodeVO> searchDocuments(String name, long pageNum, long pageSize) {
        if (!StringUtils.hasText(name)) throw new BusinessException("请输入文档名称");
        if (name.trim().length() > 200 || pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BusinessException("文档搜索条件不正确");
        }
        Set<String> grants = grantedDirectoryIds();
        Page<KnowledgeNodeVO> empty = Page.of(pageNum, pageSize, 0);
        empty.setRecords(List.of());
        if (grants.isEmpty()) return empty;
        boolean allDirectories = grants.contains("*");
        List<String> roles = allDirectories ? List.of() : securityUtils.checkSubject().getRoles();
        if (!allDirectories && roles.isEmpty()) return empty;
        IPage<KitKnowledgeSourceDocument> page = nodeMapper.searchAccessibleDocuments(
                Page.of(pageNum, pageSize), name.trim(), allDirectories, roles);
        Page<KnowledgeNodeVO> result = Page.of(pageNum, pageSize, page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public IPage<KnowledgeNodeVO> children(String parentId, String name, long pageNum, long pageSize) {
        KitKnowledgeSourceDocument parent = StringUtils.hasText(parentId) ? requireDirectory(parentId) : null;
        Set<String> grants = grantedDirectoryIds();
        boolean full = grants.contains("*") || (parent != null && canRead(parent, grants));
        LambdaQueryWrapper<KitKnowledgeSourceDocument> query = new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .orderByAsc(KitKnowledgeSourceDocument::getNodeType)
                .orderByAsc(KitKnowledgeSourceDocument::getSortNo)
                .orderByAsc(KitKnowledgeSourceDocument::getName);
        if (parent == null) query.isNull(KitKnowledgeSourceDocument::getParentId);
        else query.eq(KitKnowledgeSourceDocument::getParentId, parentId);
        if (StringUtils.hasText(name)) query.like(KitKnowledgeSourceDocument::getName, name.trim());
        if (full) {
            IPage<KitKnowledgeSourceDocument> page = nodeMapper.selectPage(Page.of(pageNum, pageSize), query);
            Page<KnowledgeNodeVO> result = Page.of(pageNum, pageSize, page.getTotal());
            result.setRecords(page.getRecords().stream().map(this::toVO).toList());
            return result;
        }
        query.eq(KitKnowledgeSourceDocument::getNodeType, DIRECTORY);
        List<KitKnowledgeSourceDocument> candidates = nodeMapper.selectList(query);
        Set<String> visibleAncestorIds = new HashSet<>();
        if (!grants.isEmpty()) {
            List<KitKnowledgeSourceDocument> granted = nodeMapper.selectBatchIds(grants);
            granted.forEach(item -> visibleAncestorIds.addAll(pathIds(item.getDirectoryPath())));
        }
        List<KnowledgeNodeVO> visible = candidates.stream()
                .filter(d -> visibleAncestorIds.contains(d.getId()))
                .map(this::toVO).toList();
        long from = Math.min(visible.size(), Math.max(0, pageNum - 1) * pageSize);
        long to = Math.min(visible.size(), from + pageSize);
        Page<KnowledgeNodeVO> result = Page.of(pageNum, pageSize, visible.size());
        result.setRecords(visible.subList((int) from, (int) to));
        return result;
    }

    public List<String> grantsForRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) return List.of();
        return roleMapper.selectList(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .eq(KitKnowledgeDirectoryRole::getRoleCode, roleCode)
                .eq(KitKnowledgeDirectoryRole::getDeleted, false)).stream()
                .map(KitKnowledgeDirectoryRole::getDirectoryId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRoleScope(KnowledgeRoleScopeSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getRoleCode())) throw new BusinessException("角色编码不能为空");
        Set<String> ids = new HashSet<>(Objects.requireNonNullElse(dto.getDirectoryIds(), List.of()));
        for (String id : ids) if (!ROOT_DIRECTORY_ID.equals(id)) requireDirectory(id);
        roleMapper.delete(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .eq(KitKnowledgeDirectoryRole::getRoleCode, dto.getRoleCode())
                .eq(KitKnowledgeDirectoryRole::getDeleted, false));
        for (String id : ids) roleMapper.insert(new KitKnowledgeDirectoryRole().setRoleCode(dto.getRoleCode()).setDirectoryId(id));
    }

    public Set<String> grantedDirectoryIds() {
        List<String> roles = securityUtils.checkSubject().getRoles();
        if (roles.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG)) return Set.of("*");
        return grantedDirectoryIds(roles);
    }

    public Set<String> grantedDirectoryIds(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();
        if (roles.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG)) return Set.of("*");
        List<KitKnowledgeDirectoryRole> grants = roleMapper.selectList(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .in(KitKnowledgeDirectoryRole::getRoleCode, roles)
                .eq(KitKnowledgeDirectoryRole::getDeleted, false));
        Set<String> ids = new HashSet<>();
        grants.forEach(grant -> ids.add(grant.getDirectoryId()));
        if (ids.contains(ROOT_DIRECTORY_ID)) return Set.of("*");
        return ids;
    }

    public boolean canReadRoot() {
        return grantedDirectoryIds().contains("*");
    }

    public boolean canRead(KitKnowledgeSourceDocument node, Collection<String> grants) {
        if (node == null || grants == null || grants.isEmpty()) return false;
        if (grants.contains("*")) return true;
        return pathIds(node.getDirectoryPath()).stream().anyMatch(grants::contains);
    }

    public static List<String> pathIds(String path) {
        if (!StringUtils.hasText(path)) return List.of();
        List<String> ids = new ArrayList<>();
        for (String part : path.split("/")) if (StringUtils.hasText(part)) ids.add(part);
        return ids;
    }

    public KnowledgeNodeVO toVO(KitKnowledgeSourceDocument node) {
        KnowledgeNodeVO vo = new KnowledgeNodeVO().setId(node.getId()).setParentId(node.getParentId())
                .setNodeType(node.getNodeType()).setName(node.getName()).setSortNo(node.getSortNo())
                .setFileId(node.getFileId()).setFileName(node.getFileName()).setFileSize(node.getFileSize())
                .setFileFormat(node.getFileFormat()).setDocumentStatus(node.getDocumentStatus())
                .setEnabled(node.getEnabled()).setEmbeddingCompleted(node.getEmbeddingCompleted())
                .setImageOcrParsed(node.getImageOcrParsed()).setParsedAt(node.getParsedAt())
                .setProcessedAt(node.getProcessedAt())
                .setProcessMessage(node.getProcessMessage());
        vo.copyBaseProperties(node);
        return vo;
    }
}

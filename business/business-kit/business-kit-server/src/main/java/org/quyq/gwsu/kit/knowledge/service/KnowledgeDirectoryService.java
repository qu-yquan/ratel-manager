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
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDirectoryPermission;
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

    public KitKnowledgeSourceDocument requireEditableDocument(String id) {
        KitKnowledgeSourceDocument document = requireReadableDocument(id);
        if (!canEditDocument(document)) throw new BusinessException("没有文档的编辑权限");
        return document;
    }

    public boolean canEditDocument(KitKnowledgeSourceDocument document) {
        return canEditDocument(document, grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE),
                grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD));
    }

    public boolean canEditDocument(KitKnowledgeSourceDocument document, Collection<String> managers,
                                   Collection<String> uploads) {
        if (canRead(document, managers)) return true;
        String username = securityUtils.getUsername();
        return StringUtils.hasText(username) && Objects.equals(username, document.getCreateOp())
                && canRead(document, uploads);
    }

    public boolean canManage(KitKnowledgeSourceDocument directory) {
        return canRead(directory, grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE));
    }

    public boolean canUpload(KitKnowledgeSourceDocument directory) {
        return canRead(directory, grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD));
    }

    public boolean canManageRoot() {
        return grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE).contains("*");
    }

    public boolean canUploadRoot() {
        return grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD).contains("*");
    }

    @Transactional(rollbackFor = Exception.class)
    public String saveDirectory(KnowledgeDirectorySaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName()) || dto.getName().trim().length() > 200) {
            throw new BusinessException("目录名称不能为空且长度不能超过200");
        }
        String name = dto.getName().trim();
        if (StringUtils.hasText(dto.getId())) {
            KitKnowledgeSourceDocument old = requireDirectory(dto.getId());
            if (!canManage(old)) throw new BusinessException("没有目录的管理权限");
            assertUnique(old.getParentId(), name, old.getId());
            nodeMapper.updateById(new KitKnowledgeSourceDocument().setId(old.getId()).setName(name));
            return old.getId();
        }
        KitKnowledgeSourceDocument parent = StringUtils.hasText(dto.getParentId()) ? requireDirectory(dto.getParentId()) : null;
        if (parent == null && !canManageRoot()) throw new BusinessException("没有根目录的管理权限");
        if (parent != null && !canManage(parent)) {
            throw new BusinessException("没有父目录的管理权限");
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
        Set<String> uploads = management ? Set.of() : grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD);
        Set<String> managers = management ? Set.of() : grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE);
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
                .map(directory -> toVO(directory, grants, uploads, managers).setDocumentCount(counts.getOrDefault(directory.getId(), 0L)))
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
        Set<String> uploads = grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD);
        Set<String> managers = grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE);
        result.setRecords(page.getRecords().stream().map(node -> toVO(node, grants, uploads, managers)).toList());
        return result;
    }

    public IPage<KnowledgeNodeVO> children(String parentId, String name, long pageNum, long pageSize) {
        KitKnowledgeSourceDocument parent = StringUtils.hasText(parentId) ? requireDirectory(parentId) : null;
        Set<String> grants = grantedDirectoryIds();
        Set<String> uploads = grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD);
        Set<String> managers = grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE);
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
            result.setRecords(page.getRecords().stream().map(node -> toVO(node, grants, uploads, managers)).toList());
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
                .map(node -> toVO(node, grants, uploads, managers)).toList();
        long from = Math.min(visible.size(), Math.max(0, pageNum - 1) * pageSize);
        long to = Math.min(visible.size(), from + pageSize);
        Page<KnowledgeNodeVO> result = Page.of(pageNum, pageSize, visible.size());
        result.setRecords(visible.subList((int) from, (int) to));
        return result;
    }

    public List<KnowledgeRoleScopeSaveDTO.Grant> grantsForRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) return List.of();
        return roleMapper.selectList(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .eq(KitKnowledgeDirectoryRole::getRoleCode, roleCode)
                .eq(KitKnowledgeDirectoryRole::getDeleted, false)).stream()
                .map(row -> {
                    var grant = new KnowledgeRoleScopeSaveDTO.Grant();
                    grant.setDirectoryId(row.getDirectoryId());
                    grant.setPermissionType(row.getPermissionType());
                    return grant;
                }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRoleScope(KnowledgeRoleScopeSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getRoleCode())) throw new BusinessException("角色编码不能为空");
        List<KnowledgeRoleScopeSaveDTO.Grant> grants = Objects.requireNonNullElse(dto.getGrants(), List.of());
        Set<String> keys = new HashSet<>();
        for (var grant : grants) {
            if (grant == null || !StringUtils.hasText(grant.getDirectoryId()) || grant.getPermissionType() == null)
                throw new BusinessException("目录权限配置不正确");
            if (!ROOT_DIRECTORY_ID.equals(grant.getDirectoryId())) requireDirectory(grant.getDirectoryId());
            if (!keys.add(grant.getDirectoryId() + ":" + grant.getPermissionType()))
                throw new BusinessException("目录权限重复");
        }
        roleMapper.delete(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .eq(KitKnowledgeDirectoryRole::getRoleCode, dto.getRoleCode())
                .eq(KitKnowledgeDirectoryRole::getDeleted, false));
        for (var grant : grants) roleMapper.insert(new KitKnowledgeDirectoryRole()
                .setRoleCode(dto.getRoleCode()).setDirectoryId(grant.getDirectoryId())
                .setPermissionType(grant.getPermissionType()));
    }

    public Set<String> grantedDirectoryIds() {
        return grantedDirectoryIds(KnowledgeDirectoryPermission.SEARCH);
    }

    public Set<String> grantedDirectoryIds(KnowledgeDirectoryPermission required) {
        List<String> roles = securityUtils.checkSubject().getRoles();
        if (roles.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG)) return Set.of("*");
        return grantedDirectoryIds(roles, required);
    }

    public Set<String> grantedDirectoryIds(Collection<String> roles) {
        return grantedDirectoryIds(roles, KnowledgeDirectoryPermission.SEARCH);
    }

    public Set<String> grantedDirectoryIds(Collection<String> roles, KnowledgeDirectoryPermission required) {
        if (roles == null || roles.isEmpty()) return Set.of();
        if (roles.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG)) return Set.of("*");
        List<KitKnowledgeDirectoryRole> grants = roleMapper.selectList(new LambdaQueryWrapper<KitKnowledgeDirectoryRole>()
                .in(KitKnowledgeDirectoryRole::getRoleCode, roles)
                .eq(KitKnowledgeDirectoryRole::getDeleted, false));
        Set<String> ids = new HashSet<>();
        grants.stream().filter(grant -> grant.getPermissionType() != null && grant.getPermissionType().allows(required))
                .forEach(grant -> ids.add(grant.getDirectoryId()));
        if (ids.contains(ROOT_DIRECTORY_ID)) return Set.of("*");
        return ids;
    }

    public boolean canReadRoot() {
        return grantedDirectoryIds().contains("*");
    }

    public boolean isAdministrator() {
        return securityUtils.checkSubject().isAdmin();
    }

    public KnowledgeNodeVO rootCapabilities() {
        boolean search = canReadRoot();
        boolean upload = canUploadRoot();
        boolean manage = canManageRoot();
        return new KnowledgeNodeVO().setId(ROOT_DIRECTORY_ID).setNodeType(DIRECTORY)
                .setName("全部目录").setCanSearch(search).setCanUpload(upload)
                .setCanManage(manage).setCanEdit(manage).setCanDelete(false);
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
        return toVO(node, grantedDirectoryIds(), grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD),
                grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE));
    }

    private KnowledgeNodeVO toVO(KitKnowledgeSourceDocument node, Set<String> searches,
                                 Set<String> uploads, Set<String> managers) {
        KnowledgeNodeVO vo = new KnowledgeNodeVO().setId(node.getId()).setParentId(node.getParentId())
                .setNodeType(node.getNodeType()).setName(node.getName()).setSortNo(node.getSortNo())
                .setFileId(node.getFileId()).setFileName(node.getFileName()).setFileSize(node.getFileSize())
                .setFileFormat(node.getFileFormat()).setDocumentStatus(node.getDocumentStatus())
                .setEnabled(node.getEnabled()).setEmbeddingCompleted(node.getEmbeddingCompleted())
                .setImageOcrParsed(node.getImageOcrParsed()).setParsedAt(node.getParsedAt())
                .setProcessedAt(node.getProcessedAt())
                .setProcessMessage(node.getProcessMessage());
        vo.copyBaseProperties(node);
        boolean manage = canRead(node, managers);
        boolean upload = canRead(node, uploads);
        boolean edit = manage || (DOCUMENT.equals(node.getNodeType()) && upload
                && StringUtils.hasText(securityUtils.getUsername())
                && Objects.equals(securityUtils.getUsername(), node.getCreateOp()));
        vo.setCanSearch(canRead(node, searches)).setCanUpload(upload).setCanManage(manage)
                .setCanEdit(edit).setCanDelete(DIRECTORY.equals(node.getNodeType()) ? manage : edit);
        return vo;
    }
}

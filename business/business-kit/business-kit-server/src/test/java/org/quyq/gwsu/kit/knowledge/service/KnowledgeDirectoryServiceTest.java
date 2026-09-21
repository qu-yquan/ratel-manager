package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeDirectoryRole;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDirectoryPermission;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeDirectoryRoleMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.domain.Subject;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class KnowledgeDirectoryServiceTest {
    private final KnowledgeDirectoryService service = new KnowledgeDirectoryService(
            mock(KnowledgeSourceDocumentMapper.class), mock(KnowledgeDirectoryRoleMapper.class), mock(SecurityUtils.class));

    @Test
    void documentWithoutGrantedDirectoryIsDenied() {
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument()
                .setDirectoryPath("/root/team/").setNodeType(KnowledgeDirectoryService.DOCUMENT);
        assertFalse(service.canRead(document, Set.of()));
        assertFalse(service.canRead(document, Set.of("other")));
    }

    @Test
    void parentGrantCoversDescendantsButNotSiblings() {
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument()
                .setDirectoryPath("/root/team/").setNodeType(KnowledgeDirectoryService.DOCUMENT);
        assertTrue(service.canRead(document, Set.of("root")));
        assertTrue(service.canRead(document, Set.of("team")));
        assertFalse(service.canRead(document, Set.of("teams")));
        assertTrue(KnowledgeDirectoryService.pathIds(document.getDirectoryPath()).equals(List.of("root", "team")));
    }

    @Test
    void rootGrantCoversRootDocumentsAndAllDescendants() {
        KnowledgeDirectoryRoleMapper roleMapper = mock(KnowledgeDirectoryRoleMapper.class);
        KnowledgeDirectoryService directoryService = new KnowledgeDirectoryService(
                mock(KnowledgeSourceDocumentMapper.class), roleMapper, mock(SecurityUtils.class));
        when(roleMapper.selectList(any())).thenReturn(List.of(new KitKnowledgeDirectoryRole()
                .setDirectoryId(KnowledgeDirectoryService.ROOT_DIRECTORY_ID).setRoleCode("editor")
                .setPermissionType(KnowledgeDirectoryPermission.SEARCH)));

        Set<String> grants = directoryService.grantedDirectoryIds(List.of("editor"));
        assertTrue(grants.contains("*"));
        assertTrue(directoryService.canRead(new KitKnowledgeSourceDocument().setDirectoryPath("")
                .setNodeType(KnowledgeDirectoryService.DOCUMENT), grants));
        assertTrue(directoryService.canRead(new KitKnowledgeSourceDocument().setDirectoryPath("/team/")
                .setNodeType(KnowledgeDirectoryService.DOCUMENT), grants));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void permissionLevelsAndUploaderOwnershipAreEnforced() {
        KnowledgeDirectoryRoleMapper roleMapper = mock(KnowledgeDirectoryRoleMapper.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        Subject subject = mock(Subject.class);
        when(subject.getRoles()).thenReturn(List.of("editor"));
        when(securityUtils.checkSubject()).thenReturn(subject);
        when(securityUtils.getUsername()).thenReturn("alice");
        when(roleMapper.selectList(any())).thenReturn(List.of(
                new KitKnowledgeDirectoryRole().setDirectoryId("team").setRoleCode("editor")
                        .setPermissionType(KnowledgeDirectoryPermission.UPLOAD),
                new KitKnowledgeDirectoryRole().setDirectoryId("managed").setRoleCode("editor")
                        .setPermissionType(KnowledgeDirectoryPermission.MANAGE)));
        KnowledgeDirectoryService directoryService = new KnowledgeDirectoryService(
                mock(KnowledgeSourceDocumentMapper.class), roleMapper, securityUtils);

        assertEquals(Set.of("team", "managed"), directoryService.grantedDirectoryIds());
        assertEquals(Set.of("team", "managed"), directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD));
        assertEquals(Set.of("managed"), directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE));
        KitKnowledgeSourceDocument own = new KitKnowledgeSourceDocument().setNodeType("DOCUMENT").setDirectoryPath("/team/");
        own.setCreateOp("alice");
        KitKnowledgeSourceDocument other = new KitKnowledgeSourceDocument().setNodeType("DOCUMENT").setDirectoryPath("/team/");
        other.setCreateOp("bob");
        KitKnowledgeSourceDocument managed = new KitKnowledgeSourceDocument().setNodeType("DOCUMENT").setDirectoryPath("/managed/");
        managed.setCreateOp("bob");
        assertTrue(directoryService.canEditDocument(own));
        assertFalse(directoryService.canEditDocument(other));
        assertTrue(directoryService.canEditDocument(managed));
    }

    @Test
    void treeCountsDocumentsInDescendantDirectories() {
        KnowledgeSourceDocumentMapper nodeMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeDirectoryService directoryService = new KnowledgeDirectoryService(
                nodeMapper, mock(KnowledgeDirectoryRoleMapper.class), mock(SecurityUtils.class));
        when(nodeMapper.selectList(any())).thenReturn(List.of(
                new KitKnowledgeSourceDocument().setId("root").setNodeType("DIRECTORY").setDirectoryPath("/root/"),
                new KitKnowledgeSourceDocument().setId("child").setParentId("root")
                        .setNodeType("DIRECTORY").setDirectoryPath("/root/child/")));
        when(nodeMapper.countDocumentsByParent()).thenReturn(List.of(
                Map.of("parent_id", "root", "document_count", 1L),
                Map.of("parent_id", "child", "document_count", 2L)));

        var tree = directoryService.tree(true);

        assertEquals(3L, tree.get(0).getDocumentCount());
        assertEquals(2L, tree.get(1).getDocumentCount());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void visibleAncestorCountExcludesUnauthorizedDocuments() {
        KnowledgeSourceDocumentMapper nodeMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeDirectoryRoleMapper roleMapper = mock(KnowledgeDirectoryRoleMapper.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        Subject subject = mock(Subject.class);
        when(subject.getRoles()).thenReturn(List.of("reader"));
        when(securityUtils.checkSubject()).thenReturn(subject);
        when(roleMapper.selectList(any())).thenReturn(List.of(new KitKnowledgeDirectoryRole()
                .setDirectoryId("child").setRoleCode("reader")
                .setPermissionType(KnowledgeDirectoryPermission.SEARCH)));
        when(nodeMapper.selectList(any())).thenReturn(List.of(
                new KitKnowledgeSourceDocument().setId("root").setNodeType("DIRECTORY").setDirectoryPath("/root/"),
                new KitKnowledgeSourceDocument().setId("child").setParentId("root")
                        .setNodeType("DIRECTORY").setDirectoryPath("/root/child/"),
                new KitKnowledgeSourceDocument().setId("sibling").setParentId("root")
                        .setNodeType("DIRECTORY").setDirectoryPath("/root/sibling/")));
        when(nodeMapper.countDocumentsByParent()).thenReturn(List.of(
                Map.of("parent_id", "root", "document_count", 5L),
                Map.of("parent_id", "child", "document_count", 2L),
                Map.of("parent_id", "sibling", "document_count", 3L)));

        var tree = new KnowledgeDirectoryService(nodeMapper, roleMapper, securityUtils).tree(false);

        assertEquals(List.of("root", "child"), tree.stream().map(item -> item.getId()).toList());
        assertEquals(2L, tree.get(0).getDocumentCount());
        assertEquals(2L, tree.get(1).getDocumentCount());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void globalDocumentSearchUsesCurrentRoleScope() {
        KnowledgeSourceDocumentMapper nodeMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeDirectoryRoleMapper roleMapper = mock(KnowledgeDirectoryRoleMapper.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        Subject subject = mock(Subject.class);
        when(subject.getRoles()).thenReturn(List.of("reader"));
        when(securityUtils.checkSubject()).thenReturn(subject);
        when(roleMapper.selectList(any())).thenReturn(List.of(new KitKnowledgeDirectoryRole()
                .setDirectoryId("child").setRoleCode("reader")
                .setPermissionType(KnowledgeDirectoryPermission.SEARCH)));
        Page<KitKnowledgeSourceDocument> found = Page.of(1, 20, 1);
        found.setRecords(List.of(new KitKnowledgeSourceDocument().setId("document")
                .setNodeType("DOCUMENT").setName("财务文档")));
        when(nodeMapper.searchAccessibleDocuments(any(Page.class), eq("财务"), eq(false), eq(List.of("reader"))))
                .thenReturn(found);

        var results = new KnowledgeDirectoryService(nodeMapper, roleMapper, securityUtils)
                .searchDocuments(" 财务 ", 1, 20);

        assertEquals(1, results.getTotal());
        assertEquals("document", results.getRecords().getFirst().getId());
        verify(nodeMapper).searchAccessibleDocuments(any(Page.class), eq("财务"), eq(false), eq(List.of("reader")));
    }
}

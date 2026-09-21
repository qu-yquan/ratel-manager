package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDirectoryPermission;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeDirectoryRoleMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeNodeDeleteServiceTest {
    private final KnowledgeSourceDocumentMapper nodeMapper = mock(KnowledgeSourceDocumentMapper.class);
    private final KnowledgeDirectoryRoleMapper roleMapper = mock(KnowledgeDirectoryRoleMapper.class);
    private final KnowledgeDirectoryService directoryService = mock(KnowledgeDirectoryService.class);
    private final IKnowledgeSourceDocumentService documentService = mock(IKnowledgeSourceDocumentService.class);
    private final IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
    private final KnowledgeNodeDeleteService service = new KnowledgeNodeDeleteService(
            nodeMapper, roleMapper, directoryService, documentService, ingestTaskService);

    @Test
    void deletingDirectoryAlsoCleansNestedDocuments() {
        KitKnowledgeSourceDocument directory = node("directory", "DIRECTORY", "/directory/");
        KitKnowledgeSourceDocument nested = node("nested", "DIRECTORY", "/directory/nested/");
        KitKnowledgeSourceDocument document = node("document", "DOCUMENT", "/directory/nested/");
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE)).thenReturn(Set.of("directory"));
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD)).thenReturn(Set.of("directory"));
        when(directoryService.canRead(directory, Set.of("directory"))).thenReturn(true);
        when(nodeMapper.selectById("directory")).thenReturn(directory);
        when(nodeMapper.selectList(any())).thenReturn(List.of(nested, document));

        service.delete(List.of("directory"));

        verify(ingestTaskService).ensureNoActiveTask("document");
        verify(documentService).deleteDocumentData("document");
        verify(roleMapper).delete(any());
        verify(nodeMapper).delete(any());
    }

    @Test
    void inaccessibleDirectoryCannotBeDeleted() {
        KitKnowledgeSourceDocument directory = node("directory", "DIRECTORY", "/directory/");
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE)).thenReturn(Set.of());
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD)).thenReturn(Set.of());
        when(nodeMapper.selectById("directory")).thenReturn(directory);

        assertThrows(BusinessException.class, () -> service.delete(List.of("directory")));

        verify(nodeMapper, never()).selectList(any());
        verify(nodeMapper, never()).delete(any());
    }

    @Test
    void activeImportStopsWholeDeleteBeforeCleanup() {
        KitKnowledgeSourceDocument directory = node("directory", "DIRECTORY", "/directory/");
        KitKnowledgeSourceDocument document = node("document", "DOCUMENT", "/directory/");
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.MANAGE)).thenReturn(Set.of("directory"));
        when(directoryService.grantedDirectoryIds(KnowledgeDirectoryPermission.UPLOAD)).thenReturn(Set.of("directory"));
        when(directoryService.canRead(directory, Set.of("directory"))).thenReturn(true);
        when(nodeMapper.selectById("directory")).thenReturn(directory);
        when(nodeMapper.selectList(any())).thenReturn(List.of(document));
        doThrow(new BusinessException("正在导入")).when(ingestTaskService).ensureNoActiveTask("document");

        assertThrows(BusinessException.class, () -> service.delete(List.of("directory")));

        verifyNoInteractions(documentService);
        verify(nodeMapper, never()).delete(any());
        verify(roleMapper, never()).delete(any());
    }

    private KitKnowledgeSourceDocument node(String id, String type, String path) {
        return new KitKnowledgeSourceDocument().setId(id).setNodeType(type).setDirectoryPath(path);
    }
}

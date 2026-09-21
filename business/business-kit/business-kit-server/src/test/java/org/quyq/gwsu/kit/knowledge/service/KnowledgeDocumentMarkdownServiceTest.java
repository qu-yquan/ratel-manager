package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeContentRenderService;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPagePublishService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentMarkdownServiceTest {

    @Test
    void rendersKnowledgeImageMarkersBeforeReturningMarkdown() {
        KnowledgeDirectoryService directoryService = mock(KnowledgeDirectoryService.class);
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper versionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgeContentRenderService contentRenderService = mock(KnowledgeContentRenderService.class);
        KnowledgeDocumentMarkdownService service = new KnowledgeDocumentMarkdownService(
                directoryService,
                pageMapper,
                versionMapper,
                mock(KnowledgePageBlockMapper.class),
                mock(KnowledgePageSourceRefMapper.class),
                mock(KnowledgeSourceDocumentMapper.class),
                mock(KnowledgeSourceDocumentPagePublishService.class),
                mock(KnowledgeChunkBuilder.class),
                mock(KnowledgeChunkEmbeddingService.class),
                mock(KnowledgeChunkIndexRepository.class),
                mock(KnowledgeDocumentEventService.class),
                mock(IKnowledgeIngestTaskService.class),
                mock(CacheUtils.class),
                contentRenderService);
        String rawMarkdown = "![架构图](knowledge_image:fileId=image-file-id)";
        String renderedMarkdown = "![架构图](https://example.test/kit/file/stream/image-file-id)";
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument().setTargetPageId("page-id");
        KitKnowledgePage page = new KitKnowledgePage().setCurrentVersionId("version-id");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion().setMarkdownContent(rawMarkdown);
        when(directoryService.requireReadableDocument("document-id")).thenReturn(document);
        when(pageMapper.selectById("page-id")).thenReturn(page);
        when(versionMapper.selectById("version-id")).thenReturn(version);
        when(contentRenderService.render(rawMarkdown)).thenReturn(renderedMarkdown);

        String result = service.getMarkdown("document-id");

        assertEquals(renderedMarkdown, result);
        verify(contentRenderService).render(rawMarkdown);
    }
}

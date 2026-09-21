package org.quyq.gwsu.kit.knowledge.engine.search;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeContentRenderService;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDirectoryService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSearchEngineScopeTest {
    @Test
    void passesDirectoryGrantsToElasticsearchInsteadOfDocumentIds() {
        KnowledgeChunkIndexRepository indexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeChunkEmbeddingService embeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeDirectoryService directoryService = mock(KnowledgeDirectoryService.class);
        KnowledgeSearchEngine engine = new KnowledgeSearchEngine(indexRepository, embeddingService,
                mock(KnowledgeSearchRerankService.class), mock(KnowledgeContentRenderService.class),
                mock(KnowledgeProperties.class), mock(KnowledgePageBlockMapper.class),
                mock(KnowledgePageSourceRefMapper.class), mock(KnowledgePageVersionMapper.class),
                mock(KnowledgePageMapper.class), directoryService, mock(KnowledgeSourceDocumentMapper.class));
        Set<String> grants = Set.of("directory-1");
        when(directoryService.grantedDirectoryIds()).thenReturn(grants);
        when(embeddingService.embedQuery("policy")).thenReturn(Optional.empty());
        when(indexRepository.search("policy", grants, 10, Optional.empty())).thenReturn(List.of());
        KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
        dto.setKeyword("policy");
        dto.setSize(10);

        engine.search(dto);

        verify(indexRepository).search(eq("policy"), eq(grants), eq(10), eq(Optional.empty()));
    }
}

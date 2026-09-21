package org.quyq.gwsu.kit.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeMarkdownSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentBlocksVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuildRequest;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeContentRenderService;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgeBlockDraft;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePageDraft;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPagePublishService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentMarkdownService {
    private final KnowledgeDirectoryService directoryService;
    private final KnowledgePageMapper pageMapper;
    private final KnowledgePageVersionMapper versionMapper;
    private final KnowledgePageBlockMapper blockMapper;
    private final KnowledgePageSourceRefMapper refMapper;
    private final KnowledgeSourceDocumentMapper documentMapper;
    private final KnowledgeSourceDocumentPagePublishService publishService;
    private final KnowledgeChunkBuilder chunkBuilder;
    private final KnowledgeChunkEmbeddingService embeddingService;
    private final KnowledgeChunkIndexRepository indexRepository;
    private final KnowledgeDocumentEventService eventService;
    private final IKnowledgeIngestTaskService taskService;
    private final CacheUtils cacheUtils;
    private final KnowledgeContentRenderService contentRenderService;

    public String getMarkdown(String documentId) {
        var document = directoryService.requireReadableDocument(documentId);
        if (!StringUtils.hasText(document.getTargetPageId())) return "";
        KitKnowledgePage page = pageMapper.selectById(document.getTargetPageId());
        if (page == null || !StringUtils.hasText(page.getCurrentVersionId())) return "";
        KitKnowledgePageVersion version = versionMapper.selectById(page.getCurrentVersionId());
        return version == null ? "" : contentRenderService.render(version.getMarkdownContent());
    }

    public KnowledgeDocumentBlocksVO getBlocks(String documentId) {
        var document = directoryService.requireReadableDocument(documentId);
        if (!StringUtils.hasText(document.getTargetPageId())) return new KnowledgeDocumentBlocksVO().setBlocks(List.of());
        KitKnowledgePage page = pageMapper.selectById(document.getTargetPageId());
        if (page == null || !StringUtils.hasText(page.getCurrentVersionId())) return new KnowledgeDocumentBlocksVO().setBlocks(List.of());
        List<KitKnowledgePageBlock> blocks = blockMapper.selectByVersionId(page.getCurrentVersionId()).stream()
                .sorted(Comparator.comparing(KitKnowledgePageBlock::getOrderNo)).toList();
        Map<String, String> locators = refMapper.selectByPageBlockIds(blocks.stream().map(KitKnowledgePageBlock::getId).toList())
                .stream().collect(Collectors.toMap(ref -> ref.getPageBlockId(), ref -> ref.getSourceLocator() == null ? "" : ref.getSourceLocator()));
        return new KnowledgeDocumentBlocksVO().setPageVersionId(page.getCurrentVersionId()).setBlocks(blocks.stream()
                .map(block -> new KnowledgeDocumentBlocksVO.Block().setId(block.getId()).setOrderNo(block.getOrderNo())
                        .setBlockType(block.getBlockType()).setContent(block.getContent())
                        .setSourceLocator(locators.get(block.getId())))
                .toList());
    }

    public void saveBlocks(KnowledgeMarkdownSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDocumentId()) || !StringUtils.hasText(dto.getPageVersionId())
                || dto.getBlocks() == null || dto.getBlocks().isEmpty()) throw new BusinessException("请提供文档块内容");
        String documentId = dto.getDocumentId();
        taskService.ensureNoActiveTask(documentId);
        cacheUtils.executeWithLock("knowledge:page:" + documentId, () -> {
            var document = directoryService.requireEditableDocument(documentId);
            KitKnowledgePage page = pageMapper.selectById(document.getTargetPageId());
            if (page == null || !Objects.equals(page.getCurrentVersionId(), dto.getPageVersionId())) {
                throw new BusinessException("文档内容已更新，请刷新后重试");
            }
            List<KitKnowledgePageBlock> current = blockMapper.selectByVersionId(page.getCurrentVersionId()).stream()
                    .sorted(Comparator.comparing(KitKnowledgePageBlock::getOrderNo)).toList();
            if (dto.getBlocks().stream().anyMatch(block -> block == null || !StringUtils.hasText(block.getId())))
                throw new BusinessException("文档块标识不能为空");
            if (current.size() != dto.getBlocks().size()
                    || !Set.copyOf(current.stream().map(KitKnowledgePageBlock::getId).toList())
                    .equals(Set.copyOf(dto.getBlocks().stream().map(KnowledgeMarkdownSaveDTO.Block::getId).toList()))) {
                throw new BusinessException("只能编辑已有文档块");
            }
            var refs = refMapper.selectByPageBlockIds(current.stream().map(KitKnowledgePageBlock::getId).toList());
            var refsById = refs.stream().collect(Collectors.toMap(ref -> ref.getPageBlockId(), ref -> ref));
            var editsById = dto.getBlocks().stream().collect(Collectors.toMap(KnowledgeMarkdownSaveDTO.Block::getId, block -> block,
                    (left, right) -> { throw new BusinessException("文档块重复"); }));
            List<GeneratedKnowledgeBlockDraft> blocks = current.stream().map(block -> {
                var edit = editsById.get(block.getId());
                if (edit == null || edit.getBlockType() == null || !StringUtils.hasText(edit.getContent()))
                    throw new BusinessException("文档块类型和内容不能为空");
                var ref = refsById.get(block.getId());
                if (ref == null || !Objects.equals(ref.getSourceDocumentId(), documentId))
                    throw new BusinessException("文档块来源不正确");
                return new GeneratedKnowledgeBlockDraft(edit.getBlockType(), edit.getContent().trim(),
                        Objects.requireNonNullElse(ref.getSourceSegmentStartNo(), 0),
                        Objects.requireNonNullElse(ref.getSourceSegmentEndNo(), 0), ref.getSourceLocator());
            }).toList();
            String markdown = blocks.stream().map(GeneratedKnowledgeBlockDraft::content).collect(Collectors.joining("\n\n"));
            String versionId = publishService.publish(document,
                    new GeneratedKnowledgePageDraft(document.getFileName(), blocks, markdown));
            KitKnowledgePageVersion version = versionMapper.selectById(versionId);
            List<KitKnowledgePageBlock> pageBlocks = blockMapper.selectByVersionId(versionId);
            var publishedRefs = refMapper.selectByPageBlockIds(pageBlocks.stream().map(KitKnowledgePageBlock::getId).toList());
            var chunks = chunkBuilder.build(new KnowledgeChunkBuildRequest(version.getPageId(), document.getFileName(), version,
                    pageBlocks, publishedRefs));
            boolean embedded = embeddingService.embedChunks(chunks);
            indexRepository.replacePageVersion(version.getPageId(), versionId, chunks);
            documentMapper.updateById(document.setEmbeddingCompleted(embedded));
            eventService.append(documentId, null, "MARKDOWN_EDITED", "人工编辑并重建检索索引");
            return null;
        });
    }
}

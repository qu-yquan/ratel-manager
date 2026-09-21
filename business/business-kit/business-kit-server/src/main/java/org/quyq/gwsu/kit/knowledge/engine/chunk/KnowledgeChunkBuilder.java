package org.quyq.gwsu.kit.knowledge.engine.chunk;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 从已发布 Page Block 构建 ES-only Chunk。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeChunkBuilder {

    private static final String PUBLISHED = "PUBLISHED";

    private final KnowledgeProperties properties;

    public List<KnowledgeChunkDocument> build(KnowledgeChunkBuildRequest request) {
        validateSourceRefs(request);
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        for (KitKnowledgePageSourceRef ref : request.sourceRefs()) {
            refByBlockId.put(ref.getPageBlockId(), ref);
        }
        List<KnowledgeChunkDocument> chunks = new ArrayList<>();
        List<KitKnowledgePageBlock> orderedBlocks = request.blocks().stream()
                .sorted(Comparator.comparing(KitKnowledgePageBlock::getOrderNo))
                .toList();
        String currentHeading = "";
        List<String> consecutiveHeadingContents = new ArrayList<>();
        for (KitKnowledgePageBlock block : orderedBlocks) {
            if (block.getBlockType() != null && KnowledgeBlockType.HEADING == block.getBlockType()) {
                currentHeading = block.getContent();
                if (StringUtils.hasText(block.getContent())) {
                    consecutiveHeadingContents.add(block.getContent().trim());
                }
            }
            KitKnowledgePageSourceRef ref = refByBlockId.get(block.getId());
            if (block.getBlockType() == KnowledgeBlockType.HEADING || ref == null || !StringUtils.hasText(ref.getSourceDocumentId())) {
                continue;
            }
            String headingPrefix = buildHeadingPrefix(consecutiveHeadingContents);
            List<String> blockChunks = splitBlockContent(block.getContent());
            for (int index = 0; index < blockChunks.size(); index++) {
                String content = prependHeadingPrefix(blockChunks.get(index), headingPrefix, index == 0);
                chunks.add(new KnowledgeChunkDocument(
                        IdWorker.getIdStr(),
                        request.pageId(),
                        request.pageVersion().getId(),
                        block.getId(),
                        ref.getSourceDocumentId(),
                        null,
                        request.title(),
                        currentHeading,
                        content,
                        chunks.size() + 1,
                        DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)),
                        PUBLISHED,
                        request.pageVersion().getVersionNo(),
                        Instant.now(),
                        null,
                        null));
            }
            consecutiveHeadingContents.clear();
        }
        return List.copyOf(chunks);
    }

    private void validateSourceRefs(KnowledgeChunkBuildRequest request) {
        if (CollectionUtils.isEmpty(request.blocks()) || CollectionUtils.isEmpty(request.sourceRefs())) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        for (KitKnowledgePageSourceRef ref : request.sourceRefs()) {
            if (!StringUtils.hasText(ref.getPageBlockId())
                    || refByBlockId.put(ref.getPageBlockId(), ref) != null) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
        for (KitKnowledgePageBlock block : request.blocks()) {
            if (block == null || !StringUtils.hasText(block.getId())) {
                throw new BusinessException(KitErrorCode.E03009);
            }
            KitKnowledgePageSourceRef ref = refByBlockId.get(block.getId());
            if (block.getBlockType() != KnowledgeBlockType.HEADING
                    && (Objects.isNull(ref) || !StringUtils.hasText(ref.getSourceDocumentId()))) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
    }

    private List<String> splitBlockContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        int maxToken = Math.max(1, properties.getMaxToken());
        if (content.length() <= maxToken) {
            return List.of(content);
        }
        List<String> chunks = new ArrayList<>();
        String remaining = content;
        while (remaining.length() > maxToken) {
            int splitAt = findNaturalBoundary(remaining, maxToken);
            chunks.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (StringUtils.hasText(remaining)) {
            chunks.add(remaining);
        }
        return chunks;
    }

    private int findNaturalBoundary(String text, int maxToken) {
        int boundary = -1;
        String window = text.substring(0, Math.min(text.length(), maxToken + 1));
        for (String delimiter : List.of("。", ".", "\n", "；", ";", "，", ",")) {
            int index = window.lastIndexOf(delimiter);
            if (index > boundary) {
                boundary = index + delimiter.length();
            }
        }
        if (boundary > 0) {
            return boundary;
        }
        return maxToken;
    }

    private String buildHeadingPrefix(List<String> headingContents) {
        if (CollectionUtils.isEmpty(headingContents)) {
            return "";
        }
        return headingContents.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String prependHeadingPrefix(String chunkContent, String headingPrefix, boolean firstChunk) {
        if (!firstChunk || !StringUtils.hasText(headingPrefix)) {
            return chunkContent;
        }
        if (!StringUtils.hasText(chunkContent)) {
            return headingPrefix;
        }
        return headingPrefix + "\n" + chunkContent;
    }
}

package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgeBlockDraft;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePageDraft;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.task.KnowledgeIngestSupersededException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 一文一页发布服务。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSourceDocumentPagePublishService {

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    @Transactional(rollbackFor = Exception.class)
    public String publish(String taskId, String sourceDocumentId, GeneratedKnowledgePageDraft draft) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectByIdForUpdate(sourceDocumentId);
        if (sourceDocument == null || !Objects.equals(taskId, sourceDocument.getActiveTaskId())) {
            throw new KnowledgeIngestSupersededException(taskId);
        }
        return publishLocked(sourceDocument, draft, taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public String publish(KitKnowledgeSourceDocument sourceDocument, GeneratedKnowledgePageDraft draft) {
        KitKnowledgeSourceDocument lockedDocument = sourceDocumentMapper.selectByIdForUpdate(sourceDocument.getId());
        return publishLocked(lockedDocument, draft, null);
    }

    private String publishLocked(KitKnowledgeSourceDocument sourceDocument,
                                 GeneratedKnowledgePageDraft draft,
                                 String taskId) {
        String pageId = resolvePageId(sourceDocument);
        KitKnowledgePage page = ensurePage(pageId, draft.title());
        archiveCurrentVersion(page.getCurrentVersionId());
        String newVersionId = IdWorker.getIdStr();
        int nextVersionNo = Objects.requireNonNullElse(pageVersionMapper.selectMaxVersionNo(page.getId()), 0) + 1;
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId(newVersionId)
                .setPageId(page.getId())
                .setVersionNo(nextVersionNo)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent(draft.markdownContent())
                .setPublishedAt(LocalDateTime.now());
        pageVersionMapper.insert(version);
        int orderNo = 1;
        for (GeneratedKnowledgeBlockDraft blockDraft : draft.blocks()) {
            String blockId = IdWorker.getIdStr();
            pageBlockMapper.insert(new KitKnowledgePageBlock()
                    .setId(blockId)
                    .setPageVersionId(newVersionId)
                    .setOrderNo(orderNo++)
                    .setBlockType(blockDraft.blockType())
                    .setContent(blockDraft.content()));
            pageSourceRefMapper.insert(new KitKnowledgePageSourceRef()
                    .setId(IdWorker.getIdStr())
                    .setPageBlockId(blockId)
                    .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                    .setSourceDocumentId(sourceDocument.getId())
                    .setSourceSegmentStartNo(blockDraft.sourceSegmentStartNo())
                    .setSourceSegmentEndNo(blockDraft.sourceSegmentEndNo())
                    .setSourceLocator(blockDraft.sourceLocator()));
        }
        pageMapper.update(new KitKnowledgePage()
                        .setTitle(draft.title())
                        .setPageStatus(KnowledgePageStatus.PUBLISHED)
                        .setCurrentVersionId(newVersionId),
                new LambdaUpdateWrapper<KitKnowledgePage>()
                        .eq(KitKnowledgePage::getId, pageId)
                        .eq(KitKnowledgePage::getDeleted, false));
        LambdaUpdateWrapper<KitKnowledgeSourceDocument> documentUpdate =
                new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                        .eq(KitKnowledgeSourceDocument::getId, sourceDocument.getId())
                        .eq(KitKnowledgeSourceDocument::getDeleted, false);
        if (StringUtils.hasText(taskId)) {
            documentUpdate.eq(KitKnowledgeSourceDocument::getActiveTaskId, taskId);
        }
        sourceDocumentMapper.update(new KitKnowledgeSourceDocument().setTargetPageId(pageId), documentUpdate);
        return newVersionId;
    }

    private String resolvePageId(KitKnowledgeSourceDocument sourceDocument) {
        if (StringUtils.hasText(sourceDocument.getTargetPageId())) {
            return sourceDocument.getTargetPageId();
        }
        return IdWorker.getIdStr();
    }

    private KitKnowledgePage ensurePage(String pageId, String title) {
        KitKnowledgePage page = pageMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getDeleted, false));
        if (page != null) {
            return page;
        }
        KitKnowledgePage created = new KitKnowledgePage()
                .setId(pageId)
                .setTitle(title)
                .setPageStatus(KnowledgePageStatus.DRAFT);
        pageMapper.insert(created);
        return created;
    }

    private void archiveCurrentVersion(String currentVersionId) {
        if (!StringUtils.hasText(currentVersionId)) {
            return;
        }
        pageVersionMapper.update(new KitKnowledgePageVersion().setVersionStatus(KnowledgePageVersionStatus.ARCHIVED),
                new LambdaUpdateWrapper<KitKnowledgePageVersion>()
                        .eq(KitKnowledgePageVersion::getId, currentVersionId)
                        .eq(KitKnowledgePageVersion::getDeleted, false));
    }
}

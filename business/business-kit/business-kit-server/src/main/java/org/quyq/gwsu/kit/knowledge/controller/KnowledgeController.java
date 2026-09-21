package org.quyq.gwsu.kit.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchMetaVO;
import org.quyq.gwsu.kit.api.knowledge.KnowledgeClientApi;
import org.quyq.gwsu.kit.api.knowledge.dto.*;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.engine.search.KnowledgeSearchEngine;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDirectoryService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentEventService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeDocumentMarkdownService;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeNodeVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentEventVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentBlocksVO;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.kit.knowledge.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器。
 */
@RestController
@RequestMapping("knowledge")
@Tag(name = "知识库")
@RequiredArgsConstructor
public class KnowledgeController implements KnowledgeClientApi {

    private final KnowledgeProperties knowledgeProperties;

    private final KnowledgeIngestApplicationService knowledgeIngestApplicationService;

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    private final IKnowledgeIngestTaskService ingestTaskService;

    private final KnowledgeSearchEngine knowledgeSearchEngine;

    private final KnowledgeDirectoryService directoryService;

    private final KnowledgeNodeDeleteService nodeDeleteService;

    private final KnowledgeDocumentMarkdownService markdownService;

    private final KnowledgeDocumentEventService eventService;

    @Override
    @GetMapping("/search/meta")
    @Operation(summary = "获取知识检索元信息")
    public R<KnowledgeSearchMetaVO> getSearchMeta() {
        return R.ok(new KnowledgeSearchMetaVO()
                .setWikiPageLanguage(knowledgeProperties.getWikiOutputLanguage()));
    }

    @PostMapping("/document/save")
    @Operation(summary = "保存知识源文档并提交导入")
    public R<String> saveDocument(@RequestBody KnowledgeDocumentSaveDTO dto) {
        return R.ok(knowledgeIngestApplicationService.saveDocumentAndSubmit(dto));
    }

    @PostMapping("/document/{documentId}")
    @Operation(summary = "查询知识源文档详情")
    public R<KnowledgeDocumentVO> getDocument(@PathVariable String documentId) {
        return R.ok(sourceDocumentService.getDocument(documentId));
    }

    @PostMapping("/document/enable/{documentId}")
    @Operation(summary = "启用知识源文档")
    public R<Void> enableDocument(@PathVariable String documentId) {
        sourceDocumentService.updateEnabled(documentId, true);
        return R.ok();
    }

    @PostMapping("/document/disable/{documentId}")
    @Operation(summary = "禁用知识源文档")
    public R<Void> disableDocument(@PathVariable String documentId) {
        sourceDocumentService.updateEnabled(documentId, false);
        return R.ok();
    }

    @PostMapping("/directory/save")
    @Operation(summary = "新建或重命名知识目录")
    public R<String> saveDirectory(@RequestBody KnowledgeDirectorySaveDTO dto) {
        return R.ok(directoryService.saveDirectory(dto));
    }

    @GetMapping("/directory/tree")
    @Operation(summary = "获取可访问知识目录树")
    public R<List<KnowledgeNodeVO>> directoryTree() {
        return R.ok(directoryService.tree(false));
    }

    @GetMapping("/directory/root/capabilities")
    @Operation(summary = "获取知识根目录操作权限")
    public R<KnowledgeNodeVO> rootCapabilities() {
        return R.ok(directoryService.rootCapabilities());
    }

    @GetMapping("/directory/manage/tree")
    @Operation(summary = "获取授权管理目录树")
    public R<List<KnowledgeNodeVO>> managementDirectoryTree() {
        return R.ok(directoryService.tree(true));
    }

    @GetMapping("/node/children")
    @Operation(summary = "分页查询目录直接子节点")
    public R<IPage<KnowledgeNodeVO>> children(@RequestParam(required = false) String parentId,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(directoryService.children(parentId, name, pageNum, pageSize));
    }

    @GetMapping("/node/search")
    @Operation(summary = "按名称搜索有权限的全部知识文档")
    public R<IPage<KnowledgeNodeVO>> searchDocuments(@RequestParam String name,
                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(directoryService.searchDocuments(name, pageNum, pageSize));
    }

    @PostMapping("/node/delete")
    @Operation(summary = "批量删除知识目录与文档")
    public R<Void> deleteNodes(@RequestBody KnowledgeNodeDeleteDTO dto) {
        nodeDeleteService.delete(dto == null ? null : dto.getIds());
        return R.ok();
    }

    @GetMapping("/role/{roleCode}/directories")
    @Operation(summary = "获取角色已授权知识目录")
    public R<List<KnowledgeRoleScopeSaveDTO.Grant>> roleDirectories(@PathVariable String roleCode) {
        return R.ok(directoryService.grantsForRole(roleCode));
    }

    @PostMapping("/role/directories")
    @Operation(summary = "保存角色知识目录授权")
    public R<Void> saveRoleDirectories(@RequestBody KnowledgeRoleScopeSaveDTO dto) {
        directoryService.saveRoleScope(dto);
        return R.ok();
    }

    @GetMapping("/document/{documentId}/markdown")
    @Operation(summary = "读取文档Markdown")
    public R<String> getMarkdown(@PathVariable String documentId) {
        return R.ok(markdownService.getMarkdown(documentId));
    }

    @GetMapping("/document/block-types")
    @Operation(summary = "获取文档块类型选项")
    public R<List<KeyValue<String, String>>> blockTypes() {
        return R.ok(List.of(
                new KeyValue<>(KnowledgeBlockType.HEADING.name(), "标题"),
                new KeyValue<>(KnowledgeBlockType.PARAGRAPH.name(), "段落"),
                new KeyValue<>(KnowledgeBlockType.LIST.name(), "列表"),
                new KeyValue<>(KnowledgeBlockType.TABLE.name(), "表格"),
                new KeyValue<>(KnowledgeBlockType.CODE.name(), "代码"),
                new KeyValue<>(KnowledgeBlockType.QUOTE.name(), "引用")));
    }

    @GetMapping("/document/{documentId}/blocks")
    @Operation(summary = "读取文档Markdown块")
    public R<KnowledgeDocumentBlocksVO> getDocumentBlocks(@PathVariable String documentId) {
        return R.ok(markdownService.getBlocks(documentId));
    }

    @PostMapping("/document/markdown")
    @Operation(summary = "保存文档Markdown")
    public R<Void> saveMarkdown(@RequestBody KnowledgeMarkdownSaveDTO dto) {
        markdownService.saveBlocks(dto);
        return R.ok();
    }

    @GetMapping("/document/{documentId}/events")
    @Operation(summary = "查询文档事件日志")
    public R<List<KnowledgeDocumentEventVO>> events(@PathVariable String documentId) {
        return R.ok(eventService.list(documentId));
    }

    @PostMapping("/task/retry/{taskId}")
    @Operation(summary = "重新提交知识导入任务")
    public R<String> retryTask(@PathVariable String taskId) {
        return R.ok(knowledgeIngestApplicationService.retryAndSubmit(taskId));
    }

    @Override
    @PostMapping("/search")
    @Operation(summary = "知识库检索")
    public R<List<KnowledgeSearchResultVO>> search(@RequestBody KnowledgeSearchDTO dto) {
        return R.ok(knowledgeSearchEngine.search(dto));
    }

    @PostMapping("/search/debug")
    @Operation(summary = "按目录或角色调试知识检索")
    public R<List<KnowledgeSearchResultVO>> debugSearch(@RequestBody KnowledgeDebugSearchDTO dto) {
        return R.ok(knowledgeSearchEngine.searchWithDirectoryScopes(dto,
                debugScopes(dto.getMode(), dto.getDirectoryId(), dto.getRoleCode())));
    }

    @PostMapping("/search/debug/adjacent")
    @Operation(summary = "查询调试范围内的前后知识Chunk")
    public R<List<KnowledgeSearchResultVO>> debugAdjacent(@RequestBody KnowledgeDebugAdjacentDTO dto) {
        return R.ok(knowledgeSearchEngine.findAdjacentChunkWithDirectoryScopes(
                dto.getPageBlockId(), dto.getDirection(), dto.getOffset(),
                debugScopes(dto.getMode(), dto.getDirectoryId(), dto.getRoleCode())));
    }

    private List<String> debugScopes(String mode, String directoryId, String roleCode) {
        if ("DIRECTORY".equals(mode)) {
            if (directoryId == null || directoryId.isBlank()) throw new BusinessException("请选择目录");
            if (KnowledgeDirectoryService.ROOT_DIRECTORY_ID.equals(directoryId))
                return List.copyOf(directoryService.grantedDirectoryIds());
            var directory = directoryService.requireDirectory(directoryId);
            if (!directoryService.canRead(directory, directoryService.grantedDirectoryIds()))
                throw new BusinessException("没有该目录的检索权限");
            return List.of(directoryId);
        }
        if ("ROLE".equals(mode)) {
            if (roleCode == null || roleCode.isBlank()) throw new BusinessException("请选择角色");
            if (!directoryService.isAdministrator()) throw new BusinessException("只有管理员可以按角色调试检索");
            return List.copyOf(directoryService.grantedDirectoryIds(
                    List.of(roleCode, SecurityConstants.Authentication.ROLE_COMMON_FLAG)));
        }
        throw new BusinessException("检索调试模式不正确");
    }

    @Override
    @PostMapping("/chunk/adjacent")
    @Operation(summary = "查询指定Block的上一个或下一个")
    public R<List<KnowledgeSearchResultVO>> findAdjacentChunk(@RequestBody KnowledgeChunkAdjacentDTO dto) {
        return R.ok(knowledgeSearchEngine.findAdjacentChunk(
                dto.getPageBlockId(),
                dto.getDirection(),
                dto.getOffset()));
    }
}

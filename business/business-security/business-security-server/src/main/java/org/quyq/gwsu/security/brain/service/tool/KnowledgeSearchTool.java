package org.quyq.gwsu.security.brain.service.tool;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.kit.api.knowledge.KnowledgeClientApi;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.security.brain.service.citation.KnowledgeCitationContext;
import org.quyq.gwsu.security.brain.service.skill.KnowledgeSearchSkillRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 知识检索工具。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool {

    private static final int DEFAULT_TOP_K = 8;

    private static final int DEFAULT_ADJACENT_BLOCK_COUNT = 5;

    private final KnowledgeClientApi knowledgeClientApi;

    @Tool(name = "SearchKnowledge", description = KnowledgeSearchSkillRepository.SKILL_NAME + "技能的伴随工具，必须加载该技能才能使用。" +
            """
            根据检索词召回匹配度最高的知识片段。
            入参 query 必填，topK 选填，默认返回 8 条。
            注意：返回结果只是相关片段，不是完整答案，后续必须结合 FindAdjacentKnowledgeChunk 补全上下文再判断是否可回答用户问题。
            """)
    public Mono<String> searchKnowledge(
            @ToolParam(name = "query", description = "检索词，必须使用知识库底层语言进行检索") String query,
            @ToolParam(name = "topK", description = "返回条数，选填，默认 8") Integer topK,
            RuntimeContext runtimeContext) {
        return Mono.fromSupplier(() -> {
            if (!StringUtils.hasText(query)) {
                return "错误：query 不能为空。";
            }
            KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
            dto.setKeyword(query.trim());
            dto.setSize(topK == null || topK <= 0 ? DEFAULT_TOP_K : topK);
            List<KnowledgeSearchResultVO> results = FeignUtils.data(knowledgeClientApi.search(dto));
            if (CollectionUtils.isEmpty(results)) {
                return "未检索到匹配片段。";
            }
            return renderResults("知识片段检索结果", results, runtimeContext);
        }).onErrorResume(ex -> Mono.just("知识库检索服务暂时不可用，请稍后重试。原因：" + ex.getMessage()));
    }

    @Tool(name = "FindAdjacentKnowledgeChunk", description = KnowledgeSearchSkillRepository.SKILL_NAME + "技能的伴随工具，必须加载该技能才能使用。" +
            """
            根据 pageBlockId 和方向获取连续相邻的 5 个知识 block。
            direction 只能传 PREVIOUS 或 NEXT。
            该工具可多次调用；继续向外扩展时，必须传入当前已获取结果中最新边缘 block 的 pageBlockId。
            """)
    public Mono<String> findAdjacentKnowledgeChunk(
            @ToolParam(name = "pageBlockId", description = "当前要扩展的 block ID；继续扩展时请传当前边缘 block 的最新 pageBlockId") String pageBlockId,
            @ToolParam(name = "direction", description = "扩展方向，只能是 PREVIOUS 或 NEXT") KnowledgeChunkDirection direction,
            RuntimeContext runtimeContext) {
        return Mono.fromSupplier(() -> {
            if (!StringUtils.hasText(pageBlockId)) {
                return "错误：pageBlockId 不能为空。";
            }
            if (direction == null) {
                return "错误：direction 不能为空，且只能是 PREVIOUS 或 NEXT。";
            }
            KnowledgeChunkAdjacentDTO dto = new KnowledgeChunkAdjacentDTO();
            dto.setPageBlockId(pageBlockId.trim());
            dto.setDirection(direction);
            dto.setOffset(DEFAULT_ADJACENT_BLOCK_COUNT);
            List<KnowledgeSearchResultVO> results = FeignUtils.data(knowledgeClientApi.findAdjacentChunk(dto));
            if (CollectionUtils.isEmpty(results)) {
                return "未检索到相邻片段。";
            }
            return renderResults("相邻知识片段检索结果", results, runtimeContext);
        }).onErrorResume(ex -> Mono.just("知识上下文补全失败，当前无法确认答案完整性。原因：" + ex.getMessage()));
    }

    private String renderResults(String title, List<KnowledgeSearchResultVO> results, RuntimeContext runtimeContext) {
        StringBuilder sb = new StringBuilder(title).append("：\n");
        KnowledgeCitationContext citationContext = KnowledgeCitationContext.getOrCreate(runtimeContext);
        for (int i = 0; i < results.size(); i++) {
            KnowledgeSearchResultVO result = results.get(i);
            String citationKey = citationContext.register(result);
            sb.append("\n#").append(i + 1).append("\n")
                    .append("- citationKey: ").append(citationKey).append("\n")
                    .append("- pageBlockId: ").append(nullToDash(result.getPageBlockId())).append("\n")
                    .append("- chunkId: ").append(nullToDash(result.getChunkId())).append("\n")
                    .append("- sourceDocumentId: ").append(nullToDash(result.getSourceDocumentId())).append("\n")
                    .append("- sourceFileName: ").append(nullToDash(result.getSourceFileName())).append("\n")
                    .append("- title: ").append(nullToDash(result.getTitle())).append("\n")
                    .append("- headingPath: ").append(nullToDash(result.getHeadingPath())).append("\n")
                    .append("- score: ").append(result.getScore() == null ? "-" : result.getScore()).append("\n")
                    .append("- content:\n")
                    .append(result.getContent() == null ? "-" : result.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}

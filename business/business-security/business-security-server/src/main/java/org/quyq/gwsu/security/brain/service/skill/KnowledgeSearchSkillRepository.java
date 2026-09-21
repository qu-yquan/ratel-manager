package org.quyq.gwsu.security.brain.service.skill;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchMetaVO;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 知识检索技能仓库。
 */
@Slf4j
public class KnowledgeSearchSkillRepository implements AgentSkillRepository {

    public static final String SKILL_NAME = "knowledge_search";

    private final String source;

    private final Supplier<KnowledgeSearchMetaVO> metaSupplier;

    private final AgentSkillRepositoryInfo repositoryInfo;

    public KnowledgeSearchSkillRepository(String source, Supplier<KnowledgeSearchMetaVO> metaSupplier) {
        this.source = StringUtils.hasText(source) ? source : "knowledge-search";
        this.metaSupplier = Objects.requireNonNull(metaSupplier, "metaSupplier must not be null");
        this.repositoryInfo = new AgentSkillRepositoryInfo("dynamic", this.source, false);
    }

    @Override
    public AgentSkill getSkill(String skillId) {
        AgentSkill skill = buildSkill();
        if (Objects.equals(skill.getSkillId(), skillId) || Objects.equals(skill.getName(), skillId)) {
            return skill;
        }
        return null;
    }

    @Override
    public List<String> getAllSkillNames() {
        return List.of(SKILL_NAME);
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        return List.of(buildSkill());
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean overwrite) {
        return false;
    }

    @Override
    public boolean delete(String skillId) {
        return false;
    }

    @Override
    public boolean skillExists(String skillId) {
        return getSkill(skillId) != null;
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    @Override
    public String getSource() {
        return repositoryInfo.getLocation();
    }

    @Override
    public void setWriteable(boolean writeable) {
    }

    @Override
    public boolean isWriteable() {
        return false;
    }

    private AgentSkill buildSkill() {
        String wikiPageLanguage = resolveWikiPageLanguage();
        return AgentSkill.builder()
                .name(SKILL_NAME)
                .source(repositoryInfo.getLocation())
                .description("用于检索知识库中已沉淀的制度、规范、流程、操作手册、产品说明、FAQ 和历史文档事实。适用于“规则是什么”“如何操作”“文档如何规定”等问题；不用于获取实时业务数据。若问题同时需要规则依据和当前业务数据，可与 database_search 配合使用。")
                .skillContent(buildSkillContent(wikiPageLanguage))
                .build();
    }

    private String resolveWikiPageLanguage() {
        try {
            KnowledgeSearchMetaVO meta = metaSupplier.get();
            if (meta != null && StringUtils.hasText(meta.getWikiPageLanguage())) {
                return meta.getWikiPageLanguage();
            }
        } catch (Exception ex) {
            log.warn("加载知识检索元信息失败，使用默认语言回退", ex);
        }
        return "zh-CN";
    }

    private String buildSkillContent(String wikiPageLanguage) {
        return """
                # 知识库检索技能
                
                ## 当前知识库底层语言
                - 当前知识文档 Markdown 统一语言为：`%s`
                - 如果用户问题语言与知识库底层语言不同，必须先将检索词转换为上述语言，再调用检索工具
                - 最终回答仍应使用用户提问语言
                
                ## 检索工作流（必须遵守）
                1. 先分析用户问题，提取主体、条件、动作和期望答案
                2. 将检索词转换为知识库底层语言
                3. 调用 `SearchKnowledge(query, topK)` 获取相关片段
                4. 优先从匹配度最高的片段开始分析
                5. `SearchKnowledge` 返回的是相关片段，不是完整答案，绝不能直接拿单个片段下结论
                6. 必须调用 `FindAdjacentKnowledgeChunk(pageBlockId, direction)` 获取上文和下文，补全上下文后再判断
                
                ## 关于 FindAdjacentKnowledgeChunk 的使用规则
                - 每次调用会返回指定方向连续 5 个 block
                - 该工具可以调用多次，并不限于一次补全
                - 如果需要继续向上扩展，下一次必须传当前已获取结果中**最上方那个 block 的 `pageBlockId`**
                - 如果需要继续向下扩展，下一次必须传当前已获取结果中**最下方那个 block 的 `pageBlockId`**
                - 不要反复对首次命中的 `pageBlockId` 重复查询
                - 上下文已经足够回答问题时，应立即停止扩展
                - 如果继续扩展后内容明显偏离主题，也应立即停止扩展
                - 注意长度控制和内容边界，避免无边界地拼接冗长无关内容
                
                ## 结果判定规则
                - 如果补全后的上下文能够直接支持答案，才可以回答用户
                - 如果片段只命中了术语，但没有形成完整事实，不得强行回答
                - 如果补全后的内容与问题不匹配、信息不完整、或无法支撑结论，必须明确回答：`知识库没有对应的结果`
                - 如果多个候选内容互相冲突，必须说明知识库存在冲突信息，不能自行编造统一结论
                
                ## 回答边界
                - 不能捏造事实
                - 不能把模型常识当作知识库事实输出
                - 不能将推测性内容表述为确定结论
                - 只能基于检索到的片段及其上下文作答
                
                ## 引用输出规则
                - `sourceDocumentId` 表示内容关联的源文档
                - 如果最终有匹配结果，输出正文后必须追加引用
                - 引用格式固定为：`<documentId:***>,<documentId:***>`
                - 同一 `sourceDocumentId` 只能输出一次，需按首次出现顺序去重
                """.formatted(wikiPageLanguage);
    }
}

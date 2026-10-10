package org.quyq.gwsu.security.brain.vo;

import io.agentscope.core.agui.event.AguiEvent;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 历史消息的扩展元数据。
 */
@Data
@Accessors(chain = true)
public class BrainHistoryMessageMetadataVO {

    private List<KnowledgeReferenceVO> knowledgeReferences;

    /**
     * 当前仍处于待处理状态的官方 AG-UI 审批中断。
     */
    private List<AguiEvent.Interrupt> approvalInterrupts;

    /**
     * 当前仍处于待回答状态的 AskUserQuestion 官方 AG-UI 中断。
     */
    private List<AguiEvent.Interrupt> questionInterrupts;
}

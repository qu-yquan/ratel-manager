package org.quyq.gwsu.security.brain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.common.ai.agui.model.AguiMessage;

/**
 * 历史消息恢复对象。通过组合扩展消息元数据，避免改变通用 AG-UI 消息模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrainHistoryMessageVO {

    private AguiMessage message;

    private BrainHistoryMessageMetadataVO metadata;
}

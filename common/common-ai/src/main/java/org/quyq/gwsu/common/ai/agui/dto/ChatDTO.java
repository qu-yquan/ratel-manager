package org.quyq.gwsu.common.ai.agui.dto;


import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
public record ChatDTO(
        String method ,
        Map<String , Object> params ,
        // info/agent/stop 没有完整的 RunAgentInput；仅在运行和连接时按 method 转换。
        Object body
) {
}

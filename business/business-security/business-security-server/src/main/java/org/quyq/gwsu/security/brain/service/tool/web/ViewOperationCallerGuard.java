package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.agent.Agent;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.stereotype.Component;

/**
 * 限制视图操作工具只能由主智能体直接调用。
 */
@Component
public class ViewOperationCallerGuard {

    public void check(Agent caller) {
        if (caller == null
                || !CoreConstants.Agent.BRAIN_AGENT_NAME.equals(caller.getName())) {
            throw new RuntimeException("VIEW_OPERATION_ROOT_ONLY：视图操作工具仅允许主智能体调用。"
                    + "子智能体必须停止界面操作，不得重试或调用其他界面工具，并将任务返回主智能体。");
        }
    }
}

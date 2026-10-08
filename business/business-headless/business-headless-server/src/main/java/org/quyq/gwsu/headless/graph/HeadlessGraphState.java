package org.quyq.gwsu.headless.graph;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 无头浏览器工作流状态。
 */
public final class HeadlessGraphState extends AgentState {

    public HeadlessGraphState(Map<String, Object> initData) {
        super(initData);
    }
}

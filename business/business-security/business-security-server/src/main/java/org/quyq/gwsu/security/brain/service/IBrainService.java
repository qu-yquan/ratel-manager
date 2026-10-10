package org.quyq.gwsu.security.brain.service;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.processor.AguiRequestProcessor;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
public interface IBrainService {

    String AGENT_ID = "brain";


    void refreshSingletonAgent();

    Agent getOrCreateSingletonAgent();

}

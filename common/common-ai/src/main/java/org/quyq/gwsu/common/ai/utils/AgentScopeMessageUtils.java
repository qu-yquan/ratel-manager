package org.quyq.gwsu.common.ai.utils;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;

public final class AgentScopeMessageUtils {

    private AgentScopeMessageUtils() {
    }

    public static String text(Msg msg) {
        if (msg == null) {
            return "";
        }
        return msg.getContentBlocks(TextBlock.class).stream()
                .map(TextBlock::getText)
                .reduce("", String::concat);
    }
}

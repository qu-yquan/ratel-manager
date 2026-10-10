package org.quyq.gwsu.common.ai.constants;

import java.time.Duration;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description Ai模块公共常量
 */
public interface AIConstants {

    /**
     * 需要人工审批的工具名
     */
    String MSG_METADATA_APPROVAL_TOOLS_KEY = "approval_tools";

    interface AguiCustomEvent {

        /**
         * web端工具执行事件
         */
        String TOOL_EXECUTE = "TOOL_EXECUTE";

        /**
         * 知识库引用事件。
         */
        String KNOWLEDGE_REFERENCES = "KNOWLEDGE_REFERENCES";

    }

    interface Param {
        String THREAD_ID = "threadId";
        String EMITTER_WRAPPER = "servletHeaders";
        String RUNTIME_CONTEXT = "runtimeContext";
        String FORWARDED_PROPS_KEY =  "forwardedProps";

        String FORWARDED_PROPS_OPERATION_MODE_KEY = "operationMode";
        String FORWARDED_PROPS_CURRENT_PATH_KEY = "currentPath";

    }

    interface AgentStateKey {
        String WEB_PAGE_APPROVAL_INDEXES = "webPageApprovalIndexes";
    }

    interface ToolName {

        String ASK_USER_QUESTION = "AskUserQuestion";

    }

    /**
     * AgentScope 分布式存储 Redis 常量。
     */
    interface DistributedStoreRedis {

        String KEY_PREFIX = "agentscope:distributed:";
        String NAMESPACE_SEPARATOR = "/";
        String STORE_ITEM_KEY_TEMPLATE = KEY_PREFIX + "store:item:%s:%s";
        String STORE_ITEM_VERSION_KEY_TEMPLATE = KEY_PREFIX + "store:item:version:%s:%s";
        String STORE_INDEX_KEY_TEMPLATE = KEY_PREFIX + "store:index:%s";
        String HISTORY_INDEX_KEY_TEMPLATE = KEY_PREFIX + "history:index:user:%s";
        String HISTORY_DETAIL_KEY_TEMPLATE = KEY_PREFIX + "history:detail:user:%s:session:%s";
        String STATE_KEY_PREFIX = KEY_PREFIX + "state:";
        String STATE_KEYS_SUFFIX = ":_keys";
        String STATE_LIST_SUFFIX = ":list";
        String STATE_LIST_HASH_SUFFIX = ":hash";
        String STATE_VALUE_KEY_TEMPLATE = STATE_KEY_PREFIX + "%s:%s";
        String STATE_LIST_KEY_TEMPLATE = STATE_KEY_PREFIX + "%s:%s" + STATE_LIST_SUFFIX;
        String STATE_KEYS_KEY_TEMPLATE = STATE_KEY_PREFIX + "%s" + STATE_KEYS_SUFFIX;
        String STATE_SESSION_SCAN_PATTERN_TEMPLATE = STATE_KEY_PREFIX + "%s:*" + STATE_KEYS_SUFFIX;
        String SNAPSHOT_KEY_TEMPLATE = KEY_PREFIX + "snapshot:%s";
        String LOCK_KEY_TEMPLATE = KEY_PREFIX + "lock:%s:%s";

        String FIELD_KEY = "key";
        String FIELD_VERSION = "version";
        String FIELD_PAYLOAD = "payload";
        String FIELD_CREATED_AT = "createdAt";
        String FIELD_MODIFIED_AT = "modifiedAt";
        String FIELD_SNAPSHOT_ID = "snapshotId";
        String FIELD_CONTENT = "content";
        String FIELD_UPDATED_AT = "updatedAt";

        Duration SNAPSHOT_TTL = Duration.ofHours(24);
        Duration LOCK_TTL = Duration.ofSeconds(30);
        Duration LOCK_RETRY_INTERVAL = Duration.ofMillis(200);
    }

}

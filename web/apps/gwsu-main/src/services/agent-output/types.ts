import type { KnowledgeReference } from '@/services/knowledge-reference';

/**
 * AGENT_OUTPUT 自定义事件 payload
 * 后端 OutputViewEventHandlerHook 发送的完整 JSONL Patch 行
 */
export interface AgentOutputPayload {
  /** JSONL Patch 行文本 */
  text: string;
  /** 该 patch 中引用标识对应的有效文档数据 */
  references?: KnowledgeReference[];
}

/** 输出视图新一轮开始 */
export interface AgentOutputStartPayload {
  text: string;
}

/**
 * AGENT_OUTPUT_END 自定义事件 payload
 * 后端发送，标识本次 AI 输出结束
 */
export interface AgentOutputEndPayload {
  /** 结束标识 */
  text: string;
}

/**
 * AGENT_OUTPUT_RESET 自定义事件 payload
 * 前端在新建会话或切换会话时发送，要求输出面板清空当前展示内容
 */
export interface AgentOutputResetPayload {
  /** 重置原因 */
  reason?: string;
}

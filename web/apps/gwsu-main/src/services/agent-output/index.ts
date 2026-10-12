import type {
  AgentOutputPayload,
  AgentOutputStartPayload,
  AgentOutputEndPayload,
  AgentOutputResetPayload,
} from './types';

export type {
  AgentOutputPayload,
  AgentOutputStartPayload,
  AgentOutputEndPayload,
  AgentOutputResetPayload,
} from './types';

/** 输出事件监听器列表 */
const outputListeners = new Set<(payload: AgentOutputPayload) => void>();
const outputStartListeners = new Set<
  (payload: AgentOutputStartPayload) => void
>();

/** 输出结束事件监听器列表 */
const outputEndListeners = new Set<(payload: AgentOutputEndPayload) => void>();

/** 输出重置事件监听器列表 */
const outputResetListeners = new Set<
  (payload: AgentOutputResetPayload) => void
>();

/**
 * 分发 AI 输出事件
 * 由 CopilotKitProvider 中的 CUSTOM 事件监听调用
 */
export function dispatchAgentOutput(payload: AgentOutputPayload): void {
  outputListeners.forEach((listener) => listener(payload));
}

export function dispatchAgentOutputStart(
  payload: AgentOutputStartPayload,
): void {
  outputStartListeners.forEach((listener) => listener(payload));
}

/**
 * 分发 AI 输出结束事件
 */
export function dispatchAgentOutputEnd(payload: AgentOutputEndPayload): void {
  outputEndListeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前输出内容
 */
export function clearAgentOutput(): void {
  outputResetListeners.forEach((listener) =>
    listener({ reason: 'session-reset' }),
  );
}

/**
 * 注册输出事件监听器（供 AiOutputPanel 使用）
 * @returns 取消监听的函数
 */
export function onAgentOutput(
  listener: (payload: AgentOutputPayload) => void,
): () => void {
  outputListeners.add(listener);
  return () => {
    outputListeners.delete(listener);
  };
}

export function onAgentOutputStart(
  listener: (payload: AgentOutputStartPayload) => void,
): () => void {
  outputStartListeners.add(listener);
  return () => outputStartListeners.delete(listener);
}

/**
 * 注册输出结束事件监听器
 * @returns 取消监听的函数
 */
export function onAgentOutputEnd(
  listener: (payload: AgentOutputEndPayload) => void,
): () => void {
  outputEndListeners.add(listener);
  return () => {
    outputEndListeners.delete(listener);
  };
}

/**
 * 注册输出重置事件监听器
 * @returns 取消监听的函数
 */
export function onAgentOutputReset(
  listener: (payload: AgentOutputResetPayload) => void,
): () => void {
  outputResetListeners.add(listener);
  return () => {
    outputResetListeners.delete(listener);
  };
}

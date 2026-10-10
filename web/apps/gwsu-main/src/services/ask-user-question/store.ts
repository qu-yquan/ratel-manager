import type {
  AskUserQuestionAnswer,
  AskUserQuestionPayload,
  PendingAskUserQuestionInterrupt,
} from './types';

/** 事件监听器列表，payload 为 null 表示清除状态 */
const listeners = new Set<(payload: AskUserQuestionPayload | null) => void>();

/** 当前待回答问题 */
let currentPending: AskUserQuestionPayload | null = null;

/** 当前待恢复的 AskUserQuestion 官方 interrupt。 */
let currentPendingInterrupt: PendingAskUserQuestionInterrupt | null = null;

/**
 * 分发 AskUserQuestion 事件
 * 由 CopilotKitProvider 中的 ToolCallEndEvent 监听调用
 */
export function dispatchAskUserQuestion(payload: AskUserQuestionPayload): void {
  currentPending = payload;
  listeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前待回答事件，并通知所有监听器
 * 用户完成作答、新建会话、切换会话时调用
 */
export function clearAskUserQuestion(): void {
  currentPending = null;
  currentPendingInterrupt = null;
  listeners.forEach((listener) => listener(null));
}

/** 仅清除问题展示，不影响已捕获的官方 interrupt。 */
export function clearAskUserQuestionPrompt(): void {
  currentPending = null;
  listeners.forEach((listener) => listener(null));
}

export function setPendingAskUserQuestionInterrupt(
  pending: PendingAskUserQuestionInterrupt,
): void {
  currentPendingInterrupt = pending;
}

export function clearPendingAskUserQuestionInterrupt(
  interruptId?: string,
): void {
  if (interruptId && currentPendingInterrupt?.interrupt.id !== interruptId) {
    return;
  }
  currentPendingInterrupt = null;
}

/** 使用官方 interrupt/resume 协议提交问题答案。 */
export async function resolveAskUserQuestion(
  toolCallId: string,
  answer: AskUserQuestionAnswer,
): Promise<void> {
  const pending = currentPendingInterrupt;
  if (!pending) {
    throw new Error('当前没有待处理的 AskUserQuestion interrupt');
  }
  const interrupt = pending.interrupts.find(
    (item) => item.toolCallId === toolCallId,
  );
  if (!interrupt) {
    throw new Error(
      `未找到工具调用 ${toolCallId} 对应的 AskUserQuestion interrupt`,
    );
  }

  clearPendingAskUserQuestionInterrupt(interrupt.id);
  try {
    await pending.resolve(answer, interrupt.id);
  } catch (error) {
    // 官方续跑失败时保留原 interrupt，允许界面或无头调用重试；
    // 若期间已收到新的 interrupt，则不得用旧状态覆盖。
    if (!currentPendingInterrupt) {
      currentPendingInterrupt = pending;
    }
    throw error;
  }
}

/**
 * 获取当前待回答事件
 */
export function getPendingAskUserQuestion(): AskUserQuestionPayload | null {
  return currentPending;
}

/**
 * 注册事件监听器（供 UI 组件使用）
 * payload 为 null 时表示状态已清除
 * @returns 取消监听的函数
 */
export function onAskUserQuestion(
  listener: (payload: AskUserQuestionPayload | null) => void,
): () => void {
  listeners.add(listener);

  // 如果已有待回答事件，立即通知
  if (currentPending) {
    listener(currentPending);
  }

  return () => {
    listeners.delete(listener);
  };
}

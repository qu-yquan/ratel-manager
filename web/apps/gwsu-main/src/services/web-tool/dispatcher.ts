import { aiRequestErrorPool, post } from '@gwsu/core';
import { getWebTool } from './registry';
import type { WebToolExecutePayload, WebToolCallbackRequest, WebToolConfirmEvent, WebToolResult } from './types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

/** 需要 AI 操作模式的工具列表 */
const AI_MODE_REQUIRED_TOOLS = [
  'RouteNavigation',
  'ClickElement',
  'InputText',
  'SelectOption',
  'ScrollPage',
  'AttachUploadedFile',
];

/** 确认事件监听器列表 */
const confirmListeners = new Set<(event: WebToolConfirmEvent) => void>();

/**
 * 注册确认事件监听器（供 UI 组件使用）
 */
export function onWebToolConfirm(listener: (event: WebToolConfirmEvent) => void): () => void {
  confirmListeners.add(listener);
  return () => {
    confirmListeners.delete(listener);
  };
}

/**
 * 分发Web工具执行
 * 通用流程：查找执行器 → (INTERACTIVE时用户确认) → 执行工具 → 回调结果
 * 只有"执行工具"步骤调用不同的executor，其余均为通用逻辑
 */
export async function dispatchWebTool(payload: WebToolExecutePayload): Promise<void> {
  const startedAt = Date.now();
  const { toolCallId, toolName, params } = payload;
  let result: WebToolResult;
  let actionDispatched: boolean | undefined;
  const store = useForwardedPropsStore.getState();
  const executor = getWebTool(toolName);

  if (store.operationMode === 'human' && AI_MODE_REQUIRED_TOOLS.includes(toolName)) {
    const reason = store.exitReason || '当前为人类操作模式，请先进入AI操作模式';
    if (store.exitReason) store.clearExitReason();
    result = { success: false, result: reason };
    actionDispatched = false;
  } else if (!executor) {
    result = { success: false, result: `未知工具: ${toolName}` };
    actionDispatched = false;
  } else {
    try {
      result = await executor.execute(params);
      // 失败可能发生在操作发出之后，不能据此断言未执行。
      if (result.success && toolName !== 'GetPageState') actionDispatched = true;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      result = { success: false, result: `执行异常: ${message}` };
    }
  }

  // 包括 DOM 读取失败的分支，也要交付已采集的接口错误。
  const snapshot = toolName === 'GetPageState' ? aiRequestErrorPool.snapshot() : undefined;
  let content: Record<string, unknown> = { message: result.result };
  if (toolName === 'GetPageState') {
    try {
      const parsed: unknown = JSON.parse(result.result);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        content = parsed as Record<string, unknown>;
      }
    } catch {
      // 错误分支的纯文本也使用同一时间和错误快照协议。
    }
  }
  const serialized = JSON.stringify({
    ...content,
    toolName,
    toolCallId,
    startedAt,
    finishedAt: Date.now(),
    actionDispatched,
    ...(snapshot ? {
      snapshotAt: snapshot.snapshotAt,
      requestErrors: snapshot.errors,
      droppedErrorCount: snapshot.droppedCount,
    } : {}),
  });
  const accepted = await callbackToolResult(toolCallId, result.success, serialized);
  if (accepted && snapshot) aiRequestErrorPool.acknowledge(snapshot.receipt);
}

/**
 * 回调工具执行结果给后端
 * 通用逻辑，所有工具执行后统一调用
 */
async function callbackToolResult(
  toolCallId: string,
  success: boolean,
  result: string,
): Promise<boolean> {
  try {
    const response = await post<unknown>('/security/brain/tool/callback', {
      toolCallId,
      success,
      result,
    } satisfies WebToolCallbackRequest, { captureAiError: false });
    return response.code === 0 || response.code === 200;
  } catch (error) {
    console.error('[WebTool] 回调结果失败:', error);
    return false;
  }
}

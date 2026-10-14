import type { AiErrorReceipt, AiRequestError, AiRequestErrorSnapshot } from '../types/ai-request-error';

const MAX_ERRORS = 100;

/** 独立工厂便于验证消费并发边界；应用使用下方的窗口级单例。 */
export function createAiRequestErrorPool() {
  let enabled = false;
  let sessionId: string | null = null;
  let errors: AiRequestError[] = [];
  let dropped = 0;
  let acknowledgedDropped = 0;

  return {
    setEnabled(next: boolean): void {
      if (next === enabled) return;
      enabled = next;
      sessionId = next ? crypto.randomUUID() : null;
      errors = [];
      dropped = 0;
      acknowledgedDropped = 0;
    },
    append(error: AiRequestError): void {
      if (!enabled) return;
      errors.push({ ...error });
      if (errors.length > MAX_ERRORS) {
        errors.shift();
        dropped++;
      }
    },
    snapshot(): AiRequestErrorSnapshot {
      return {
        snapshotAt: Date.now(),
        errors: errors.map(error => ({ ...error })),
        droppedCount: dropped - acknowledgedDropped,
        receipt: { sessionId, errorIds: errors.map(error => error.id), droppedThrough: dropped },
      };
    },
    acknowledge(receipt: AiErrorReceipt): void {
      if (!enabled || receipt.sessionId !== sessionId) return;
      const ids = new Set(receipt.errorIds);
      errors = errors.filter(error => !ids.has(error.id));
      acknowledgedDropped = Math.max(acknowledgedDropped, Math.min(dropped, receipt.droppedThrough));
    },
  };
}

// 沿用项目共享 Store 的真实窗口单例方式，避免 qiankun 沙箱各持一份错误池。
const rawWindow = (0, eval)('window') as Window & {
  __GWSU_AI_REQUEST_ERROR_POOL__?: ReturnType<typeof createAiRequestErrorPool>;
};
export const aiRequestErrorPool = rawWindow.__GWSU_AI_REQUEST_ERROR_POOL__
  ?? (rawWindow.__GWSU_AI_REQUEST_ERROR_POOL__ = createAiRequestErrorPool());

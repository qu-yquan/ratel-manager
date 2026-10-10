import type { Interrupt, RunAgentResult } from '@ag-ui/client';

/**
 * 问题选项
 */
export interface QuestionOption {
  label: string;
  description: string;
}

/**
 * 问题参数（对应后端 QuestionParam）
 */
export interface QuestionParam {
  question: string;
  header: string;
  options: QuestionOption[];
  multiSelect: boolean;
}

/**
 * AskUserQuestion 事件载荷
 */
export interface AskUserQuestionPayload {
  /** 工具调用唯一标识，用于构造 tool 结果消息 */
  toolCallId: string;
  /** 问题列表（1-4个） */
  questions: QuestionParam[];
}

/**
 * 用户作答结果
 */
export interface AskUserQuestionAnswer {
  /** key=question, value=选中的label（多选时逗号分隔） */
  answers: Record<string, string>;
  /** 可选的备注信息 */
  annotations: Record<string, { preview?: string; notes?: string }>;
}

export type AskUserQuestionResolve = (
  payload?: unknown,
  interruptId?: string,
) => Promise<RunAgentResult | void>;

/** AskUserQuestion 暂停工具对应的官方 AG-UI interrupt。 */
export interface PendingAskUserQuestionInterrupt {
  interrupt: Interrupt;
  interrupts: Interrupt[];
  resolve: AskUserQuestionResolve;
}

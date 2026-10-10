import type {
  AskUserQuestionPayload,
  QuestionOption,
  QuestionParam,
} from './types';

function normalizeOptions(options: unknown): QuestionOption[] {
  if (Array.isArray(options)) return options as QuestionOption[];
  if (options && typeof options === 'object') {
    return [options as QuestionOption];
  }
  return [];
}

/** 将工具参数统一转换为 AskUserQuestion 前端载荷。 */
export function normalizeAskUserQuestionPayload(
  toolCallId: string | undefined,
  args: unknown,
): AskUserQuestionPayload | null {
  if (!toolCallId || !args || typeof args !== 'object') return null;
  const rawQuestions = (args as Record<string, unknown>).questions;
  if (!Array.isArray(rawQuestions) || rawQuestions.length === 0) return null;

  const questions: QuestionParam[] = rawQuestions.map((item) => {
    const question = item as Record<string, unknown>;
    return {
      question: String(question.question ?? ''),
      header: String(question.header ?? ''),
      options: normalizeOptions(question.options),
      multiSelect: Boolean(question.multiSelect),
    };
  });
  return { toolCallId, questions };
}

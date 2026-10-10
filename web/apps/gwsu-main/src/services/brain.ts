/**
 * 大脑相关 API 服务
 */

import { post, get, del } from '@gwsu/core';
import type { Interrupt } from '@ag-ui/client';
import type { KnowledgeReference } from './knowledge-reference';

/**
 * 历史会话信息
 */
export interface BrainHistorySession {
  sessionId: string;
  title: string;
  messageCount: number;
  updatedAt: string;
  timeDisplay: string;
}

/**
 * 工具调用信息
 */
export interface BrainToolCall {
  id: string;
  type: 'function';
  function: {
    name: string;
    arguments: string;
  };
  args: string;
}

/**
 * 消息信息（AG-UI Message 格式）
 */
export interface BrainMessage {
  id: string;
  role: 'user' | 'assistant' | 'system' | 'tool' | 'reasoning';
  content: string | object[] | null;
  toolCalls?: BrainToolCall[];
  toolCallId?: string | null;
  /** 加密的推理内容，用于会话状态连续性 */
  encryptedValue?: string;
}

export interface BrainHistoryMessage {
  message: BrainMessage;
  metadata?: {
    knowledgeReferences?: KnowledgeReference[];
    approvalInterrupts?: Interrupt[];
    questionInterrupts?: Interrupt[];
  } | null;
}

export interface BrainHistorySessionSlice {
  records: BrainHistorySession[];
  hasMore: boolean;
  nextPageNum?: number | null;
}

/**
 * 分页查询历史会话列表
 */
export async function getHistorySessions(
  pageNum: number = 1,
  pageSize: number = 20,
): Promise<BrainHistorySessionSlice> {
  const response = await post<BrainHistorySessionSlice>(
    '/security/brain/history/sessions',
    {
      pageNum,
      pageSize,
    },
  );
  return response.data;
}

/**
 * 查询会话消息列表
 */
export async function getSessionMessages(
  sessionId: string,
): Promise<BrainHistoryMessage[]> {
  const response = await get<BrainHistoryMessage[]>(
    `/security/brain/history/sessions/${sessionId}/messages`,
  );
  return response.data;
}

/**
 * 删除会话
 */
export async function deleteSession(sessionId: string): Promise<boolean> {
  const response = await del<boolean>(
    `/security/brain/history/sessions/${sessionId}`,
  );
  return response.data;
}

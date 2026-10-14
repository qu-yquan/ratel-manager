/** AI 可读取的请求错误；时间均为浏览器生成的 Unix 毫秒时间戳。 */
export interface AiRequestError {
  id: string;
  method: string;
  path: string;
  requestStartedAt: number;
  failedAt: number;
  type: 'http' | 'business' | 'network' | 'timeout' | 'parse';
  httpStatus?: number;
  businessCode?: number;
  errCode?: string;
  message: string;
}

/** 仅供前端确认消费，不发送给 AI。 */
export interface AiErrorReceipt {
  sessionId: string | null;
  errorIds: string[];
  /** 累计丢弃序号，重复确认不会误减新产生的丢弃计数。 */
  droppedThrough: number;
}

export interface AiRequestErrorSnapshot {
  snapshotAt: number;
  errors: AiRequestError[];
  droppedCount: number;
  receipt: AiErrorReceipt;
}

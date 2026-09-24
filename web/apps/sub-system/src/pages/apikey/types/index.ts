export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages?: number;
}

export interface ApiKeyInfo {
  id: string;
  apiKeyName: string;
  maskedKey: string;
  status: number;
  expireTime?: string;
  lastUsedTime?: string;
  createTime?: string;
  remark?: string;
}

export interface ApiKeyDetail extends ApiKeyInfo {
  lastUsedIp?: string;
}

export interface ApiKeyQuery {
  apiKeyName?: string;
  pageNum: number;
  pageSize: number;
}

export type ApiKeyExpireType = 'FOREVER' | 'CUSTOM_DATE' | 'AFTER_DAYS';

export interface ApiKeyCreateDTO {
  apiKeyName: string;
  expireType: ApiKeyExpireType;
  expireTime?: string;
  expireDays?: number;
  remark?: string;
}

export interface ApiKeyCreateResult {
  id: string;
  apiKeyName: string;
  apiKey: string;
  expireTime?: string;
  remark?: string;
}

import { del, get, post } from '@gwsu/core';
import type {
  ApiKeyCreateDTO,
  ApiKeyCreateResult,
  ApiKeyDetail,
  ApiKeyInfo,
  ApiKeyQuery,
  PageResult,
} from '../types';

export async function createApiKey(data: ApiKeyCreateDTO): Promise<ApiKeyCreateResult> {
  const res = await post<ApiKeyCreateResult>('/system/api-key/create', data);
  return res.data;
}

export async function getApiKeyPage(query: ApiKeyQuery): Promise<PageResult<ApiKeyInfo>> {
  const res = await post<PageResult<ApiKeyInfo>>('/system/api-key/page', query);
  return res.data;
}

export async function getApiKeyDetail(id: string): Promise<ApiKeyDetail> {
  const res = await get<ApiKeyDetail>(`/system/api-key/${id}`);
  return res.data;
}

export async function deleteApiKey(id: string): Promise<void> {
  await del<void>(`/system/api-key/${id}`);
}

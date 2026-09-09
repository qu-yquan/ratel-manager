import {del, get, post} from '@gwsu/core';
import type {
  OAuthClientEnums,
  OAuthClientInfo,
  OAuthClientPageResult,
  OAuthClientQuery,
  OAuthClientSecret,
  OAuthScopeInfo,
  OAuthScopePageResult,
  OAuthScopeQuery,
  ApiResourcePageResult,
} from '../types';

export async function getOAuthClientPage(query: OAuthClientQuery): Promise<OAuthClientPageResult> {
  const res = await post<OAuthClientPageResult>('/security/oauth/client/page', query);
  return res.data;
}

export async function getOAuthClientById(id: string): Promise<OAuthClientInfo> {
  const res = await get<OAuthClientInfo>(`/security/oauth/client/id/${id}`);
  return res.data;
}

export async function getOAuthClientEnums(): Promise<OAuthClientEnums> {
  const res = await get<OAuthClientEnums>('/security/oauth/client/enums');
  return res.data;
}

export async function saveOrUpdateOAuthClient(data: OAuthClientInfo): Promise<OAuthClientSecret> {
  const res = await post<OAuthClientSecret>('/security/oauth/client', data);
  return res.data;
}

export async function resetOAuthClientSecret(id: string): Promise<OAuthClientSecret> {
  const res = await post<OAuthClientSecret>(`/security/oauth/client/${id}/secret/reset`);
  return res.data;
}

export async function deleteOAuthClients(ids: string[]): Promise<boolean> {
  const res = await del<boolean>('/security/oauth/client', ids);
  return res.data;
}

export async function getOAuthScopePage(query: OAuthScopeQuery): Promise<OAuthScopePageResult> {
  const res = await post<OAuthScopePageResult>('/security/oauth/scope/page', query);
  return res.data;
}

export async function getOAuthScope(id: string): Promise<OAuthScopeInfo> {
  const res = await get<OAuthScopeInfo>(`/security/oauth/scope/${id}`);
  return res.data;
}

export async function getOAuthScopeOptions(accountType?: string): Promise<OAuthScopeInfo[]> {
  const suffix = accountType ? `?accountType=${encodeURIComponent(accountType)}` : '';
  const res = await get<OAuthScopeInfo[]>(`/security/oauth/scope/options${suffix}`);
  return res.data;
}

export async function saveOAuthScope(data: OAuthScopeInfo): Promise<boolean> {
  const res = await post<boolean>('/security/oauth/scope', data);
  return res.data;
}

export async function deleteOAuthScopes(ids: string[]): Promise<boolean> {
  const res = await del<boolean>('/security/oauth/scope', ids);
  return res.data;
}

export async function getApiResourcePage(query: {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}): Promise<ApiResourcePageResult> {
  const res = await post<ApiResourcePageResult>('/security/apiResource/page', query);
  return res.data;
}

export async function getApiResourcesByIds(ids: string[]): Promise<ApiResourceInfo[]> {
  if (!ids.length) return [];
  const res = await post<ApiResourceInfo[]>('/security/apiResource/list/by-ids', ids);
  return res.data;
}

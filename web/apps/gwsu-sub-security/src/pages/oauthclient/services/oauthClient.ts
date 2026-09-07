import {del, get, post} from '@gwsu/core';
import type {
  OAuthClientEnums,
  OAuthClientInfo,
  OAuthClientPageResult,
  OAuthClientQuery,
  OAuthClientSecret,
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

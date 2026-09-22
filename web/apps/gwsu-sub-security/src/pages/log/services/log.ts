import { get, post } from "@gwsu/core";

import type {
  KeyValueOption,
  LoginLogItem,
  LoginLogQuery,
  OperationLogItem,
  OperationLogQuery,
  PageResult,
} from "../types";

const LOGIN_BASE = "/log/log/login";
const OPERATION_BASE = "/log/log/operation";

export async function getLoginAccountTypes(): Promise<KeyValueOption[]> {
  const response = await get<KeyValueOption[]>(`${LOGIN_BASE}/account-types`);
  return response.data ?? [];
}

export async function getLoginLogPage(
  query: LoginLogQuery
): Promise<PageResult<LoginLogItem>> {
  const response = await post<PageResult<LoginLogItem>>(
    `${LOGIN_BASE}/page`,
    query
  );
  return response.data;
}

export async function getLoginLogById(id: string): Promise<LoginLogItem> {
  const response = await get<LoginLogItem>(`${LOGIN_BASE}/${id}`);
  return response.data;
}

export async function getOperationLogPage(
  query: OperationLogQuery
): Promise<PageResult<OperationLogItem>> {
  const response = await post<PageResult<OperationLogItem>>(
    `${OPERATION_BASE}/page`,
    query
  );
  return response.data;
}

export async function getOperationLogById(
  id: string
): Promise<OperationLogItem> {
  const response = await get<OperationLogItem>(`${OPERATION_BASE}/${id}`);
  return response.data;
}

export async function getOperationLogTreeByTid(
  tid: string
): Promise<OperationLogItem> {
  const response = await get<OperationLogItem>(`${OPERATION_BASE}/tid/${tid}`);
  return response.data;
}

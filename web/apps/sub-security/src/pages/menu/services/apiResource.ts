import { post } from '@gwsu/core';
import type { ApiResourceQuery, ApiResourcePageResult, ModuleInfo } from '../types';

/** 分页查询接口资源 */
export async function getApiResourcePage(
  query: ApiResourceQuery,
): Promise<ApiResourcePageResult> {
  const res = await post<ApiResourcePageResult>(
    '/security/apiResource/page',
    query,
  );
  return res.data;
}

/** 获取模块列表 */
export async function getModuleList(): Promise<ModuleInfo[]> {
  const res = await post<ModuleInfo[]>('/modules/list');
  return res.data;
}

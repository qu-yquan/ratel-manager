import { get, post, del } from '@gwsu/core';
import type {
  BusinessFunctionQuery,
  BusinessFunctionPageResult,
  BusinessFunctionDetail,
  BusinessFunctionInfo,
} from '../types';

export async function getBusinessFunctionPage(
  query: BusinessFunctionQuery,
): Promise<BusinessFunctionPageResult> {
  const res = await post<BusinessFunctionPageResult>(
    '/security/business-function/page',
    query,
  );
  return res.data;
}

export async function listAllBusinessFunctions(): Promise<BusinessFunctionInfo[]> {
  const res = await get<BusinessFunctionInfo[]>(
    '/security/business-function/listAll',
  );
  return res.data;
}

export async function getBusinessFunctionDetail(
  id: string,
): Promise<BusinessFunctionDetail> {
  const res = await get<BusinessFunctionDetail>(
    `/security/business-function/${id}`,
  );
  return res.data;
}

export async function saveOrUpdateBusinessFunction(
  data: BusinessFunctionInfo,
): Promise<string> {
  const res = await post<string>('/security/business-function', data);
  return res.data;
}

export async function batchDeleteBusinessFunctions(
  ids: string[],
): Promise<boolean> {
  const res = await del<boolean>('/security/business-function', ids);
  return res.data;
}

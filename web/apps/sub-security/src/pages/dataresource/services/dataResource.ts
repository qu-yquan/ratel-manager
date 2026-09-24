import { get, post } from '@gwsu/core';
import type {
  DataResourceInfo,
  DataResourceQuery,
  StringEnumOption,
  ResourceAttribute,
} from '../types';

const BASE = '/security/data-resource';

/** 分页查询数据资源 */
export async function getDataResourcePage(query: DataResourceQuery) {
  const res = await post<{
    records: DataResourceInfo[];
    total: number;
    size: number;
    current: number;
    pages: number;
  }>(`${BASE}/page`, query);
  return res.data;
}

/** 新增或更新数据资源 */
export async function saveOrUpdateDataResource(
  data: DataResourceInfo,
): Promise<boolean> {
  const res = await post<boolean>(BASE, data);
  return res.data;
}

/** 批量删除数据资源 */
export async function deleteDataResources(ids: string[]): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/delete`, ids);
  return res.data;
}

/** 同步数据资源规则到 Redis */
export async function syncToRedis(): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/sync`);
  return res.data;
}

/** 获取断言类型枚举选项 */
export async function getAssertTypeOptions(): Promise<StringEnumOption[]> {
  const res = await get<StringEnumOption[]>(`${BASE}/enums/assert-type`);
  return res.data ?? [];
}

/** 获取条件关联关系枚举选项 */
export async function getConditionTypeOptions(): Promise<StringEnumOption[]> {
  const res = await get<StringEnumOption[]>(`${BASE}/enums/condition-type`);
  return res.data ?? [];
}

/**
 * 获取用户数据资源属性列表
 * 注意：此接口来自 business-system 服务（BasicController），非本服务接口
 */
export async function getResourceAttributes(): Promise<ResourceAttribute[]> {
  const res = await get<ResourceAttribute[]>('/system/basic/dataResourceAttribute');
  return res.data ?? [];
}

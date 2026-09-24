import { get, post, put, del } from '@gwsu/core';
import type {
  EnumOption,
  MenuTreeNode,
  ButtonItem,
  MenuSaveRequest,
  MenuSortItem,
} from '../types';

/** 获取菜单所属类型枚举 */
export async function getMenuOwners(): Promise<EnumOption[]> {
  const res = await get<EnumOption[]>('/security/menu/enums/owners');
  return res.data;
}

/** 获取菜单位置类型枚举 */
export async function getMenuPositions(): Promise<EnumOption[]> {
  const res = await get<EnumOption[]>('/security/menu/enums/positions');
  return res.data;
}

/** 获取菜单树（目录+菜单） */
export async function getMenuTree(
  owner: number,
  position?: number,
): Promise<MenuTreeNode[]> {
  const data: Record<string, unknown> = { owner };
  if (position !== undefined) {
    data.position = position;
  }
  const res = await post<MenuTreeNode[]>('/security/menu/tree', data);
  return res.data;
}

/** 获取指定菜单下的按钮列表 */
export async function getMenuButtons(
  owner: number,
  menuId: string,
): Promise<ButtonItem[]> {
  const res = await get<ButtonItem[]>(
    `/security/menu/tree/${owner}/buttons/${menuId}`,
  );
  return res.data;
}

/** 获取菜单详情 */
export async function getMenuById(id: string): Promise<MenuTreeNode> {
  const res = await get<MenuTreeNode>(`/security/menu/${id}`);
  return res.data;
}

/** 新增或更新菜单 */
export async function saveOrUpdateMenu(
  data: MenuSaveRequest,
): Promise<boolean> {
  const res = await post<boolean>('/security/menu', data);
  return res.data;
}

/** 批量删除菜单 */
export async function deleteMenu(ids: string[]): Promise<boolean> {
  const res = await del<boolean>('/security/menu', ids);
  return res.data;
}

/** 批量更新菜单排序和父级 */
export async function batchSortMenu(sortItems: MenuSortItem[]): Promise<boolean> {
  const res = await put<boolean>('/security/menu/sort', sortItems);
  return res.data;
}

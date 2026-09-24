import { get, post, put, del } from '@gwsu/core';
import type {
  DeptTypeOption,
  DeptTreeNode,
  DeptDetail,
  DeptSaveRequest,
  UserDeptDetail,
  UserDeptSaveRequest,
  SetPrimaryDeptRequest,
  RemoveUserDeptRequest,
} from '@/pages/dept/types';

/** 获取部门类型列表 */
export async function getDeptTypes(): Promise<DeptTypeOption[]> {
  const res = await get<DeptTypeOption[]>('/system/dept/types');
  return res.data;
}

/** 获取部门树 */
export async function getDeptTree(): Promise<DeptTreeNode[]> {
  const res = await get<DeptTreeNode[]>('/system/dept/tree');
  return res.data;
}

/** 获取部门详情 */
export async function getDeptDetail(id: string): Promise<DeptDetail> {
  const res = await get<DeptDetail>(`/system/dept/${id}`);
  return res.data;
}

/** 保存部门（新增或更新） */
export async function saveDept(data: DeptSaveRequest): Promise<string> {
  const res = await post<string>('/system/dept', data);
  return res.data;
}

/** 删除部门 */
export async function deleteDept(id: string): Promise<void> {
  await del<void>(`/system/dept/${id}`);
}

/** 获取子部门列表 */
export async function getDeptChildren(parentId: string): Promise<DeptDetail[]> {
  const res = await get<DeptDetail[]>(`/system/dept/${parentId}/children`);
  return res.data;
}

/** 添加父部门 */
export async function addParentDept(id: string, parentId: string): Promise<void> {
  await post<void>(`/system/dept/${id}/parent/${parentId}`);
}

/** 移除父部门 */
export async function removeParentDept(id: string, parentId: string): Promise<void> {
  await del<void>(`/system/dept/${id}/parent/${parentId}`);
}

/** 获取部门下用户列表 */
export async function getDeptUsers(deptId: string): Promise<UserDeptDetail[]> {
  const res = await get<UserDeptDetail[]>(`/system/dept/${deptId}/users`);
  return res.data;
}

/** 设置用户部门 */
export async function setUserDept(data: UserDeptSaveRequest): Promise<void> {
  await post<void>('/system/user-dept', data);
}

/** 设置主部门 */
export async function setPrimaryDept(data: SetPrimaryDeptRequest): Promise<void> {
  await put<void>('/system/user-dept/primary', data);
}

/** 移除用户部门 */
export async function removeUserDept(data: RemoveUserDeptRequest): Promise<void> {
  await del<void>('/system/user-dept', data as unknown as Record<string, unknown>);
}

/** 获取用户所属部门 */
export async function getUserDepts(userId: string): Promise<UserDeptDetail[]> {
  const res = await get<UserDeptDetail[]>(`/system/user-dept/user/${userId}`);
  return res.data;
}

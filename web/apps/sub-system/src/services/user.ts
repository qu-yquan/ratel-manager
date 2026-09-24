import { get, post, put, del } from '@gwsu/core';
import type {
  SysUserVO,
  SysUserDetailVO,
  SysUserQueryDTO,
  SysAccountBindDTO,
  DeptUserCountMap,
  DingTalkAccountOption,
} from '@/pages/user/types';

/** 分页查询用户 */
export async function getUserPage(query: SysUserQueryDTO) {
  const res = await post<{ records: SysUserVO[]; total: number; current: number; size: number }>(
    '/system/manager/page',
    query,
  );
  return res.data;
}

/** 获取用户详情 */
export async function getUserDetail(id: string): Promise<SysUserDetailVO> {
  const res = await get<SysUserDetailVO>(`/system/manager/${id}`);
  return res.data;
}

/** 新增或编辑用户 */
export async function saveOrUpdateUser(data: SysUserVO): Promise<string> {
  const res = await post<string>('/system/manager', data);
  return res.data;
}

/** 启禁用用户 */
export async function updateUserStatus(id: string, status: number): Promise<void> {
  await put<void>(`/system/manager/${id}/status?status=${status}`);
}

/** 绑定账号 */
export async function bindAccount(userId: string, data: SysAccountBindDTO): Promise<void> {
  await post<void>(`/system/manager/${userId}/account`, data);
}

/** 解绑账号 */
export async function unbindAccount(userId: string, accountId: string): Promise<void> {
  await del<void>(`/system/manager/${userId}/account/${accountId}`);
}

/** 获取可绑定的钉钉账号列表 */
export async function getBindableDingTalkAccounts(): Promise<DingTalkAccountOption[]> {
  const res = await get<DingTalkAccountOption[]>('/system/manager/dingtalk/bindable');
  return res.data ?? [];
}

/** 获取各部门用户数量 */
export async function getDeptUserCount(): Promise<DeptUserCountMap> {
  const res = await get<DeptUserCountMap>('/system/dept/user-count');
  return res.data;
}

/** 批量删除用户 */
export async function batchDeleteUsers(ids: string[]): Promise<void> {
  await del<void>('/system/manager', ids);
}

/** 重置用户密码 */
export async function resetPassword(userId: string, newPassword: string): Promise<void> {
  await put<void>(`/system/manager/${userId}/password`, { newPassword });
}

/** 给用户分配角色 */
export async function allocateRoles(userId: string, roleIds: string[]): Promise<void> {
  await put<void>(`/security/role/allocationRole/${userId}`, roleIds);
}

/** 查询用户已分配的角色编码列表 */
export async function getUserRoleCodes(subjectId: string): Promise<string[]> {
  const res = await get<{ dataScope: unknown; roles: string[] }>(`/security/role/list/${subjectId}`);
  return res.data?.roles ?? [];
}

/** 查询角色全量列表（启用状态） */
export async function getEnabledRoleList(): Promise<RoleOption[]> {
  const res = await get<RoleOption[]>('/security/role/list', { status: 1 });
  return res.data ?? [];
}

/** 角色选项（用于用户分配角色） */
export interface RoleOption {
  id: string;
  roleName: string;
  roleCode: string;
  status: boolean;
}

/** 部门类型选项 */
export interface DeptTypeOption {
  code: number;
  name: string;
}

/** 部门树节点 */
export interface DeptTreeNode {
  id: string;
  name: string;
  type: number;
  enabled: boolean;
  parentId: string | null;
  children?: DeptTreeNode[];
}

/** 部门详情 */
export interface DeptDetail {
  id: string;
  name: string;
  type: number;
  parentId: string | null;
  parentName?: string;
  parentIds?: string[];
  extraParents?: { id: string; name: string }[];
  enabled: boolean;
  sort: number;
  path: string;
}

/** 用户部门详情 */
export interface UserDeptDetail {
  userId: string;
  username: string;
  nickname: string;
  deptId: string;
  deptName: string;
  isPrimary: boolean;
}

/** 部门保存请求 */
export interface DeptSaveRequest {
  id?: string;
  name: string;
  type: number;
  parentId?: string;
  enabled?: boolean;
  sort?: number;
}

/** 设置用户部门请求 */
export interface UserDeptSaveRequest {
  userId: string;
  deptIds: string[];
  primaryDeptId?: string;
}

/** 设置主部门请求 */
export interface SetPrimaryDeptRequest {
  userId: string;
  deptId: string;
}

/** 移除用户部门请求 */
export interface RemoveUserDeptRequest {
  userId: string;
  deptIds: string[];
  newPrimaryDeptId?: string;
}

/** 部门类型颜色映射 */
export const DEPT_TYPE_COLORS: Record<number, string> = {
  1: '#1677ff',
  2: '#13c2c2',
  3: '#52c41a',
  4: '#faad14',
  5: '#722ed1',
};

/** 部门类型名称映射 */
export const DEPT_TYPE_NAMES: Record<number, string> = {
  1: '公司',
  2: '分公司',
  3: '部门',
  4: '小组',
  5: '虚拟团队',
};

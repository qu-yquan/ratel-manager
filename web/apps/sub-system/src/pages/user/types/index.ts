/** 用户信息 */
export interface SysUserVO {
  userId: string;
  userName: string;
  nickname: string;
  avatar: string;
  email: string;
  phone: string;
  gender: number;
  status: number;
  lastLoginTime: string;
  password?: string;
  deptId?: string;
  deptName?: string;
}

/** 账号信息 */
export interface SysAccountVO {
  id: string;
  userId: string;
  identityType: string;
  identifier: string;
  status: number;
  verified: boolean;
  verifiedTime: string | null;
  bindTime: string | null;
}

/** 用户部门关联 */
export interface SysUserDeptVO {
  id: string;
  deptId: string;
  deptName: string;
  isPrimary: boolean;
}

/** 用户详情 */
export interface SysUserDetailVO extends SysUserVO {
  accounts: SysAccountVO[];
  depts: SysUserDeptVO[];
}

/** 用户查询条件 */
export interface SysUserQueryDTO {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  deptId?: string;
}

/** 账号绑定 */
export interface SysAccountBindDTO {
  identityType: string;
  identifier: string;
  credential?: string;
  /** 绑定钉钉时，原钉钉账号所属用户ID（用于切换绑定，删除原用户） */
  originalUserId?: string;
}

/** 可绑定的钉钉账号选项 */
export interface DingTalkAccountOption {
  id: string;
  identifier: string;
  userId: string;
  nickname: string;
  userName: string;
}

/** 部门用户数统计 */
export interface DeptUserCountMap {
  [deptId: string]: number;
}

/** 登录类型映射 */
export const IDENTITY_TYPE_MAP: Record<string, { label: string; icon: string }> = {
  password: { label: '用户名密码', icon: '\u{1F511}' },
  phone: { label: '手机号登录', icon: '\u{1F4F1}' },
  dingtalk: { label: '钉钉登录', icon: '\u{1F4AC}' },
};

/** 性别映射 */
export const GENDER_MAP: Record<number, string> = {
  0: '未知',
  1: '男',
  2: '女',
};

/** 用户状态映射 */
export const USER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '禁用', color: '#ff4d4f' },
  1: { text: '启用', color: '#52c41a' },
};

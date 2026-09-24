/** 枚举选项 */
export interface EnumOption {
  code: number;
  description: string;
}

/** 菜单树节点 */
export interface MenuTreeNode {
  id: string;
  parentId: string | null;
  menuName: string;
  menuType: number;
  sort: number;
  icon: string | null;
  path: string | null;
  microApp: string | null;
  visible: boolean;
  status: boolean;
  permission: string | null;
  buttonKey: string | null;
  description: string | null;
  position: number | null;
  owner: number | null;
  children?: MenuTreeNode[];
}

/** 菜单详情（与树节点结构一致） */
export type MenuDetail = MenuTreeNode;

/** 按钮项 */
export type ButtonItem = MenuTreeNode;

/** 菜单保存/更新请求 */
export interface MenuSaveRequest {
  id?: string;
  parentId?: string | null;
  menuName: string;
  menuType: number;
  sort?: number;
  icon?: string;
  path?: string;
  microApp?: string;
  visible?: boolean;
  status?: boolean;
  permission?: string;
  buttonKey?: string;
  description?: string;
  position?: number;
  owner?: number;
}

/** 接口资源项 */
export interface ApiResourceItem {
  id: string;
  modulePrefix: string;
  tagName: string;
  reqPath: string;
  reqMethod: string;
  summary: string;
  loginAllowAccess: number;
}

/** 接口资源分页查询参数 */
export interface ApiResourceQuery {
  pageNum: number;
  pageSize: number;
  modulePrefix?: string;
  tagName?: string;
  reqPath?: string;
  keyword?: string;
}

/** 接口资源分页结果 */
export interface ApiResourcePageResult {
  records: ApiResourceItem[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 菜单排序项 */
export interface MenuSortItem {
  id: string;
  parentId: string;
  sort: number;
}

/** 模块信息 */
export interface ModuleInfo {
  prefix: string;
  applicationName: string;
  note: string;
}

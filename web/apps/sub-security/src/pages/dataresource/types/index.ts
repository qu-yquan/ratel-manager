/** 数据资源配置 */
export interface DataResourceInfo {
  id?: string;
  catalogName?: string;
  schemaName?: string;
  tableName: string;
  description?: string;
  supportSelfOnly?: boolean;
  selfOnlyField?: string;
  status: boolean;
  conditions?: DataResourceCondition[];
  createTime?: string;
}

/** 数据资源字段条件 */
export interface DataResourceCondition {
  id?: string;
  fieldName: string;
  showNull: boolean;
  userResourceFields: string[];
  assertType: string;
  relationship?: string;
  sort: number;
}

/** 数据资源查询条件 */
export interface DataResourceQuery {
  tableName?: string;
  catalogName?: string;
  schemaName?: string;
  status?: boolean;
  pageNum?: number;
  pageSize?: number;
}

/** 用户资源属性（从 business-system 获取） */
export interface ResourceAttribute {
  key: string;
  desc: string;
}

/** 枚举选项（字符串值） */
export interface StringEnumOption {
  label: string;
  value: string;
}

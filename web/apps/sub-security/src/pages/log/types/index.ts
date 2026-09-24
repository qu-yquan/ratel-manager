export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}

export interface KeyValueOption {
  key: string;
  value: string;
}

export interface LoginLogItem {
  id: string;
  authorizationId?: string;
  accountType?: string;
  visitorType?: string;
  loginType?: string;
  grantType?: string;
  clientId?: string;
  userId?: string;
  userName?: string;
  loginAccount?: string;
  endType?: "LOGOUT" | "EXPIRE";
  terminal?: string;
  terminalDetail?: string;
  clientIp?: string;
  status?: boolean;
  failureCode?: string;
  failureMessage?: string;
  loginTime?: string;
  endTime?: string;
  createTime?: string;
}

export interface LoginLogQuery extends PageQuery {
  accountType: string;
  loginAccount?: string;
  userName?: string;
  clientIp?: string;
  loginType?: string;
  status?: number;
  loginTimeStart?: string;
  loginTimeEnd?: string;
}

export interface OperationLogItem {
  operId: string;
  authorizationId?: string;
  tid?: string;
  parentId?: string;
  modulePrefix?: string;
  fromApp?: string;
  apiModule?: string;
  menuId?: string;
  operSubject?: number;
  apiDescription?: string;
  method?: string;
  requestUrl?: string;
  requestMethod?: string;
  terminal?: string;
  terminalDetail?: string;
  operName?: string;
  requestParam?: string;
  responseData?: string;
  errorMsg?: string;
  status?: boolean;
  requestTime?: string;
  responseTime?: string;
  consumeMill?: number;
  hasChildren?: boolean;
  children?: OperationLogItem[];
}

export interface OperationLogQuery extends PageQuery {
  authorizationId?: string;
  tid?: string;
  modulePrefix?: string;
  operName?: string;
  requestUrl?: string;
  requestMethod?: string;
  status?: number;
  requestTimeStart?: string;
  requestTimeEnd?: string;
}

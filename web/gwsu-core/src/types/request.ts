/**
 * HTTP 请求工具类型定义
 */

/** 请求配置 */
export interface RequestOptions {
  /** 请求地址 */
  url: string;
  /** 请求方法 */
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  /** 请求参数 */
  params?: Record<string, unknown>;
  /** 请求体 */
  data?: Record<string, unknown> | unknown;
  /** 请求头 */
  headers?: Record<string, string>;
  /** 超时时间(ms) */
  timeout?: number;
  /** 是否显示错误提示 */
  showError?: boolean;
  /** 是否显示加载状态 */
  showLoading?: boolean;
  /** 是否跳过登录 Token 注入 */
  skipAuth?: boolean;
  /** 收到 401 时是否跳过全局登录失效处理 */
  skipUnauthorizedRedirect?: boolean;
  /** 自定义错误提示信息 */
  errorMessage?: string;
}

/** 响应结构 */
export interface ApiResponse<T = unknown> {
  /** 状态码 */
  code: number;
  /** 响应消息 */
  msg: string;
  /** 响应数据 */
  data: T;
  /** 时间戳 */
  timestamp?: number;
  /** 错误码 */
  errCode?: string;
}

/** 分页请求参数 */
export interface PaginationParams {
  /** 当前页码 */
  current?: number;
  /** 每页条数 */
  pageSize?: number;
  /** 排序字段 */
  sortField?: string;
  /** 排序方式 */
  sortOrder?: 'asc' | 'desc';
}

/** 分页响应数据 */
export interface PaginationData<T> {
  /** 数据列表 */
  list: T[];
  /** 总条数 */
  total: number;
  /** 当前页码 */
  current: number;
  /** 每页条数 */
  pageSize: number;
}

/** 请求拦截器 */
export type RequestInterceptor = (
  config: RequestOptions
) => RequestOptions | Promise<RequestOptions>;

/** 响应拦截器 */
export type ResponseInterceptor<T = unknown> = (
  response: ApiResponse<T>
) => ApiResponse<T> | Promise<ApiResponse<T>>;

/** 错误拦截器 */
export type ErrorInterceptor = (error: Error) => void | Promise<void>;

/** 下载请求配置 */
export interface DownloadRequestOptions {
  url: string;
  headers?: Record<string, string>;
  timeout?: number;
  showError?: boolean;
}

/** 请求错误类，包含错误码 */
export interface RequestError extends Error {
  /** 错误码 */
  errCode?: string;
}

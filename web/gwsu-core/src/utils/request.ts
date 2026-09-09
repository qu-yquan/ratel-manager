/**
 * HTTP 请求工具
 * 统一的请求封装，支持拦截器、错误处理等
 */

import type {
    ApiResponse,
    DownloadRequestOptions,
    ErrorInterceptor,
    RequestError,
    RequestInterceptor,
    RequestOptions,
    ResponseInterceptor,
} from '../types';
import {emitEvent, EventType} from '../constants';
import {useHeadlessStore} from '../stores/headlessStore';
import {useUserStore} from '../stores/userStore';

// 存储拦截器
const requestInterceptors: RequestInterceptor[] = [];
const responseInterceptors: ResponseInterceptor[] = [];
const errorInterceptors: ErrorInterceptor[] = [];

type InternalApiResponse<T = unknown> = ApiResponse<T> & {
    httpStatus?: number;
    skipUnauthorizedRedirect?: boolean;
};

// 默认配置
const defaultConfig = {
    baseURL: '/api',
    timeout: 30000,
    showError: true,
    showLoading: false,
};

let config = {...defaultConfig};

// 错误提示回调（可由应用层注入，支持错误码；null 时回退到默认实现）
let showErrorToast: ((message: string, errCode?: string) => void) | null = null;

// 默认错误提示实现（仅当 showErrorToast 未注入时使用，降级到 console）
function defaultShowErrorToast(msg: string, errCode?: string | undefined): void {
    console.error('[Request Error]', msg, errCode ? `(错误码: ${errCode})` : '');
}

// 是否已初始化
let initialized = false;


/**
 * 设置错误提示回调函数
 * 应用层可以注入自己的提示组件
 */
export function setErrorToastHandler(handler: (message: string, errCode?: string) => void) {
    showErrorToast = handler;
}

/**
 * 设置全局配置
 */
export function setRequestConfig(options: Partial<typeof defaultConfig>) {
    config = {...config, ...options};
}

/**
 * 获取当前配置
 */
export function getRequestConfig() {
    return {...config};
}

/**
 * 添加请求拦截器
 */
export function addRequestInterceptor(interceptor: RequestInterceptor) {
    requestInterceptors.push(interceptor);
}

/**
 * 添加响应拦截器
 */
export function addResponseInterceptor(interceptor: ResponseInterceptor) {
    responseInterceptors.push(interceptor);
}

/**
 * 添加错误拦截器
 */
export function addErrorInterceptor(interceptor: ErrorInterceptor) {
    errorInterceptors.push(interceptor);
}

/**
 * 清除所有拦截器
 */
export function clearInterceptors() {
    requestInterceptors.length = 0;
    responseInterceptors.length = 0;
    errorInterceptors.length = 0;
}

/**
 * 初始化默认拦截器
 * 模块加载时自动执行
 */
function initDefaultInterceptors() {
    if (initialized) return;

    // 默认请求拦截器 - 添加 token
    addRequestInterceptor((options) => {
        if (options.skipAuth) {
            return options;
        }
        const tokenInfo = useUserStore.getState().getTokenInfo();
        if (tokenInfo?.token) {
            options.headers = {
                ...options.headers,
                Authorization: `Bearer ${tokenInfo.token}`,
            };
        }
        return options;
    });

    // 默认响应拦截器 - 处理业务错误
    addResponseInterceptor((response) => {
        // 获取 HTTP 状态码
        const internalResponse = response as InternalApiResponse;
        const httpStatus = internalResponse.httpStatus || 200;

        // 处理 HTTP 500 等服务端错误
        if (httpStatus >= 500) {
            const resultObj = response as unknown as Record<string, unknown>;
            const errorMsg = resultObj.msg || '服务器内部错误';
            const errCode = resultObj.errCode as string | undefined;
            const error = new Error(errorMsg as string) as RequestError;
            if (errCode) {
                error.errCode = errCode;
            }
            throw error;
        }

        // 根据后端返回的 code 判断请求是否成功
        if (response.code === 0 || response.code === 200) {
            return response;
        }

        // 处理常见的业务错误码
        if (response.code === 401) {
            if (internalResponse.skipUnauthorizedRedirect) {
                throw new Error(response.msg || '请求失败');
            }
            // 未授权，清除用户数据并跳转登录
            useUserStore.getState().clearUserData();
            // 清除无头浏览器 threadId
            useHeadlessStore.getState().clearThreadId();
            // 使用事件枚举发送事件
            emitEvent(EventType.TOKEN_EXPIRED);
            throw new Error('登录已过期，请重新登录');
        }

        // 其他业务错误
        const error = new Error(response.msg || '请求失败') as RequestError;
        if (response.errCode) {
            error.errCode = response.errCode;
        }
        throw error;
    });

    // 默认错误拦截器 - 仅做日志记录，弹窗由 handleError 统一处理
    addErrorInterceptor((error) => {
        const reqError = error as RequestError;
        console.error('[Request Error]', error.message);
        if (reqError.errCode) {
            console.error('[Request Error] errCode:', reqError.errCode);
        }
    });

    initialized = true;
}

// 模块加载时自动初始化
initDefaultInterceptors();

/**
 * 处理错误
 */
async function handleError(error: Error, showError: boolean): Promise<void> {
    // 执行错误拦截器（仅做日志等处理）
    for (const interceptor of errorInterceptors) {
        try {
            await interceptor(error);
        } catch (e) {
            console.error('Error in error interceptor:', e);
        }
    }

    // 统一错误提示
    if (showError) {
        const reqError = error as RequestError;
        if (showErrorToast) {
            showErrorToast(error.message, reqError.errCode);
        } else {
            defaultShowErrorToast(error.message, reqError.errCode);
        }
        // 标记已处理，防止 unhandledrejection 触发 react-error-overlay 导致白屏
        (error as RequestError & { _requestHandled?: boolean })._requestHandled = true;
    }
}

/**
 * 发起请求 - 使用 fetch 实现
 */
async function fetchRequest<T>(options: RequestOptions): Promise<ApiResponse<T>> {
    const {
        url,
        method = 'GET',
        params,
        data,
        headers = {},
        timeout = config.timeout,
    } = options;

    // 构建完整 URL
    let fullUrl = config.baseURL + url;

    // 处理查询参数
    if (params && Object.keys(params).length > 0) {
        const searchParams = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                searchParams.append(key, String(value));
            }
        });
        fullUrl += `?${searchParams.toString()}`;
    }

    // 构建请求配置
    const isFormData = typeof FormData !== 'undefined' && data instanceof FormData;
    const fetchOptions: RequestInit = {
        method,
        headers: isFormData ? { ...headers } : {
            'Content-Type': 'application/json',
            ...headers,
        },
    };

    // 处理请求体
    if (data && method !== 'GET') {
        fetchOptions.body = isFormData ? (data as FormData) : JSON.stringify(data);
    }

    // 创建超时控制器
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);
    fetchOptions.signal = controller.signal;

    try {
        const response = await fetch(fullUrl, fetchOptions);
        clearTimeout(timeoutId);

        // 尝试解析响应体
        let result: ApiResponse<T>;
        const responseText = await response.text();
        try {
            result = responseText ? JSON.parse(responseText) : {};
        } catch {
            // JSON 解析失败，使用 HTTP 错误信息
            if (!response.ok) {
                throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
            }
            throw new Error('响应格式错误');
        }

        // 将 HTTP 状态码附加到响应对象上，供拦截器使用
        const internalResult = result as InternalApiResponse<T>;
        internalResult.httpStatus = response.status;
        internalResult.skipUnauthorizedRedirect = options.skipUnauthorizedRedirect;

        return result;
    } catch (error) {
        clearTimeout(timeoutId);
        if (error instanceof Error) {
            if (error.name === 'AbortError') {
                throw new Error('请求超时');
            }
            throw error;
        }
        throw new Error('未知错误');
    }
}

/**
 * 通用请求方法
 */
export async function request<T = unknown>(
    options: RequestOptions
): Promise<ApiResponse<T>> {
    try {
        // 执行请求拦截器
        let processedOptions = options;
        for (const interceptor of requestInterceptors) {
            processedOptions = await interceptor(processedOptions);
        }

        // 发起请求
        let response = await fetchRequest<T>(processedOptions);

        // 执行响应拦截器
        for (const interceptor of responseInterceptors) {
            response = (await interceptor(response)) as ApiResponse<T>;
        }

        return response;
    } catch (error) {
        await handleError(error as Error, options.showError ?? config.showError);
        throw error;
    }
}

/**
 * GET 请求
 */
export function get<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    options?: Partial<RequestOptions>
): Promise<ApiResponse<T>> {
    return request<T>({
        url,
        method: 'GET',
        params,
        ...options,
    });
}

/**
 * POST 请求
 */
export function post<T = unknown>(
    url: string,
    data?: Record<string, unknown> | unknown,
    options?: Partial<RequestOptions>
): Promise<ApiResponse<T>> {
    return request<T>({
        url,
        method: 'POST',
        data,
        ...options,
    });
}

/**
 * PUT 请求
 */
export function put<T = unknown>(
    url: string,
    data?: Record<string, unknown> | unknown,
    options?: Partial<RequestOptions>
): Promise<ApiResponse<T>> {
    return request<T>({
        url,
        method: 'PUT',
        data,
        ...options,
    });
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(
    url: string,
    data?: Record<string, unknown> | unknown,
    options?: Partial<RequestOptions>
): Promise<ApiResponse<T>> {
    return request<T>({
        url,
        method: 'DELETE',
        data,
        ...options,
    });
}

/**
 * PATCH 请求
 */
export function patch<T = unknown>(
    url: string,
    data?: Record<string, unknown> | unknown,
    options?: Partial<RequestOptions>
): Promise<ApiResponse<T>> {
    return request<T>({
        url,
        method: 'PATCH',
        data,
        ...options,
    });
}

/**
 * 下载请求 - 返回原始 Response，支持 Range 等场景
 * 复用请求拦截器（Token 注入）、URL 构建、超时控制
 * 跳过 JSON 解析和响应拦截器，直接返回原始 Response
 */
export async function downloadRequest(options: DownloadRequestOptions): Promise<Response> {
    const { url, headers = {}, timeout = config.timeout } = options;

    try {
        let processedOptions: RequestOptions = {
            url,
            method: 'GET',
            headers,
            timeout,
        };
        for (const interceptor of requestInterceptors) {
            processedOptions = await interceptor(processedOptions);
        }

        const fullUrl = config.baseURL + processedOptions.url;
        const fetchOptions: RequestInit = {
            method: 'GET',
            headers: processedOptions.headers,
        };

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), processedOptions.timeout);
        fetchOptions.signal = controller.signal;

        const response = await fetch(fullUrl, fetchOptions);
        clearTimeout(timeoutId);

        if (!response.ok && response.status !== 206) {
            throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        return response;
    } catch (error) {
        await handleError(error as Error, options.showError ?? config.showError);
        throw error;
    }
}

// 导出默认对象
export default {
    request,
    get,
    post,
    put,
    del,
    patch,
    downloadRequest,
    setRequestConfig,
    getRequestConfig,
    setErrorToastHandler,
    addRequestInterceptor,
    addResponseInterceptor,
    addErrorInterceptor,
    clearInterceptors,
};

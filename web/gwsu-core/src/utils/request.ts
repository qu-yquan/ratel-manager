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
import {aiRequestErrorPool} from '../stores/aiRequestErrorStore';

interface RequestObservation {
    requestStartedAt?: number;
    method?: string;
    path?: string;
    httpStatus?: number;
}

function requestFailure(message: string, metadata: Partial<RequestError>): RequestError {
    return Object.assign(new Error(message), metadata);
}

/** 仅输出诊断摘要，不收集请求体、请求头和响应体。 */
function sanitizeDiagnostic(value: string): string {
    return value
        .replace(/Bearer\s+[^\s"'<>]+/gi, 'Bearer [已隐藏]')
        .replace(/([?&][^=\s]+)=([^&\s"'<>]*)/g, '$1=[已隐藏]')
        .replace(/((?:token|password|secret|authorization|cookie)["']?\s*[:=]\s*["']?)[^\s,;"'<>]+/gi, '$1[已隐藏]')
        .slice(0, 500);
}

function captureRequestError(error: unknown, observation: RequestObservation, capture = true): void {
    if (!capture || observation.requestStartedAt === undefined) return;
    try {
        const failure = error as RequestError;
        if (failure?.name === 'AbortError') return;
        aiRequestErrorPool.append({
            id: crypto.randomUUID(),
            method: observation.method ?? 'GET',
            path: sanitizeDiagnostic(new URL(observation.path ?? '/', window.location.href).pathname),
            requestStartedAt: observation.requestStartedAt,
            failedAt: Date.now(),
            type: failure?.type ?? 'network',
            httpStatus: failure?.httpStatus ?? observation.httpStatus,
            businessCode: failure?.businessCode,
            errCode: failure?.errCode ? sanitizeDiagnostic(failure.errCode) : undefined,
            message: sanitizeDiagnostic(failure?.message || '未知请求错误'),
        });
    } catch {
        // 诊断失败不能覆盖原始请求异常。
    }
}

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

        const metadata: Partial<RequestError> = {
            type: httpStatus < 200 || httpStatus >= 300 ? 'http' : 'business',
            httpStatus,
            businessCode: response.code,
            errCode: response.errCode,
        };

        // HTTP 状态优先于业务成功码；保留现有登录失效行为。
        if (httpStatus === 401 || response.code === 401) {
            if (internalResponse.skipUnauthorizedRedirect) {
                throw requestFailure(response.msg || '请求失败', metadata);
            }
            useUserStore.getState().clearUserData();
            useHeadlessStore.getState().clearThreadId();
            emitEvent(EventType.TOKEN_EXPIRED);
            throw requestFailure('登录已过期，请重新登录', metadata);
        }
        if (httpStatus < 200 || httpStatus >= 300) {
            throw requestFailure(response.msg || `HTTP Error: ${httpStatus}`, metadata);
        }
        if (response.code === 0 || response.code === 200) return response;
        throw requestFailure(response.msg || '请求失败', metadata);
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
async function fetchRequest<T>(options: RequestOptions, observation: RequestObservation): Promise<ApiResponse<T>> {
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
        Object.assign(observation, { requestStartedAt: Date.now(), method: fetchOptions.method, path: fullUrl });
        const response = await fetch(fullUrl, fetchOptions);
        observation.httpStatus = response.status;

        // 尝试解析响应体
        let result: ApiResponse<T>;
        const responseText = await response.text();
        try {
            result = responseText ? JSON.parse(responseText) : {};
        } catch {
            // JSON 解析失败，使用 HTTP 错误信息
            if (!response.ok) {
                throw requestFailure(`HTTP Error: ${response.status} ${response.statusText}`, { type: 'http', httpStatus: response.status });
            }
            throw requestFailure('响应格式错误', { type: 'parse', httpStatus: response.status });
        }

        if (!result || typeof result !== 'object' || Array.isArray(result)) {
            throw requestFailure('响应格式错误', {
                type: response.ok ? 'parse' : 'http', httpStatus: response.status,
            });
        }

        // 将 HTTP 状态码附加到响应对象上，供拦截器使用
        const internalResult = result as InternalApiResponse<T>;
        internalResult.httpStatus = response.status;
        internalResult.skipUnauthorizedRedirect = options.skipUnauthorizedRedirect;

        return result;
    } catch (error) {
        if (error instanceof Error) {
            if (error.name === 'AbortError' && controller.signal.aborted) {
                throw requestFailure('请求超时', { type: 'timeout', httpStatus: observation.httpStatus });
            }
            throw error;
        }
        throw new Error('未知错误');
    } finally {
        clearTimeout(timeoutId);
    }
}

/**
 * 通用请求方法
 */
export async function request<T = unknown>(
    options: RequestOptions
): Promise<ApiResponse<T>> {
    const observation: RequestObservation = {};
    let processedOptions = options;
    try {
        // 执行请求拦截器
        for (const interceptor of requestInterceptors) {
            processedOptions = await interceptor(processedOptions);
        }

        // 发起请求
        let response = await fetchRequest<T>(processedOptions, observation);

        // 执行响应拦截器
        for (const interceptor of responseInterceptors) {
            response = (await interceptor(response)) as ApiResponse<T>;
        }

        return response;
    } catch (error) {
        captureRequestError(error, observation, options.captureAiError !== false && processedOptions.captureAiError !== false);
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
    const observation: RequestObservation = {};
    let processedOptions: RequestOptions = {
        ...options, method: 'GET', timeout: options.timeout ?? config.timeout,
    };
    let timeoutId: ReturnType<typeof setTimeout> | undefined;
    const controller = new AbortController();
    try {
        for (const interceptor of requestInterceptors) {
            processedOptions = await interceptor(processedOptions);
        }
        const fullUrl = config.baseURL + processedOptions.url;
        timeoutId = setTimeout(() => controller.abort(), processedOptions.timeout);
        Object.assign(observation, { requestStartedAt: Date.now(), method: 'GET', path: fullUrl });
        const response = await fetch(fullUrl, {
            method: 'GET', headers: processedOptions.headers, signal: controller.signal,
        });
        observation.httpStatus = response.status;
        if (!response.ok) {
            throw requestFailure(`HTTP Error: ${response.status} ${response.statusText}`, {
                type: 'http', httpStatus: response.status,
            });
        }
        return response;
    } catch (error) {
        const failure = controller.signal.aborted
            ? requestFailure('请求超时', { type: 'timeout', httpStatus: observation.httpStatus })
            : error;
        captureRequestError(failure, observation, options.captureAiError !== false && processedOptions.captureAiError !== false);
        await handleError(failure as Error, options.showError ?? config.showError);
        throw failure;
    } finally {
        clearTimeout(timeoutId);
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

/**
 * 登录相关 API 服务
 */

import {get, post} from '@gwsu/core';

const ANONYMOUS_REQUEST_OPTIONS = {
    skipAuth: true,
    skipUnauthorizedRedirect: true,
};

/**
 * 登录响应数据
 */
export interface LoginToken {
    /** 用户ID */
    userId: string;
    /** 登录token */
    token: string;
    /** 有效期（秒） */
    expires: number;
    /** 告警消息 */
    alterMsg?: string;
    /** 扩展数据 */
    extraData?: Record<string, string>;
}

/**
 * 终端类型
 */
export enum TerminalType {
    PC = 'PC',
    WEB = 'WEB',
    MOBILE = 'MOBILE',
    APP = 'APP',
    HD = 'HD',
}

/**
 * 登录请求参数
 */
export interface LoginParams {
    /** 登录类型 */
    type: string;
    /** 终端类型 */
    terminal: TerminalType;
    /** 用户名 */
    username: string;
    /** 密码 */
    password: string;
    /** 验证码ID */
    captchaId?: string;
    /** 验证码Code */
    captchaCode?: string;
}

/**
 * 验证码类型
 */
export enum CaptchaType {
    BLOCK_PUZZLE = 'BLOCK_PUZZLE',
    CLICK_WORD = 'CLICK_WORD',
}

export interface CaptchaData {
    captchaId: string;
    captchaType: string;
    originalImageBase64?: string;
    jigsawImageBase64?: string;
    secretKey?: string;
    token?: string;
    wordList?: string[];
}

export interface CaptchaGetResponse {
    type: CaptchaType;
    captchaId: string;
    expireSeconds: number;
    verificationExpireSeconds: number;
    data: CaptchaData;
}

export interface CaptchaCheckResponse {
    captchaId: string;
    captchaCode: string;
    extraData?: Record<string, unknown>;
}

export interface CaptchaCheckParams {
    captchaId: string;
    captchaCode: string;
    pointJson: string;
}

export type DingTalkCompleteMethod = 'binding' | 'create';

export interface DingTalkCompleteFields {
    bindingToken?: string;
    username?: string;
    password?: string;
}

export interface DingTalkCompleteParams {
    type: 'dingtalk';
    terminal: TerminalType.WEB;
    extraParam: Record<string, string[]>;
}

/**
 * 无头浏览器快速登录请求参数
 */
export interface HeadlessLoginParams {
  /** 登录类型 */
  type: "headless";
  /** 终端类型 */
  terminal: TerminalType.PC;
  /** 临时认证凭证 */
  certificationKey: string;
}

/**
 * 用户登录
 * @param params 登录参数
 */
export async function login(params: LoginParams): Promise<LoginToken> {
    const response = await post<LoginToken>(
        `/system/auth/login/manager`,
        params,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

/**
 * 获取登录验证码
 */
export async function getCaptcha(type?: CaptchaType): Promise<CaptchaGetResponse> {
    const response = await get<CaptchaGetResponse>(
        '/system/auth/captcha/get',
        type ? {type} : undefined,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

/**
 * 一次校验验证码，成功后返回登录验证码Code
 */
export async function checkCaptcha(params: CaptchaCheckParams): Promise<CaptchaCheckResponse> {
    const response = await post<CaptchaCheckResponse>(
        '/system/auth/captcha/check',
        params,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

export function buildDingTalkCompleteParams(
    method: DingTalkCompleteMethod,
    temporaryVoucher: string,
    fields: DingTalkCompleteFields,
): DingTalkCompleteParams {
    const extraParam: Record<string, string[]> = {
        createMethod: [method],
        temporaryVoucher: [temporaryVoucher],
    };

    if (method === 'binding' && fields.bindingToken) {
        extraParam.bindingToken = [fields.bindingToken];
    }
    if (method === 'create') {
        if (fields.username) {
            extraParam.username = [fields.username];
        }
        if (fields.password) {
            extraParam.password = [fields.password];
        }
    }

    return {
        type: 'dingtalk',
        terminal: TerminalType.WEB,
        extraParam,
    };
}

export async function completeDingTalkLogin(params: DingTalkCompleteParams): Promise<LoginToken> {
    const response = await post<LoginToken>(
        '/system/auth/login/manager',
        params,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

/**
 * 无头浏览器快速登录
 * @param params 无头登录参数
 */
export async function headlessLogin(params: HeadlessLoginParams): Promise<LoginToken> {
    const response = await post<LoginToken>(
        `/system/auth/login/manager`,
        params,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

/**
 * 钉钉授权地址响应
 */
export interface DingTalkAuthUrl {
    /** 授权跳转地址 */
    url: string;
    /** 扩展数据 */
    extraData?: Record<string, string>;
}

/**
 * 获取钉钉快捷登录授权地址
 */
export async function getDingTalkAuthUrl(): Promise<string> {
    const response = await get<DingTalkAuthUrl>(
        `/system/auth/url/manager/dingtalk`,
        undefined,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data.url;
}

/**
 * 登录页基础配置信息
 */
export interface LoginConfigInfo {
    /** 项目名称 */
    projectName: string;
}

/**
 * 获取登录页基础配置信息（无需登录即可调用）
 */
export async function getLoginConfigInfo(): Promise<LoginConfigInfo> {
    const response = await get<LoginConfigInfo>(
        '/system/auth/configInfo',
        undefined,
        ANONYMOUS_REQUEST_OPTIONS,
    );
    return response.data;
}

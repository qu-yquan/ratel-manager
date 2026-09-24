/**
 * 主应用公开启动配置服务。
 */

import { get } from '@gwsu/core';

const ANONYMOUS_REQUEST_OPTIONS = {
  skipAuth: true,
  skipUnauthorizedRedirect: true,
};

export interface LoginConfigInfo {
  /** 项目名称 */
  projectName: string;
  /** 微应用名与开发环境入口地址映射 */
  microApps: Record<string, string>;
}

/**
 * 获取无需登录的应用启动配置。
 */
export async function getLoginConfigInfo(): Promise<LoginConfigInfo> {
  const response = await get<LoginConfigInfo>(
    '/system/auth/configInfo',
    undefined,
    ANONYMOUS_REQUEST_OPTIONS,
  );
  return response.data;
}

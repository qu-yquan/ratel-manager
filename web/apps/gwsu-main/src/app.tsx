/**
 * 应用初始化配置
 */

import { useMenuStore, useUserStore } from '@gwsu/core';

import { getLoginConfigInfo } from './services/config';

const MICRO_APP_NAME_PATTERN = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/;
const DEVELOPMENT_ENTRY_PATTERN = /^(?:https?:)?\/\//;

interface MicroAppRuntimeConfig {
  name: string;
  entry: string;
  routePath: string;
}

function createMicroAppRuntimeConfig(
  name: string,
  developmentEntry: string,
): MicroAppRuntimeConfig {
  const normalizedName = name.trim();
  const normalizedDevelopmentEntry = developmentEntry.trim();

  if (!MICRO_APP_NAME_PATTERN.test(normalizedName)) {
    throw new Error(`微应用名格式不正确: ${name}`);
  }

  if (
    process.env.NODE_ENV === 'development' &&
    !DEVELOPMENT_ENTRY_PATTERN.test(normalizedDevelopmentEntry)
  ) {
    throw new Error(`微应用 ${normalizedName} 的开发环境地址不正确`);
  }

  return {
    name: normalizedName,
    entry:
      process.env.NODE_ENV === 'development'
        ? normalizedDevelopmentEntry
        : `/${normalizedName}/`,
    routePath: `/${normalizedName}/*`,
  };
}

/**
 * 从公开启动配置中动态注册微应用。
 */
export async function qiankun() {
  try {
    const { microApps } = await getLoginConfigInfo();
    const microAppEntries = Object.entries(microApps ?? {});

    if (microAppEntries.length === 0) {
      throw new Error('未配置可用的微应用');
    }

    const configs = microAppEntries.map(([name, entry]) =>
      createMicroAppRuntimeConfig(name, entry),
    );

    return {
      apps: configs.map(({ name, entry }) => ({ name, entry })),
      routes: configs.map(({ name, routePath }) => ({
        path: routePath,
        microApp: name,
      })),
    };
  } catch (error) {
    console.error('微应用配置加载失败', error);
    throw error;
  }
}

/**
 * 路由变化时检查菜单加载
 */
export function onRouteChange({
  location,
}: {
  location: { pathname: string };
}) {
  const isLoggedIn = useUserStore.getState().checkLogin();

  const { menus, loadMenus } = useMenuStore.getState();
  // 已登录但菜单为空时，重新加载菜单
  if (
    isLoggedIn &&
    menus.length === 0 &&
    !location.pathname.includes('/login')
  ) {
    loadMenus().catch(console.error);
  }
}

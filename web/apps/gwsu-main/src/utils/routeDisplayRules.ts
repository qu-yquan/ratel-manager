const DEFAULT_STANDALONE_ROUTE_PATTERN = '^/sub-system/login(?:/.*)?$';

/**
 * 创建独立布局路由匹配器。
 *
 * 环境变量配置无效时回退到登录页默认规则，避免登录页意外加载主应用布局。
 */
export function createStandaloneRouteMatcher(
  pattern?: string,
): (pathname: string) => boolean {
  const patternSource = pattern?.trim() || DEFAULT_STANDALONE_ROUTE_PATTERN;
  let routePattern: RegExp;

  try {
    routePattern = new RegExp(patternSource);
  } catch (error) {
    console.error('独立布局路由匹配规则配置错误，将使用默认规则', error);
    routePattern = new RegExp(DEFAULT_STANDALONE_ROUTE_PATTERN);
  }

  return (pathname: string) => routePattern.test(pathname);
}

const standaloneRouteMatcher = createStandaloneRouteMatcher(
  process.env.UMI_APP_STANDALONE_ROUTE_PATTERN,
);

/** 判断当前路径是否使用不含主应用导航及智能助手的独立布局。 */
export function isStandaloneRoute(pathname: string): boolean {
  return standaloneRouteMatcher(pathname);
}

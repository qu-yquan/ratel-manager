import { createStandaloneRouteMatcher } from './routeDisplayRules';

describe('createStandaloneRouteMatcher', () => {
  const matcher = createStandaloneRouteMatcher(
    '^/(?:sub-system/login|sub-system/sso/callback)(?:/.*)?$',
  );

  it.each([
    '/sub-system/login',
    '/sub-system/login/reset-password',
    '/sub-system/sso/callback',
    '/sub-system/sso/callback/complete',
  ])('路径 %s 使用独立布局', (pathname) => {
    expect(matcher(pathname)).toBe(true);
  });

  it.each([
    '/sub-system/login-history',
    '/sub-system/dashboard',
    '/sub-security/login-audit',
  ])('路径 %s 使用主应用布局', (pathname) => {
    expect(matcher(pathname)).toBe(false);
  });

  it('配置为空时使用默认登录页规则', () => {
    const defaultMatcher = createStandaloneRouteMatcher('  ');

    expect(defaultMatcher('/sub-system/login')).toBe(true);
    expect(defaultMatcher('/sub-system/dashboard')).toBe(false);
  });

  it('配置非法时记录错误并回退到默认规则', () => {
    const consoleError = jest
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);

    const fallbackMatcher = createStandaloneRouteMatcher('[');

    expect(fallbackMatcher('/sub-system/login')).toBe(true);
    expect(fallbackMatcher('/sub-system/sso/callback')).toBe(false);
    expect(consoleError).toHaveBeenCalledTimes(1);

    consoleError.mockRestore();
  });
});

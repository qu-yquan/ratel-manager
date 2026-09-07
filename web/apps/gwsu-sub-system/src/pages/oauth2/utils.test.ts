import {parseOAuthAuthorizeScopes, resolveOAuthRedirect} from './utils';

declare const describe: (name: string, fn: () => void) => void;
declare const it: (name: string, fn: () => void) => void;
declare const expect: (actual: unknown) => {
    toEqual(expected: unknown): void;
    toBe(expected: unknown): void;
};

describe('OAuth2 登录页工具', () => {
    it('解析 redirect 参数为绝对授权地址', () => {
        const redirect = resolveOAuthRedirect('?redirect=https%3A%2F%2Fapi.example.com%2Fsystem%2Foauth2%2Fauthorize%3Fclient_id%3Ddemo');

        expect(redirect).toBe('https://api.example.com/system/oauth2/authorize?client_id=demo');
    });

    it('拒绝非 http 协议 redirect', () => {
        const redirect = resolveOAuthRedirect('?redirect=javascript%3Aalert%281%29');

        expect(redirect).toBe(null);
    });

    it('按空白字符解析 scope', () => {
        expect(parseOAuthAuthorizeScopes('openid profile  email')).toEqual(['openid', 'profile', 'email']);
    });
}

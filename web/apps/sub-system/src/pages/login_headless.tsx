import  {useEffect, useState} from 'react';
import {EventType, emitEvent, useMenuStore, useUserStore, useHeadlessStore, fetchCurrentUserInfo} from '@gwsu/core';
import {headlessLogin, TerminalType} from '../services/login';
import styles from './login_headless.module.less';

export default function LoginHeadless() {
    const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
    const [errorMsg, setErrorMsg] = useState('');

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const certificationKey = params.get('certification');
        const threadIdParam = params.get('threadId');
        const tokenParam = params.get('token');

        // 如果有 threadId，立即存储到 headlessStore
        if (threadIdParam) {
            useHeadlessStore.getState().setThreadId(threadIdParam);
        }

        if (!certificationKey) {
            const msg = '缺少认证凭证参数';
            setStatus('error');
            setErrorMsg(msg);
            document.body.setAttribute(
              "data-headless-login-status",
              "error: " + msg
            );
            return;
        }

        let cancelled = false;

        (async () => {
            try {
                let loginToken: { token: string; userId: string; expires: number; alterMsg?: string };

                if (tokenParam) {
                    // URL 中已有 token，跳过登录接口调用，直接使用
                    loginToken = {
                        token: tokenParam,
                        userId: '',
                        expires: 86400,
                    };
                } else {
                    loginToken = await headlessLogin({
                      type: "headless",
                      terminal: TerminalType.PC,
                      certificationKey,
                    });
                }

                if (cancelled) return;

                const tokenFormat = loginToken.token && loginToken.token.split('.').length === 3 ? 'JWT' : '非JWT(UUID?)';
                console.log('[HeadlessLogin] 登录响应: userId=%s, tokenFormat=%s, tokenValue=%s',
                    loginToken.userId, tokenFormat, loginToken.token);

                const expireTime = Date.now() + loginToken.expires * 1000;

                useUserStore.getState().setTokenInfo({
                    token: loginToken.token,
                    userId: loginToken.userId,
                    expires: loginToken.expires,
                    expireTime,
                });

                const userInfo = await fetchCurrentUserInfo();
                useUserStore.getState().setUserInfo(userInfo);

                if (loginToken.alterMsg) {
                    console.warn('[HeadlessLogin]', loginToken.alterMsg);
                }

                await useMenuStore.getState().loadMenus();

                if (cancelled) return;

                // 登录成功，携带 threadId 和无头模式标记发送事件
                emitEvent(EventType.LOGIN_SUCCESS, { threadId: threadIdParam || null, isHeadless: true });

                setStatus('success');
                document.body.setAttribute(
                  "data-headless-login-status",
                  "success"
                );

                // 设置无头模式事件前缀，供 CopilotKit 事件回调输出到 console
                const shortCert = certificationKey.substring(0, 8);
                document.body.setAttribute(
                  "data-headless-event-prefix",
                  `${shortCert}-HeadlessEvent`
                );
            } catch (error) {
                if (cancelled) return;

                const msg = error instanceof Error ? error.message : '登录失败';
                setStatus('error');
                setErrorMsg(msg);
                document.body.setAttribute(
                  "data-headless-login-status",
                  "error: " + msg
                );
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <div className={styles.container} data-testid="headless-login-page">
            <div className={styles.card}>
                <div className={styles.title}>无头浏览器认证</div>

                {status === 'loading' && (
                    <p className={styles.statusText}>
                        正在认证
                        <span className={styles.loadingDots}>
                            <span className={styles.dot}>●</span>
                            <span className={styles.dot}>●</span>
                            <span className={styles.dot}>●</span>
                        </span>
                    </p>
                )}

                {status === 'success' && (
                    <>
                        <div className={styles.successIcon}>
                            <span className={styles.checkmark}>✓</span>
                        </div>
                        <p className={styles.statusText}>认证成功</p>
                    </>
                )}

                {status === 'error' && (
                    <p className={styles.errorText} data-testid="headless-login-error">
                        认证失败: {errorMsg}
                    </p>
                )}
            </div>
        </div>
    )
}

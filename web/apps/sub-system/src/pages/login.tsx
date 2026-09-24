import React, {useCallback, useEffect, useRef, useState} from 'react';
import {App} from 'antd';
import {EventType, emitEvent, useMenuStore, useUserStore, fetchCurrentUserInfo, encryptPassword} from '@gwsu/core';
import {
    getDingTalkAuthUrl,
    getLoginConfigInfo,
    login,
    LoginToken,
    TerminalType,
} from '../services/login';
import CaptchaVerify, {CaptchaPass} from './components/CaptchaVerify';
import DingTalkFirstLoginModal from './components/DingTalkFirstLoginModal';
import styles from './login.module.less';

interface PendingCredentials {
    username: string;
    encryptedPassword: string;
}

export default function Login() {
    const {message} = App.useApp();
    const [username, setUsername] = useState('admin');
    const [password, setPassword] = useState('admin123');
    const [loading, setLoading] = useState(false);
    const [projectName, setProjectName] = useState('Ratel');
    const [temporaryVoucher, setTemporaryVoucher] = useState<string | null>(null);
    const [captchaOpen, setCaptchaOpen] = useState(false);
    const pendingCredentialsRef = useRef<PendingCredentials | null>(null);

    useEffect(() => () => {
        pendingCredentialsRef.current = null;
    }, []);

    /** 登录页加载时获取项目配置信息 */
    useEffect(() => {
        getLoginConfigInfo().then((info) => {
            if (info.projectName) {
                setProjectName(info.projectName);
            }
        }).catch(() => {
            // 获取失败时使用默认值
        });
    }, []);

    const handleLoginSuccess = useCallback(async (loginToken: LoginToken) => {
        const expireTime = Date.now() + loginToken.expires * 1000;
        const userStore = useUserStore.getState();

        userStore.setTokenInfo({
            token: loginToken.token,
            userId: loginToken.userId,
            expires: loginToken.expires,
            expireTime,
        });

        try {
            const userInfo = await fetchCurrentUserInfo();
            userStore.setUserInfo(userInfo);

            if (loginToken.alterMsg) {
                message.warning(loginToken.alterMsg);
            }

            await useMenuStore.getState().loadMenus();

            emitEvent(EventType.LOGIN_SUCCESS);

            message.success('登录成功');
        } catch (error) {
            userStore.clearUserData();
            throw error;
        }
    }, [message]);

    /** 处理钉钉回调：已有账号直接登录，首次登录进入账号关联流程。 */
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const token = params.get('token');
        const voucher = params.get('temporaryVoucher');
        const errorMessage = params.get('errMsg');
        const userId = params.get('userId');
        const expires = params.get('expires');
        const alterMsg = params.get('alterMsg');

        if (!token && !voucher && !errorMessage) return;

        // 立即清除敏感参数，避免刷新重复处理或凭证残留在地址栏。
        window.history.replaceState({}, '', window.location.pathname);

        if (token && userId && expires) {
            handleLoginSuccess({
                token,
                userId,
                expires: Number(expires),
                alterMsg: alterMsg || undefined,
            }).catch(() => {
                message.error('钉钉登录失败，请重试');
            });
            return;
        }

        if (voucher) {
            setTemporaryVoucher(voucher);
            return;
        }

        if (errorMessage) {
            message.error(errorMessage);
        }
    }, [handleLoginSuccess, message]);

    const handleLogin = (e: React.FormEvent) => {
        e.preventDefault();

        if (loading) return;

        const normalizedUsername = username.trim();
        if (!normalizedUsername || !password.trim()) {
            pendingCredentialsRef.current = null;
            setCaptchaOpen(false);
            message.warning('请输入用户名和密码');
            return;
        }

        if (captchaOpen) return;

        pendingCredentialsRef.current = {
            username: normalizedUsername,
            encryptedPassword: encryptPassword(password),
        };
        setCaptchaOpen(true);
    };

    const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
        const pendingCredentials = pendingCredentialsRef.current;
        pendingCredentialsRef.current = null;
        setCaptchaOpen(false);

        if (!pendingCredentials) return;

        setLoading(true);

        try {
            const loginToken = await login({
                type: 'password',
                terminal: TerminalType.WEB,
                username: pendingCredentials.username,
                password: pendingCredentials.encryptedPassword,
                captchaId: captchaPass.captchaId,
                captchaCode: captchaPass.captchaCode,
            });

            await handleLoginSuccess(loginToken);
        } catch {
            // 错误提示已在 request.ts 中统一处理
        } finally {
            setLoading(false);
        }
    };

    const handleCaptchaCancel = () => {
        pendingCredentialsRef.current = null;
        setCaptchaOpen(false);
    };

    /** 钉钉快捷登录 - 直接重定向到钉钉授权页 */
    const handleDingTalkLogin = async () => {
        try {
            const authUrl = await getDingTalkAuthUrl();
            if (!authUrl) {
                message.error('获取钉钉授权地址失败');
                return;
            }
            window.location.href = authUrl;
        } catch {
            message.error('获取钉钉授权地址失败');
        }
    };

    return (
        <div className={styles.container}>
            {/* 左侧展示区 */}
            <div className={styles.showcase}>
                <div className={styles.brandName}>{projectName}</div>

                <div className={styles.quoteSection}>
                    <div className={styles.quoteBar}/>
                    <div className={styles.quoteContent}>
                        <div className={styles.chineseText}>
                            {'载营魄抱一'.split('').map((char, i) => (
                                <span key={i} className={styles.bounceChar} style={{animationDelay: `${i * 0.15}s`}}>{char}</span>
                            ))}
                        </div>
                        <div className={styles.chineseText}>
                            {'能无离乎'.split('').map((char, i) => (
                                <span key={i} className={styles.bounceChar} style={{animationDelay: `${(i + 5) * 0.15}s`}}>{char}</span>
                            ))}
                        </div>
                        <div className={styles.englishText}>
                            Adhere to the <span className={styles.highlightLetter}>G</span>reat{' '}
                            <span className={styles.highlightLetter}>W</span>ay, remain{' '}
                            <span className={styles.highlightLetter}>S</span>ingle-minded and{' '}
                            <span className={styles.highlightLetter}>U</span>navering
                        </div>
                    </div>
                </div>

                {/* 装饰元素 */}
                <div className={styles.decoCircle}/>
                <div className={styles.decoDot1}/>
                <div className={styles.decoDot2}/>
            </div>

            {/* 右侧登录区 */}
            <div className={styles.loginSection}>
                <div className={styles.logoIcon}>
                    <div className={styles.diamond}/>
                </div>
                <div className={styles.loginTitle}>登 录</div>

                <form onSubmit={handleLogin} className={styles.form}>
                    <div className={styles.inputGroup}>
                        <label className={styles.label}>用户名</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="请输入用户名"
                            className={styles.input}
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className={styles.inputGroup}>
                        <label className={styles.label}>密码</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="请输入密码"
                            className={styles.input}
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className={`${styles.button} ${loading ? styles.buttonLoading : ''}`}
                        disabled={loading}
                    >
                        {loading ? (
                            <span className={styles.loadingDots}>
                <span className={styles.dot}>●</span>
                <span className={styles.dot}>●</span>
                <span className={styles.dot}>●</span>
              </span>
                        ) : (
                            '登 录'
                        )}
                    </button>
                </form>

                {/* 第三方登录 */}
                <div className={styles.dividerSection}>
                    <div className={styles.dividerLine}/>
                    <span className={styles.dividerText}>其他登录方式</span>
                    <div className={styles.dividerLine}/>
                </div>

                <button
                    type="button"
                    className={styles.dingtalkButton}
                    onClick={handleDingTalkLogin}
                    aria-label="钉钉快捷登录"
                >
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 13.2c-.2.52-.84.96-1.44 1.12-.36.08-.84.12-1.4.12-.56 0-1.2-.08-1.6-.2-.56-.16-1.08-.44-1.52-.8-.2-.16-.4-.32-.56-.48-.04.16-.08.32-.16.44-.2.36-.56.6-.96.68-.12.04-.24.04-.36.04-.32 0-.6-.12-.84-.32-.28-.24-.44-.56-.48-.92v-.08l.04-.32c.08-.4.24-.76.48-1.04.08-.08.12-.2.16-.32-.28-.36-.52-.72-.72-1.12-.24-.48-.36-1-.36-1.52 0-.72.2-1.36.56-1.88.04-.04.04-.08.08-.12.28-.36.64-.64 1.08-.84.44-.2.92-.28 1.4-.28.52 0 1 .12 1.44.36.4.2.76.52 1.04.88.32.4.52.84.64 1.36.08.32.12.68.12 1.04 0 .6-.12 1.16-.36 1.64-.2.4-.48.76-.8 1.08.04.12.08.24.16.36.2.36.52.6.88.76.16.08.32.12.48.12.08 0 .16 0 .2-.04.12-.04.2-.12.24-.2.04-.08.04-.16 0-.24-.04-.08-.12-.16-.2-.2-.16-.08-.32-.12-.48-.12-.12 0-.24 0-.36.04l-.08-.16z"/>
                    </svg>
                    <span>钉钉快捷登录</span>
                </button>
            </div>
            <CaptchaVerify
                open={captchaOpen}
                onCancel={handleCaptchaCancel}
                onSuccess={(captchaPass) => {
                    void handleCaptchaSuccess(captchaPass);
                }}
            />
            <DingTalkFirstLoginModal
                open={Boolean(temporaryVoucher)}
                temporaryVoucher={temporaryVoucher}
                onCancel={() => setTemporaryVoucher(null)}
                onSuccess={async (token) => {
                    setTemporaryVoucher(null);
                    try {
                        await handleLoginSuccess(token);
                    } catch {
                        message.warning('账号关联已完成，但登录初始化失败，请重新登录');
                    }
                }}
            />
        </div>
    );
}

import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {App} from 'antd';
import {encryptPassword} from '@gwsu/core';
import {getLoginConfigInfo, login, TerminalType} from '../../services/login';
import CaptchaVerify, {CaptchaPass} from '../components/CaptchaVerify';
import {resolveOAuthRedirect} from './utils';
import styles from '../login.module.less';

export default function OAuth2Login() {
    const {message} = App.useApp();
    const redirect = useMemo(() => resolveOAuthRedirect(window.location.search), []);
    const [username, setUsername] = useState('admin');
    const [password, setPassword] = useState('admin123');
    const [loading, setLoading] = useState(false);
    const [projectName, setProjectName] = useState('Ratel');
    const [captchaOpen, setCaptchaOpen] = useState(false);

    useEffect(() => {
        getLoginConfigInfo().then((info) => {
            if (info.projectName) {
                setProjectName(info.projectName);
            }
        }).catch(() => {
            // 获取失败时使用默认值
        });
    }, []);

    const handleLoginSuccess = useCallback(() => {
        if (!redirect) {
            message.error('OAuth授权请求已失效，请从应用重新发起授权');
            return;
        }
        message.success('登录成功');
        window.location.href = redirect;
    }, [message, redirect]);

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        if (loading) return;

        if (!username.trim() || !password.trim()) {
            message.warning('请输入用户名和密码');
            return;
        }

        if (!redirect) {
            message.error('OAuth授权请求已失效，请从应用重新发起授权');
            return;
        }

        setCaptchaOpen(true);
    };

    const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
        setCaptchaOpen(false);
        setLoading(true);

        try {
            await login({
                type: 'password',
                terminal: TerminalType.WEB,
                username: username.trim(),
                password: encryptPassword(password),
                captchaId: captchaPass.captchaId,
                captchaCode: captchaPass.captchaCode,
            });

            handleLoginSuccess();
        } catch {
            // 错误提示已在 request.ts 中统一处理
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.showcase}>
                <div className={styles.brandName}>{projectName}</div>

                <div className={styles.quoteSection}>
                    <div className={styles.quoteBar}/>
                    <div className={styles.quoteContent}>
                        <div className={styles.chineseText}>
                            {'OAuth授权'.split('').map((char, i) => (
                                <span key={i} className={styles.bounceChar} style={{animationDelay: `${i * 0.15}s`}}>{char}</span>
                            ))}
                        </div>
                        <div className={styles.englishText}>
                            Sign in to continue authorization
                        </div>
                    </div>
                </div>

                <div className={styles.decoCircle}/>
                <div className={styles.decoDot1}/>
                <div className={styles.decoDot2}/>
            </div>

            <div className={styles.loginSection}>
                <div className={styles.logoIcon}>
                    <div className={styles.diamond}/>
                </div>
                <div className={styles.loginTitle}>OAuth 登录</div>

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
                        disabled={loading || !redirect}
                    >
                        {loading ? (
                            <span className={styles.loadingDots}>
                                <span className={styles.dot}>●</span>
                                <span className={styles.dot}>●</span>
                                <span className={styles.dot}>●</span>
                            </span>
                        ) : (
                            '继续授权'
                        )}
                    </button>
                </form>
            </div>
            <CaptchaVerify
                open={captchaOpen}
                onCancel={() => setCaptchaOpen(false)}
                onSuccess={(captchaPass) => {
                    void handleCaptchaSuccess(captchaPass);
                }}
            />
        </div>
    );
}

import {useMemo, useState} from 'react';
import {resolveDeviceVerificationUri} from './utils';
import styles from './deviceVerification.module.less';

const ERROR_MESSAGES: Record<string, string> = {
    invalid_grant: '设备码无效或已过期，请在设备上重新发起授权',
    invalid_request: '设备码格式不正确，请检查后重试',
    access_denied: '本次设备授权已被拒绝',
};

export default function OAuth2DeviceVerification() {
    const params = useMemo(() => new URLSearchParams(window.location.search), []);
    const verificationUri = useMemo(() => resolveDeviceVerificationUri(window.location.search), []);
    const result = params.get('result');
    const error = params.get('error') || '';
    const [userCode, setUserCode] = useState('');
    const normalizedUserCode = userCode.trim();

    if (result === 'success') {
        return (
            <main className={styles.container}>
                <section className={styles.panel} aria-live="polite">
                    <div className={`${styles.statusIcon} ${styles.successIcon}`} aria-hidden="true">✓</div>
                    <h1 className={styles.title}>设备授权完成</h1>
                    <p className={styles.subtitle}>可以返回设备继续使用，此页面现在可以关闭。</p>
                </section>
            </main>
        );
    }

    return (
        <main className={styles.container}>
            <section className={styles.panel}>
                <div className={styles.brand}>Ratel</div>
                <h1 className={styles.title}>设备授权</h1>
                <p className={styles.subtitle}>请输入设备上显示的授权码。</p>

                {result === 'error' && (
                    <div className={styles.errorMessage} role="alert">
                        {ERROR_MESSAGES[error] || '设备授权请求处理失败，请重新发起授权'}
                    </div>
                )}

                <form method="get" action={verificationUri} className={styles.form}>
                    <label className={styles.label} htmlFor="oauth-device-user-code">设备码</label>
                    <input
                        id="oauth-device-user-code"
                        className={styles.input}
                        name="user_code"
                        value={userCode}
                        onChange={event => setUserCode(event.target.value.toUpperCase())}
                        placeholder="例如：ABCD-EFGH"
                        autoComplete="one-time-code"
                        spellCheck={false}
                        autoFocus
                        required
                    />
                    <button
                        type="submit"
                        className={styles.primaryButton}
                        disabled={!normalizedUserCode}
                    >
                        继续授权
                    </button>
                </form>
            </section>
        </main>
    );
}

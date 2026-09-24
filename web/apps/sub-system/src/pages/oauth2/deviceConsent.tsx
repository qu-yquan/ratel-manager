import {useEffect, useMemo, useState} from 'react';
import {get} from '@gwsu/core';
import {parseOAuthAuthorizeScopes, resolveDeviceVerificationUri} from './utils';
import styles from './consent.module.less';

interface OAuthConsentScope {
    scopeCode: string;
    scopeName: string;
    description?: string;
}

interface OAuthConsentContext {
    clientId: string;
    clientName: string;
    scopes: OAuthConsentScope[];
    canAuthorize: boolean;
}

export default function OAuth2DeviceConsent() {
    const params = useMemo(() => new URLSearchParams(window.location.search), []);
    const verificationUri = useMemo(() => resolveDeviceVerificationUri(window.location.search), []);
    const clientId = params.get('client_id') || '';
    const state = params.get('state') || '';
    const userCode = params.get('user_code') || '';
    const requestedScopes = useMemo(
        () => params.getAll('scope').flatMap(parseOAuthAuthorizeScopes),
        [params],
    );
    const [context, setContext] = useState<OAuthConsentContext>();
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');

    useEffect(() => {
        if (!clientId || !state || !userCode) {
            setLoadError('设备授权请求参数不完整，请重新发起授权');
            setLoading(false);
            return;
        }
        get<OAuthConsentContext>(
            '/system/auth/oauth2/consent-context',
            {
                clientId,
                scope: requestedScopes.length ? requestedScopes.join(' ') : undefined,
            },
            {
                skipAuth: true,
                skipUnauthorizedRedirect: true,
                showError: false,
            },
        ).then(response => {
            setContext(response.data);
            setLoadError('');
        }).catch(error => {
            setContext(undefined);
            setLoadError(error instanceof Error ? error.message : '授权信息加载失败');
        }).finally(() => setLoading(false));
    }, [clientId, requestedScopes, state, userCode]);

    const canAuthorize = Boolean(context?.canAuthorize && context.scopes.length > 0);
    const protocolFields = (
        <>
            <input type="hidden" name="client_id" value={clientId}/>
            <input type="hidden" name="state" value={state}/>
            <input type="hidden" name="user_code" value={userCode}/>
        </>
    );

    return (
        <main className={styles.container}>
            <section className={styles.panel}>
                <h1 className={styles.title}>设备授权</h1>
                <p className={styles.subtitle}>
                    {context?.clientName
                        ? `应用「${context.clientName}」正在请求访问以下权限。`
                        : '请确认设备请求的授权权限。'}
                </p>

                <form id="oauth2-device-consent-form" method="post" action={verificationUri}>
                    {protocolFields}
                    <div className={styles.scopeList}>
                        {loading ? (
                            <div className={styles.message}>正在加载授权信息...</div>
                        ) : loadError ? (
                            <div className={`${styles.message} ${styles.errorMessage}`}>{loadError}</div>
                        ) : canAuthorize ? context?.scopes.map(scope => (
                            <label key={scope.scopeCode} className={styles.scopeItem}>
                                <input type="checkbox" name="scope" value={scope.scopeCode} defaultChecked/>
                                <span className={styles.scopeContent}>
                                    <strong>{scope.scopeName || scope.scopeCode}</strong>
                                    {scope.description && (
                                        <span className={styles.scopeDescription}>{scope.description}</span>
                                    )}
                                </span>
                            </label>
                        )) : (
                            <div className={`${styles.message} ${styles.warningMessage}`}>
                                未配置任何授权权限，请联系管理员
                            </div>
                        )}
                    </div>
                </form>

                {canAuthorize && (
                    <div className={styles.actions}>
                        <form method="post" action={verificationUri}>
                            {protocolFields}
                            <button className={styles.button} type="submit" data-ai-approval>
                                拒绝
                            </button>
                        </form>
                        <button
                            className={`${styles.button} ${styles.primaryButton}`}
                            type="submit"
                            form="oauth2-device-consent-form"
                            data-ai-approval
                        >
                            同意
                        </button>
                    </div>
                )}
            </section>
        </main>
    );
}

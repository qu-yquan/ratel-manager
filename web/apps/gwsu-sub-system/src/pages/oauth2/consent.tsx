import React, {useMemo} from 'react';
import {parseOAuthAuthorizeScopes, resolveAuthorizeUri} from './utils';
import styles from './consent.module.less';

export default function OAuth2Consent() {
    const params = useMemo(() => new URLSearchParams(window.location.search), []);
    const authorizeUri = useMemo(() => resolveAuthorizeUri(window.location.search), []);
    const clientId = params.get('client_id') || '';
    const state = params.get('state') || '';
    const scopes = params.getAll('scope').flatMap(parseOAuthAuthorizeScopes);

    return (
        <div className={styles.container}>
            <div className={styles.panel}>
                <h1 className={styles.title}>应用授权</h1>
                <p className={styles.subtitle}>
                    {clientId ? `应用 ${clientId} 正在请求访问以下权限。` : '应用正在请求访问以下权限。'}
                </p>

                <form id="oauth2-consent-form" method="post" action={authorizeUri}>
                    <input type="hidden" name="client_id" value={clientId}/>
                    <input type="hidden" name="state" value={state}/>

                    <div className={styles.scopeList}>
                        {scopes.length > 0 ? scopes.map((scope) => (
                            <label key={scope} className={styles.scopeItem}>
                                <input type="checkbox" name="scope" value={scope} defaultChecked/>
                                <span>{scope}</span>
                            </label>
                        )) : (
                            <div className={styles.scopeItem}>应用未申请额外权限</div>
                        )}
                    </div>
                </form>

                <div className={styles.actions}>
                    <form method="post" action={authorizeUri}>
                        <input type="hidden" name="client_id" value={clientId}/>
                        <input type="hidden" name="state" value={state}/>
                        <button className={styles.button} type="submit">
                            拒绝
                        </button>
                    </form>
                    <button
                        className={`${styles.button} ${styles.primaryButton}`}
                        type="submit"
                        form="oauth2-consent-form"
                    >
                        同意
                    </button>
                </div>
            </div>
        </div>
    );
}

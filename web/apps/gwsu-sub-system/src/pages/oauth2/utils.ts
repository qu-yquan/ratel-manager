export function resolveOAuthRedirect(search: string): string | null {
    const redirect = new URLSearchParams(search).get('redirect');
    if (!redirect) {
        return null;
    }
    try {
        const url = new URL(redirect, browserOrigin());
        if (url.protocol === 'http:' || url.protocol === 'https:') {
            return url.href;
        }
    } catch {
        if (redirect.startsWith('/') && !redirect.startsWith('//')) {
            return redirect;
        }
    }
    return null;
}

export function resolveAuthorizeUri(search: string): string {
    const authorizeUri = new URLSearchParams(search).get('authorize_uri');
    if (!authorizeUri) {
        return '/api/system/oauth2/authorize';
    }
    try {
        const url = new URL(authorizeUri, browserOrigin());
        if (url.protocol === 'http:' || url.protocol === 'https:') {
            return url.href;
        }
    } catch {
        if (authorizeUri.startsWith('/') && !authorizeUri.startsWith('//')) {
            return authorizeUri;
        }
    }
    return '/api/system/oauth2/authorize';
}

export function parseOAuthAuthorizeScopes(scope: string | null): string[] {
    if (!scope) {
        return [];
    }
    return scope.split(/\s+/).map((item) => item.trim()).filter(Boolean);
}

function browserOrigin(): string {
    return typeof window === 'undefined' ? 'http://localhost' : window.location.origin;
}

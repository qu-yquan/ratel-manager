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
    return resolveHttpUrlParameter(search, 'authorize_uri', '/api/system/auth/oauth2/authorize');
}

export function resolveDeviceVerificationUri(search: string): string {
    return resolveHttpUrlParameter(
        search,
        'device_verification_uri',
        '/api/system/auth/oauth2/device_verification',
    );
}

function resolveHttpUrlParameter(search: string, parameter: string, fallback: string): string {
    const value = new URLSearchParams(search).get(parameter);
    if (!value) {
        return fallback;
    }
    try {
        const url = new URL(value, browserOrigin());
        if (url.protocol === 'http:' || url.protocol === 'https:') {
            return url.href;
        }
    } catch {
        if (value.startsWith('/') && !value.startsWith('//')) {
            return value;
        }
    }
    return fallback;
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

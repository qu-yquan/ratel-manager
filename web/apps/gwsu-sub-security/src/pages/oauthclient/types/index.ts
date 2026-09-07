export type OAuthClientType = 'CONFIDENTIAL' | 'PUBLIC' | 'SPA' | 'MOBILE' | 'DESKTOP';

export type OAuthClientStatus = 'ENABLED' | 'DISABLED';

export type OAuthGrantType = 'AUTHORIZATION_CODE' | 'CLIENT_CREDENTIALS' | 'REFRESH_TOKEN' | 'DEVICE_CODE';

export type OAuthClientAuthenticationMethod = 'NONE' | 'CLIENT_SECRET_BASIC' | 'CLIENT_SECRET_POST';

export interface OAuthClientQuery {
  pageNum?: number;
  pageSize?: number;
  clientId?: string;
  clientName?: string;
  clientType?: OAuthClientType;
  status?: OAuthClientStatus;
}

export interface OAuthClientInfo {
  id?: string;
  clientId?: string;
  clientSecret?: string;
  clientName: string;
  clientType: OAuthClientType;
  accountType?: string;
  status: OAuthClientStatus;
  clientAuthenticationMethods: OAuthClientAuthenticationMethod[];
  authorizationGrantTypes: OAuthGrantType[];
  redirectUris: string[];
  postLogoutRedirectUris: string[];
  scopes: string[];
  requireProofKey: boolean;
  requireAuthorizationConsent: boolean;
  accessTokenTtlSeconds?: number;
  refreshTokenTtlSeconds?: number;
  authorizationCodeTtlSeconds?: number;
  deviceCodeTtlSeconds?: number;
  reuseRefreshTokens: boolean;
  remark?: string;
  createTime?: string;
  modifyTime?: string;
}

export interface OAuthClientSecret {
  id: string;
  clientId: string;
  clientSecret: string;
}

export interface OAuthClientPageResult {
  records: OAuthClientInfo[];
  total: number;
  current: number;
  size: number;
}

export interface KeyValueOption<T extends string = string> {
  key: T;
  value: string;
}

export interface EnumOption<T extends string = string> {
  label: string;
  value: T;
}

export interface OAuthClientEnums {
  clientTypes: KeyValueOption<OAuthClientType>[];
  accountTypes: KeyValueOption<string>[];
  statuses: KeyValueOption<OAuthClientStatus>[];
  grantTypes: KeyValueOption<OAuthGrantType>[];
  authenticationMethods: KeyValueOption<OAuthClientAuthenticationMethod>[];
}

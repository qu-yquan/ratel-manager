export type OAuthClientType =
  | "CONFIDENTIAL"
  | "PUBLIC"
  | "SPA"
  | "MOBILE"
  | "DESKTOP";

export type OAuthClientStatus = "ENABLED" | "DISABLED";

export type OAuthGrantType =
  | "AUTHORIZATION_CODE"
  | "CLIENT_CREDENTIALS"
  | "REFRESH_TOKEN"
  | "DEVICE_CODE";

export type OAuthClientAuthenticationMethod =
  | "NONE"
  | "CLIENT_SECRET_BASIC"
  | "CLIENT_SECRET_POST";

export interface OAuthClientQuery {
  pageNum?: number;
  pageSize?: number;
  clientName?: string;
  clientType?: OAuthClientType;
  accountType?: string;
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

export interface OAuthScopeInfo {
  id?: string;
  scopeCode: string;
  scopeName: string;
  description?: string;
  accountType: string;
  status: OAuthClientStatus;
  resourceIds?: string[];
  createTime?: string;
  modifyTime?: string;
}

export interface OAuthScopeQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  accountType?: string;
  status?: OAuthClientStatus;
}

export interface OAuthScopePageResult {
  records: OAuthScopeInfo[];
  total: number;
  current: number;
  size: number;
}

export interface ApiResourceInfo {
  id: string;
  modulePrefix: string;
  tagName: string;
  reqPath: string;
  reqMethod: string;
  summary?: string;
}

export interface ApiResourcePageResult {
  records: ApiResourceInfo[];
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

export interface OAuthGuideParameter {
  name: string;
  location: "Query" | "Header" | "Body";
  required: boolean;
  value: string;
  description: string;
}

export interface OAuthGuideRequest {
  key: string;
  title: string;
  method: "GET" | "POST";
  url: string;
  parameters: OAuthGuideParameter[];
  example: string;
  responseExample?: string;
}

export type OAuthGuideCodeLanguage = "javascript" | "python" | "java";

export interface OAuthGuideCodeExample {
  language: OAuthGuideCodeLanguage;
  label: string;
  code: string;
}

export interface OAuthGuideStep {
  title: string;
  description?: string;
  codeExamples?: OAuthGuideCodeExample[];
  requests?: OAuthGuideRequest[];
}

export interface OAuthGuideSection {
  grantType: OAuthGrantType;
  title: string;
  description: string;
  steps: OAuthGuideStep[];
}

export interface OAuthIntegrationGuide {
  client: OAuthClientInfo;
  apiBaseUrl: string;
  authenticationMethod: OAuthClientAuthenticationMethod;
  clientTypeLabel: string;
  accountTypeLabel: string;
  authenticationMethodLabel: string;
  redirectUri?: string;
  sections: OAuthGuideSection[];
}

export interface OAuthIntegrationGuideOptions {
  apiBaseUrl: string;
  authenticationMethod?: OAuthClientAuthenticationMethod;
  redirectUri?: string;
  clientTypeLabel?: string;
  accountTypeLabel?: string;
  authenticationMethodLabel?: string;
}

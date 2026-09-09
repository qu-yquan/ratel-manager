import type {
  OAuthClientAuthenticationMethod,
  OAuthClientInfo,
  OAuthGrantType,
  OAuthGuideCodeExample,
  OAuthGuideParameter,
  OAuthGuideRequest,
  OAuthGuideSection,
  OAuthIntegrationGuide,
  OAuthIntegrationGuideOptions,
} from "../types";

export const CLIENT_SECRET_PLACEHOLDER = "{客户端密钥}";

const GRANT_ORDER: OAuthGrantType[] = [
  "AUTHORIZATION_CODE",
  "CLIENT_CREDENTIALS",
  "REFRESH_TOKEN",
  "DEVICE_CODE",
];

const AUTHENTICATION_METHOD_ORDER: OAuthClientAuthenticationMethod[] = [
  "CLIENT_SECRET_BASIC",
  "CLIENT_SECRET_POST",
  "NONE",
];

const TOKEN_RESPONSE = `{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "access_token": "{访问令牌}",
    "token_type": "Bearer",
    "expires_in": 7200,
    "refresh_token": "{Refresh Token}"
  }
}`;

const DEVICE_RESPONSE = `{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "device_code": "{设备码}",
    "user_code": "{用户码}",
    "verification_uri": "{用户验证地址}",
    "verification_uri_complete": "{完整用户验证地址}",
    "expires_in": 300,
    "interval": 5
  }
}`;

const PKCE_CODE_EXAMPLES: OAuthGuideCodeExample[] = [
  {
    language: "javascript",
    label: "JavaScript",
    code: `function base64UrlEncode(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\\+/g, '-')
    .replace(/\\//g, '_')
    .replace(/=+$/, '');
}

const verifierBytes = crypto.getRandomValues(new Uint8Array(64));
const codeVerifier = base64UrlEncode(verifierBytes);
const digest = await crypto.subtle.digest(
  'SHA-256',
  new TextEncoder().encode(codeVerifier),
);
const codeChallenge = base64UrlEncode(new Uint8Array(digest));

console.log({codeVerifier, codeChallenge});`,
  },
  {
    language: "python",
    label: "Python",
    code: `import base64
import hashlib
import secrets

code_verifier = secrets.token_urlsafe(64)
digest = hashlib.sha256(code_verifier.encode("ascii")).digest()
code_challenge = base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")

print({"code_verifier": code_verifier, "code_challenge": code_challenge})`,
  },
  {
    language: "java",
    label: "Java",
    code: `import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceGenerator {
    public static void main(String[] args) throws Exception {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String codeVerifier = encoder.encodeToString(randomBytes);
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String codeChallenge = encoder.encodeToString(digest);

        System.out.println("code_verifier=" + codeVerifier);
        System.out.println("code_challenge=" + codeChallenge);
    }
}`,
  },
];

export function normalizeApiBaseUrl(apiBaseUrl: string): string {
  return apiBaseUrl.trim().replace(/\/+$/, "");
}

export function getAvailableAuthenticationMethods(
  client: OAuthClientInfo
): OAuthClientAuthenticationMethod[] {
  const configured = new Set(client.clientAuthenticationMethods || []);
  return AUTHENTICATION_METHOD_ORDER.filter((method) => configured.has(method));
}

export function buildOAuthIntegrationGuide(
  client: OAuthClientInfo,
  options: OAuthIntegrationGuideOptions
): OAuthIntegrationGuide {
  const apiBaseUrl = normalizeApiBaseUrl(options.apiBaseUrl);
  const authenticationMethods = getAvailableAuthenticationMethods(client);
  const authenticationMethod = authenticationMethods.includes(
    options.authenticationMethod || "NONE"
  )
    ? options.authenticationMethod!
    : authenticationMethods[0] || "NONE";
  const redirectUri = resolveRedirectUri(
    client.redirectUris,
    options.redirectUri
  );
  const configuredGrants = new Set(client.authorizationGrantTypes || []);
  const sections = GRANT_ORDER.filter((grantType) =>
    configuredGrants.has(grantType)
  ).map((grantType) =>
    buildSection(
      grantType,
      client,
      apiBaseUrl,
      authenticationMethod,
      redirectUri
    )
  );

  return {
    client,
    apiBaseUrl,
    authenticationMethod,
    clientTypeLabel: options.clientTypeLabel || client.clientType,
    accountTypeLabel: options.accountTypeLabel || client.accountType || "-",
    authenticationMethodLabel:
      options.authenticationMethodLabel || authenticationMethod,
    redirectUri,
    sections,
  };
}

function resolveRedirectUri(
  redirectUris: string[],
  selected?: string
): string | undefined {
  if (selected && redirectUris?.includes(selected)) {
    return selected;
  }
  return redirectUris?.[0];
}

function buildSection(
  grantType: OAuthGrantType,
  client: OAuthClientInfo,
  apiBaseUrl: string,
  authenticationMethod: OAuthClientAuthenticationMethod,
  redirectUri?: string
): OAuthGuideSection {
  switch (grantType) {
    case "AUTHORIZATION_CODE":
      return buildAuthorizationCodeSection(
        client,
        apiBaseUrl,
        authenticationMethod,
        redirectUri
      );
    case "CLIENT_CREDENTIALS":
      return buildClientCredentialsSection(
        client,
        apiBaseUrl,
        authenticationMethod
      );
    case "REFRESH_TOKEN":
      return buildRefreshTokenSection(client, apiBaseUrl, authenticationMethod);
    case "DEVICE_CODE":
      return buildDeviceCodeSection(client, apiBaseUrl, authenticationMethod);
  }
}

function buildAuthorizationCodeSection(
  client: OAuthClientInfo,
  apiBaseUrl: string,
  authenticationMethod: OAuthClientAuthenticationMethod,
  redirectUri?: string
): OAuthGuideSection {
  const authorizeParameters: OAuthGuideParameter[] = [
    parameter("response_type", "Query", true, "code", "固定为 code"),
    parameter(
      "client_id",
      "Query",
      true,
      client.clientId || "",
      "当前应用的客户端ID"
    ),
    parameter(
      "redirect_uri",
      "Query",
      true,
      redirectUri || "{重定向URI}",
      "必须与白名单中的地址完全一致"
    ),
    parameter(
      "state",
      "Query",
      true,
      "{随机State}",
      "用于防止跨站请求伪造，回调时必须校验"
    ),
    ...scopeParameters(client, "Query"),
  ];
  if (client.requireProofKey) {
    authorizeParameters.push(
      parameter(
        "code_challenge",
        "Query",
        true,
        "{PKCE挑战码}",
        "由 code_verifier 通过 SHA-256 计算"
      ),
      parameter("code_challenge_method", "Query", true, "S256", "固定使用 S256")
    );
  }

  const tokenParameters = [
    parameter(
      "grant_type",
      "Body",
      true,
      "authorization_code",
      "固定为 authorization_code"
    ),
    parameter("code", "Body", true, "{授权码}", "授权回调返回的一次性授权码"),
    parameter(
      "redirect_uri",
      "Body",
      true,
      redirectUri || "{重定向URI}",
      "与授权请求中的地址保持一致"
    ),
  ];
  if (client.requireProofKey) {
    tokenParameters.push(
      parameter(
        "code_verifier",
        "Body",
        true,
        "{PKCE校验码}",
        "生成挑战码时使用的原始随机字符串"
      )
    );
  }
  tokenParameters.push(
    ...clientAuthenticationParameters(client, authenticationMethod)
  );

  return {
    grantType: "AUTHORIZATION_CODE",
    title: "授权码模式",
    description:
      "适用于需要用户登录并授权的服务端应用、SPA、移动端和桌面应用。",
    steps: [
      ...(client.requireProofKey
        ? [
            {
              title: "生成PKCE参数",
              description:
                "生成高强度随机 code_verifier，并使用 SHA-256 和 Base64 URL 编码得到 code_challenge。",
              codeExamples: PKCE_CODE_EXAMPLES,
            },
          ]
        : []),
      {
        title: "跳转到授权页面",
        description: client.requireAuthorizationConsent
          ? "用户登录后需要确认授权。"
          : "用户登录后将直接进入回调流程。",
        requests: [
          request(
            "authorization-code-authorize",
            "发起授权请求",
            "GET",
            endpoint(apiBaseUrl, "authorize"),
            authorizeParameters,
            buildGetCurl(endpoint(apiBaseUrl, "authorize"), authorizeParameters)
          ),
        ],
      },
      {
        title: "处理授权回调",
        description:
          "从回调地址读取 code 和 state，校验 state 后立即使用授权码换取令牌。",
      },
      {
        title: "使用授权码换取令牌",
        requests: [
          request(
            "authorization-code-token",
            "获取Token",
            "POST",
            endpoint(apiBaseUrl, "token"),
            tokenParameters,
            buildPostCurl(
              endpoint(apiBaseUrl, "token"),
              tokenParameters,
              authenticationMethod,
              client.clientId
            ),
            tokenResponse(client)
          ),
        ],
      },
    ],
  };
}

function buildClientCredentialsSection(
  client: OAuthClientInfo,
  apiBaseUrl: string,
  authenticationMethod: OAuthClientAuthenticationMethod
): OAuthGuideSection {
  const parameters = [
    parameter(
      "grant_type",
      "Body",
      true,
      "client_credentials",
      "固定为 client_credentials"
    ),
    ...scopeParameters(client, "Body"),
    ...clientAuthenticationParameters(client, authenticationMethod),
  ];
  return {
    grantType: "CLIENT_CREDENTIALS",
    title: "客户端凭证模式",
    description:
      "适用于服务与服务之间的访问，不涉及用户登录、重定向URI或授权确认。",
    steps: [
      {
        title: "使用客户端凭证获取令牌",
        requests: [
          request(
            "client-credentials-token",
            "获取Token",
            "POST",
            endpoint(apiBaseUrl, "token"),
            parameters,
            buildPostCurl(
              endpoint(apiBaseUrl, "token"),
              parameters,
              authenticationMethod,
              client.clientId
            ),
            tokenResponse(client, false)
          ),
        ],
      },
    ],
  };
}

function buildRefreshTokenSection(
  client: OAuthClientInfo,
  apiBaseUrl: string,
  authenticationMethod: OAuthClientAuthenticationMethod
): OAuthGuideSection {
  const parameters = [
    parameter(
      "grant_type",
      "Body",
      true,
      "refresh_token",
      "固定为 refresh_token"
    ),
    parameter(
      "refresh_token",
      "Body",
      true,
      "{Refresh Token}",
      "此前授权响应返回的Refresh Token"
    ),
    ...clientAuthenticationParameters(client, authenticationMethod),
  ];
  return {
    grantType: "REFRESH_TOKEN",
    title: "刷新令牌",
    description: client.reuseRefreshTokens
      ? "刷新成功后原Refresh Token仍可继续使用。"
      : "刷新成功后必须保存响应中的新Refresh Token，旧Refresh Token将不可继续使用。",
    steps: [
      {
        title: "使用Refresh Token换取新令牌",
        description: "前置条件：已经通过其他授权模式获得Refresh Token。",
        requests: [
          request(
            "refresh-token",
            "刷新Token",
            "POST",
            endpoint(apiBaseUrl, "token"),
            parameters,
            buildPostCurl(
              endpoint(apiBaseUrl, "token"),
              parameters,
              authenticationMethod,
              client.clientId
            ),
            tokenResponse(client)
          ),
        ],
      },
    ],
  };
}

function buildDeviceCodeSection(
  client: OAuthClientInfo,
  apiBaseUrl: string,
  authenticationMethod: OAuthClientAuthenticationMethod
): OAuthGuideSection {
  const deviceParameters = [
    ...scopeParameters(client, "Body"),
    ...clientAuthenticationParameters(client, authenticationMethod),
  ];
  const pollParameters = [
    parameter(
      "grant_type",
      "Body",
      true,
      "urn:ietf:params:oauth:grant-type:device_code",
      "设备码模式固定值"
    ),
    parameter(
      "device_code",
      "Body",
      true,
      "{设备码}",
      "设备授权接口返回的 device_code"
    ),
    ...clientAuthenticationParameters(client, authenticationMethod),
  ];
  return {
    grantType: "DEVICE_CODE",
    title: "设备码模式",
    description: "适用于输入能力受限的电视、终端或其他设备。",
    steps: [
      {
        title: "申请设备码",
        requests: [
          request(
            "device-code-request",
            "获取设备码",
            "POST",
            endpoint(apiBaseUrl, "device_authorization"),
            deviceParameters,
            buildPostCurl(
              endpoint(apiBaseUrl, "device_authorization"),
              deviceParameters,
              authenticationMethod,
              client.clientId
            ),
            DEVICE_RESPONSE.replace(
              "300",
              String(client.deviceCodeTtlSeconds || 300)
            )
          ),
        ],
      },
      {
        title: "引导用户完成验证",
        description: `提示用户访问 ${endpoint(
          apiBaseUrl,
          "device_verification"
        )}，输入接口返回的用户码并完成登录授权。`,
      },
      {
        title: "轮询Token端点",
        description:
          "按照设备授权响应中的 interval 轮询；遇到 authorization_pending 时继续等待，slow_down 时增加间隔。",
        requests: [
          request(
            "device-code-token",
            "轮询Token",
            "POST",
            endpoint(apiBaseUrl, "token"),
            pollParameters,
            buildPostCurl(
              endpoint(apiBaseUrl, "token"),
              pollParameters,
              authenticationMethod,
              client.clientId
            ),
            tokenResponse(client)
          ),
        ],
      },
    ],
  };
}

function endpoint(apiBaseUrl: string, path: string): string {
  return `${apiBaseUrl}/system/auth/oauth2/${path}`;
}

function clientAuthenticationParameters(
  client: OAuthClientInfo,
  method: OAuthClientAuthenticationMethod
): OAuthGuideParameter[] {
  if (method === "CLIENT_SECRET_BASIC") {
    return [
      parameter(
        "Authorization",
        "Header",
        true,
        `Basic Base64(${client.clientId || ""}:${CLIENT_SECRET_PLACEHOLDER})`,
        "使用客户端ID和客户端密钥进行HTTP Basic认证"
      ),
    ];
  }
  if (method === "CLIENT_SECRET_POST") {
    return [
      parameter(
        "client_id",
        "Body",
        true,
        client.clientId || "",
        "当前应用的客户端ID"
      ),
      parameter(
        "client_secret",
        "Body",
        true,
        CLIENT_SECRET_PLACEHOLDER,
        "创建或重置应用时获得的客户端密钥"
      ),
    ];
  }
  return [
    parameter(
      "client_id",
      "Body",
      true,
      client.clientId || "",
      "当前应用的客户端ID"
    ),
  ];
}

function scopeParameters(
  client: OAuthClientInfo,
  location: OAuthGuideParameter["location"]
): OAuthGuideParameter[] {
  if (!client.scopes?.length) {
    return [];
  }
  return [
    parameter(
      "scope",
      location,
      false,
      client.scopes.join(" "),
      "不传时使用应用配置的全部权限；如需限制本次令牌只能访问部分权限，可追加该参数，多个Scope使用空格分隔"
    ),
  ];
}

function parameter(
  name: string,
  location: OAuthGuideParameter["location"],
  required: boolean,
  value: string,
  description: string
): OAuthGuideParameter {
  return { name, location, required, value, description };
}

function request(
  key: string,
  title: string,
  method: OAuthGuideRequest["method"],
  url: string,
  parameters: OAuthGuideParameter[],
  example: string,
  responseExample?: string
): OAuthGuideRequest {
  return { key, title, method, url, parameters, example, responseExample };
}

function buildGetCurl(url: string, parameters: OAuthGuideParameter[]): string {
  const data = parameters
    .filter((item) => item.location === "Query" && item.name !== "scope")
    .map((item) => `  --data-urlencode '${item.name}=${item.value}'`)
    .join(" \\\n");
  return `curl --get '${url}' \\\n${data}`;
}

function buildPostCurl(
  url: string,
  parameters: OAuthGuideParameter[],
  authenticationMethod: OAuthClientAuthenticationMethod,
  clientId?: string
): string {
  const lines = [`curl --request POST '${url}'`];
  if (authenticationMethod === "CLIENT_SECRET_BASIC") {
    lines.push(`  --user '${clientId || ""}:${CLIENT_SECRET_PLACEHOLDER}'`);
  }
  lines.push("  --header 'Content-Type: application/x-www-form-urlencoded'");
  parameters
    .filter((item) => item.location === "Body" && item.name !== "scope")
    .forEach((item) =>
      lines.push(`  --data-urlencode '${item.name}=${item.value}'`)
    );
  return lines.join(" \\\n");
}

function tokenResponse(
  client: OAuthClientInfo,
  includeRefreshToken = true
): string {
  let response = TOKEN_RESPONSE.replace(
    "7200",
    String(client.accessTokenTtlSeconds || 7200)
  );
  if (
    !includeRefreshToken ||
    !client.authorizationGrantTypes?.includes("REFRESH_TOKEN")
  ) {
    response = response.replace(',\n    "refresh_token": "{Refresh Token}"', "");
  }
  return response;
}

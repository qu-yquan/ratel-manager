# OAuth2 Unified Response And Consent Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wrap OAuth2 JSON endpoints in the project `R` contract and make the consent page reliably load the configured application name and Scope permissions without stale frontend authentication state.

**Architecture:** Spring Authorization Server endpoint handlers delegate OAuth response mapping to focused writers while browser redirect endpoints remain untouched. `LoginWebConfiguration` exposes `/system/auth/oauth2/consent-context` and obtains its data through `OAuthClientApi`, preserving local/distributed deployment behavior. The consent frontend uses request-level authentication suppression and renders only server-approved permissions.

**Tech Stack:** Java 25, Spring Boot 4, Spring Security Authorization Server 7, Jackson 3, JUnit 5, UmiJS 4, React 19, TypeScript.

---

### Task 1: OAuth2 unified JSON response handlers

**Files:**
- Create: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/response/OAuth2ResponseWriter.java`
- Create: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/response/OAuth2ErrorResponseHandler.java`
- Create focused success handlers under `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/response/`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/oauth/token/CustomOAuth2AccessTokenResponseSuccessHandler.java`
- Test: `common/common-authentication/src/test/java/org/quyq/gwsu/common/authentication/oauth/response/OAuth2ResponseWriterTest.java`

- [ ] **Step 1: Write failing response contract tests**

Assert that success serializes as `R.ok(oauthData)` and failure preserves HTTP `400/401`, OAuth error fields, and authentication headers.

- [ ] **Step 2: Run the focused tests and verify failure**

Run: `mvn -f common/pom.xml -pl :common-authentication -am -DskipTests=false -Dtest=OAuth2ResponseWriterTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation failure because the writer does not exist.

- [ ] **Step 3: Implement the shared writer and endpoint handlers**

Use the application `tools.jackson.databind.ObjectMapper`; write `R.ok(Map<String,Object>)` for success and `new R<>(httpStatus, message, oauthErrorData, null)` for errors. Map Token, device authorization, introspection, and revocation `Authentication` results without changing OAuth field names. Keep `CustomOAuthSubjectWriter.write(...)` before emitting a Token response.

- [ ] **Step 4: Configure all native endpoint hooks**

Modify `OAuthSecurityConfiguration` to configure `accessTokenResponseHandler/errorResponseHandler`, `deviceAuthorizationResponseHandler/errorResponseHandler`, `introspectionResponseHandler/errorResponseHandler`, and `revocationResponseHandler/errorResponseHandler`.

- [ ] **Step 5: Add the exact-path JWKS wrapper**

Create a response filter that matches only the configured JWK Set endpoint, skips redirects/non-JSON/already wrapped bodies, and wraps the JWK Set under `R.data`.

- [ ] **Step 6: Run common authentication tests**

Run: `mvn -f common/pom.xml -pl :common-authentication -am -DskipTests=false install`

Expected: all modules succeed with zero failures.

### Task 2: Consent context API across deployment modes

**Files:**
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/api/oauth/vo/OAuthConsentContextVO.java`
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/api/oauth/OAuthClientApi.java`
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/api/oauth/fallback/OAuthClientApiFallbackFactory.java`
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/oauth/controller/SecurityOAuthClientController.java`
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/oauth/service/ISecurityOAuthClientService.java`
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/oauth/service/impl/SecurityOAuthClientServiceImpl.java`
- Modify: `common/common-authentication/src/main/java/org/quyq/gwsu/common/authentication/config/LoginWebConfiguration.java`
- Test: `business/business-security/business-security-server/src/test/java/org/quyq/gwsu/security/oauth/service/impl/SecurityOAuthClientServiceImplTest.java`

- [ ] **Step 1: Write failing context resolution tests**

Cover explicit requested Scope, omitted Scope fallback, disabled/missing client, unassigned requested Scope, built-in Scope description, and no configured Scope with `canAuthorize=false`.

- [ ] **Step 2: Add the shared API contract**

Add `OAuthClientApi#getConsentContext(String clientId, String scope)` and its fallback. The VO contains `clientId`, `clientName`, `List<OAuthScopeVO> scopes`, and `boolean canAuthorize`.

- [ ] **Step 3: Implement security service resolution**

Load only enabled clients, parse space-delimited requested Scope values, reject values outside the client's configured whitelist, fall back to all configured client Scopes when omitted, and resolve Chinese descriptions through `ISecurityOAuthScopeService.listByCodes(...)`.

- [ ] **Step 4: Expose the authentication route**

Register `GET buildPath("/auth/oauth2/consent-context")` in `LoginWebConfiguration`. Validate `clientId`, call `OAuthClientApi`, unwrap through the existing API-client utility, and return `R.ok(context)`. Do not add a public security-controller route.

- [ ] **Step 5: Run business and common tests**

Run the security server focused tests and then the common authentication reactor build. Expected: zero failures.

### Task 3: Request-level stale Token isolation and consent UI

**Files:**
- Modify: `web/gwsu-core/src/types/request.ts`
- Modify: `web/gwsu-core/src/utils/request.ts`
- Modify: `web/apps/gwsu-sub-system/src/pages/oauth2/consent.tsx`
- Modify: `web/apps/gwsu-sub-system/src/pages/oauth2/consent.module.less`
- Modify: `web/apps/gwsu-sub-system/src/pages/oauth2/utils.ts`
- Test: `web/apps/gwsu-sub-system/src/pages/oauth2/utils.test.ts`

- [ ] **Step 1: Add request option tests or pure interceptor tests**

Verify `skipAuth` prevents an Authorization header and `skipUnauthorizedRedirect` suppresses user-state clearing plus `TOKEN_EXPIRED` emission while still rejecting the request.

- [ ] **Step 2: Implement request options**

Extend `RequestOptions` with optional `skipAuth` and `skipUnauthorizedRedirect`. Respect them in the default request and response interceptors without changing existing callers.

- [ ] **Step 3: Replace consent page data loading**

Call `/system/auth/oauth2/consent-context` once with `{skipAuth:true, skipUnauthorizedRedirect:true}`. Render `clientName`; render returned Scope names/descriptions; submit selected server-approved Scope codes.

- [ ] **Step 4: Implement blocked and error states**

While loading or on error, hide submit actions. When `canAuthorize=false`, show exactly `未配置任何授权权限，请联系管理员` and hide both consent buttons.

- [ ] **Step 5: Build the system frontend**

Run: `pnpm --filter gwsu-sub-system build`

Expected: Webpack compilation succeeds.

### Task 4: Integration guide and end-to-end verification

**Files:**
- Modify: `web/apps/gwsu-sub-security/src/pages/oauthclient/utils/integrationGuide.ts`
- Modify: `web/apps/gwsu-sub-security/src/pages/oauthclient/utils/integrationGuide.test.ts`

- [ ] **Step 1: Update success and error examples**

Wrap Token and device response examples in `{code,msg,data}` and describe reading fields from `data`. Add an OAuth error wrapper example without exposing real client secrets.

- [ ] **Step 2: Verify frontend builds**

Run: `pnpm --filter gwsu-sub-security build` and `pnpm --filter gwsu-sub-system build`.

Expected: both builds succeed.

- [ ] **Step 3: Run final backend verification**

Run common authentication and business security test commands from Tasks 1 and 2. Expected: all tests pass.

- [ ] **Step 4: Perform static checks**

Run `git diff --check` and search for the obsolete `/security/oauth/client/consent-context` path. Expected: no whitespace errors and no obsolete route references.

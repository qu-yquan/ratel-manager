import type {
  OAuthGuideCodeExample,
  OAuthGuideRequest,
  OAuthIntegrationGuide,
} from "../types";

export function renderIntegrationGuideHtml(
  guide: OAuthIntegrationGuide
): string {
  const { client } = guide;
  const title = `${client.clientName} OAuth2对接指南`;
  const grantTabs = guide.sections
    .map(
      (section, index) => `
    <button
      type="button"
      role="tab"
      id="grant-tab-${section.grantType}"
      aria-controls="grant-panel-${section.grantType}"
      aria-selected="${index === 0}"
      tabindex="${index === 0 ? "0" : "-1"}"
      data-grant-target="${section.grantType}"
    >${escapeHtml(section.title)}</button>
  `
    )
    .join("");
  const sections = guide.sections
    .map(
      (section, index) => `
    <section
      class="grant-panel"
      id="grant-panel-${section.grantType}"
      role="tabpanel"
      aria-labelledby="grant-tab-${section.grantType}"
      data-grant-panel="${section.grantType}"
      ${index === 0 ? "" : "hidden"}
    >
      <h2>${escapeHtml(section.title)}</h2>
      <p class="section-description">${escapeHtml(section.description)}</p>
      <ol class="steps">
        ${section.steps
          .map(
            (step) => `
          <li>
            <h3>${escapeHtml(step.title)}</h3>
            ${step.description ? `<p>${escapeHtml(step.description)}</p>` : ""}
            ${step.codeExamples?.map(renderCodeExample).join("") || ""}
            ${step.requests?.map(renderRequest).join("") || ""}
          </li>
        `
          )
          .join("")}
      </ol>
    </section>
  `
    )
    .join("");

  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(title)}</title>
  <style>
    :root { color-scheme: light; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #1f2329; background: #f5f7fa; }
    * { box-sizing: border-box; }
    body { margin: 0; line-height: 1.65; }
    main { width: min(1080px, calc(100% - 32px)); margin: 32px auto; padding: 32px; background: #fff; border: 1px solid #d9dee7; border-radius: 8px; }
    h1 { margin: 0 0 24px; font-size: 28px; }
    h2 { margin: 36px 0 8px; padding-bottom: 8px; border-bottom: 1px solid #d9dee7; font-size: 22px; }
    h3 { margin: 0 0 8px; font-size: 17px; }
    h4 { margin: 0; font-size: 15px; }
    p { margin: 8px 0 14px; }
    code, pre { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
    code { overflow-wrap: anywhere; }
    pre { margin: 10px 0 0; padding: 14px; overflow: auto; border: 1px solid #d9dee7; border-radius: 6px; background: #f6f8fa; font-size: 13px; line-height: 1.6; white-space: pre; }
    table { width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 14px; }
    th, td { padding: 8px 10px; border: 1px solid #d9dee7; text-align: left; vertical-align: top; overflow-wrap: anywhere; }
    th { background: #f2f4f7; }
    .summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0; border-top: 1px solid #d9dee7; border-left: 1px solid #d9dee7; }
    .summary div { display: grid; grid-template-columns: 150px minmax(0, 1fr); border-right: 1px solid #d9dee7; border-bottom: 1px solid #d9dee7; }
    .summary dt, .summary dd { margin: 0; padding: 9px 12px; overflow-wrap: anywhere; }
    .summary dt { background: #f2f4f7; font-weight: 600; }
    .notice { margin: 18px 0 8px; padding: 10px 12px; border-left: 4px solid #d4380d; background: #fff2e8; color: #a8071a; }
    .grant-tabs { display: flex; gap: 4px; margin-top: 28px; overflow-x: auto; border-bottom: 1px solid #d9dee7; }
    .grant-tabs button { flex: 0 0 auto; padding: 10px 16px; border: 0; border-bottom: 3px solid transparent; background: transparent; color: #586174; cursor: pointer; font: inherit; font-weight: 600; }
    .grant-tabs button:hover { color: #0958d9; }
    .grant-tabs button[aria-selected="true"] { border-bottom-color: #1677ff; color: #0958d9; }
    .grant-tabs button:focus-visible { outline: 2px solid #1677ff; outline-offset: -2px; }
    .grant-panel[hidden] { display: none; }
    .section-description { color: #586174; }
    .steps { padding-left: 24px; }
    .steps > li { margin: 20px 0 28px; padding-left: 4px; }
    .code-example, .request { margin: 14px 0; }
    .code-example h4 { margin-bottom: 6px; }
    .request { border: 1px solid #d9dee7; border-radius: 6px; overflow: hidden; }
    .request-header { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding: 10px 12px; background: #f2f4f7; }
    .method { padding: 1px 7px; border-radius: 4px; background: #1677ff; color: #fff; font-size: 12px; font-weight: 700; }
    .request-body { padding: 12px; }
    .response-title { margin-top: 16px; font-weight: 600; }
    @media (max-width: 720px) {
      main { width: 100%; margin: 0; padding: 20px 16px; border: 0; border-radius: 0; }
      .summary { grid-template-columns: 1fr; }
      .summary div { grid-template-columns: 120px minmax(0, 1fr); }
      table { display: block; overflow-x: auto; white-space: nowrap; }
    }
    @media print {
      :root { background: #fff; }
      main { width: 100%; margin: 0; padding: 0; border: 0; }
      .grant-tabs { display: none; }
      .grant-panel[hidden] { display: block !important; }
      .request, pre, table { break-inside: avoid; }
    }
  </style>
</head>
<body>
  <main>
    <h1>${escapeHtml(title)}</h1>
    <dl class="summary">
      ${summaryItem("客户端ID", client.clientId || "-")}
      ${summaryItem("客户端类型", guide.clientTypeLabel)}
      ${summaryItem("账号体系", guide.accountTypeLabel)}
      ${summaryItem("客户端认证方式", guide.authenticationMethodLabel)}
      ${summaryItem(
        "Access Token有效期",
        `${client.accessTokenTtlSeconds || 7200}秒`
      )}
      ${summaryItem(
        "Refresh Token有效期",
        `${client.refreshTokenTtlSeconds || 2592000}秒`
      )}
    </dl>
    <p class="notice">客户端密钥不会写入指南，请将 <code>{客户端密钥}</code> 替换为创建或重置应用时获得的密钥。</p>
    <nav class="grant-tabs" role="tablist" aria-label="授权模式">
      ${grantTabs}
    </nav>
    <noscript><style>.grant-tabs { display: none; } .grant-panel[hidden] { display: block; }</style></noscript>
    ${sections}
  </main>
  <script>
    (() => {
      const tabs = Array.from(document.querySelectorAll('[data-grant-target]'));
      const panels = Array.from(document.querySelectorAll('[data-grant-panel]'));

      function activateTab(tab) {
        const target = tab.dataset.grantTarget;
        tabs.forEach((item) => {
          const active = item === tab;
          item.setAttribute('aria-selected', String(active));
          item.tabIndex = active ? 0 : -1;
        });
        panels.forEach((panel) => {
          panel.hidden = panel.dataset.grantPanel !== target;
        });
        tab.focus();
      }

      tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => activateTab(tab));
        tab.addEventListener('keydown', (event) => {
          if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
            return;
          }
          event.preventDefault();
          let nextIndex = index;
          if (event.key === 'ArrowLeft') nextIndex = (index - 1 + tabs.length) % tabs.length;
          if (event.key === 'ArrowRight') nextIndex = (index + 1) % tabs.length;
          if (event.key === 'Home') nextIndex = 0;
          if (event.key === 'End') nextIndex = tabs.length - 1;
          activateTab(tabs[nextIndex]);
        });
      });
    })();
  </script>
</body>
</html>
`;
}

export function buildIntegrationGuideFileName(
  clientName: string,
  clientId?: string
): string {
  const rawName = `${clientName}-${clientId || "client"}-OAuth2对接指南.html`;
  return rawName.replace(/[\\/:*?"<>|]/g, "-");
}

export function downloadIntegrationGuide(guide: OAuthIntegrationGuide): void {
  const html = renderIntegrationGuideHtml(guide);
  const blob = new Blob([html], { type: "text/html;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = buildIntegrationGuideFileName(
    guide.client.clientName,
    guide.client.clientId
  );
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function summaryItem(label: string, value: string): string {
  return `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(
    value
  )}</dd></div>`;
}

function renderCodeExample(example: OAuthGuideCodeExample): string {
  return `
    <section class="code-example">
      <h4>${escapeHtml(example.label)}示例</h4>
      <pre><code>${escapeHtml(example.code)}</code></pre>
    </section>
  `;
}

function renderRequest(request: OAuthGuideRequest): string {
  const parameters =
    request.parameters.length > 0
      ? `
    <table>
      <thead><tr><th>参数</th><th>位置</th><th>必填</th><th>示例值</th><th>说明</th></tr></thead>
      <tbody>
        ${request.parameters
          .map(
            (item) => `
          <tr>
            <td><code>${escapeHtml(item.name)}</code></td>
            <td>${escapeHtml(item.location)}</td>
            <td>${item.required ? "是" : "否"}</td>
            <td><code>${escapeHtml(item.value)}</code></td>
            <td>${escapeHtml(item.description)}</td>
          </tr>
        `
          )
          .join("")}
      </tbody>
    </table>
  `
      : "";
  const response = request.responseExample
    ? `
    <div class="response-title">响应示例</div>
    <pre><code>${escapeHtml(request.responseExample)}</code></pre>
  `
    : "";

  return `
    <section class="request">
      <header class="request-header">
        <span class="method">${escapeHtml(request.method)}</span>
        <h4>${escapeHtml(request.title)}</h4>
        <code>${escapeHtml(request.url)}</code>
      </header>
      <div class="request-body">
        ${parameters}
        <div class="response-title">curl示例</div>
        <pre><code>${escapeHtml(request.example)}</code></pre>
        ${response}
      </div>
    </section>
  `;
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

import React, { useEffect, useMemo, useState } from "react";
import {
  Alert,
  App,
  Button,
  Descriptions,
  Drawer,
  Empty,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
} from "antd";
import type { TableProps } from "antd";
import { CopyOutlined, DownloadOutlined } from "@ant-design/icons";
import type {
  OAuthClientAuthenticationMethod,
  OAuthClientInfo,
  OAuthClientType,
  OAuthGrantType,
  OAuthGuideCodeExample,
  OAuthGuideParameter,
  OAuthGuideRequest,
} from "../../types";
import {
  buildOAuthIntegrationGuide,
  getAvailableAuthenticationMethods,
} from "../../utils/integrationGuide";
import { downloadIntegrationGuide } from "../../utils/integrationGuideExport";
import styles from "./index.module.less";

interface OAuthClientIntegrationGuideDrawerProps {
  visible: boolean;
  loading: boolean;
  data?: OAuthClientInfo | null;
  apiBaseUrl: string;
  clientTypeLabels: Map<OAuthClientType, string>;
  accountTypeLabels: Map<string, string>;
  grantTypeLabels: Map<OAuthGrantType, string>;
  authenticationMethodLabels: Map<OAuthClientAuthenticationMethod, string>;
  onClose: () => void;
}

const parameterColumns: TableProps<OAuthGuideParameter>["columns"] = [
  { title: "参数", dataIndex: "name", width: 150 },
  { title: "位置", dataIndex: "location", width: 80 },
  {
    title: "必填",
    dataIndex: "required",
    width: 70,
    render: (value: boolean) => (value ? "是" : "否"),
  },
  {
    title: "示例值",
    dataIndex: "value",
    width: 240,
    render: (value: string) => <Typography.Text code>{value}</Typography.Text>,
  },
  { title: "说明", dataIndex: "description" },
];

const OAuthClientIntegrationGuideDrawer: React.FC<
  OAuthClientIntegrationGuideDrawerProps
> = ({
  visible,
  loading,
  data,
  apiBaseUrl,
  clientTypeLabels,
  accountTypeLabels,
  grantTypeLabels,
  authenticationMethodLabels,
  onClose,
}) => {
  const { message } = App.useApp();
  const [activeGrantType, setActiveGrantType] = useState<OAuthGrantType>();
  const [authenticationMethod, setAuthenticationMethod] =
    useState<OAuthClientAuthenticationMethod>();
  const [redirectUri, setRedirectUri] = useState<string>();

  const authenticationMethods = useMemo(
    () => (data ? getAvailableAuthenticationMethods(data) : []),
    [data]
  );

  useEffect(() => {
    if (!visible || !data) {
      return;
    }
    setActiveGrantType(undefined);
    setAuthenticationMethod(authenticationMethods[0]);
    setRedirectUri(data.redirectUris?.[0]);
  }, [authenticationMethods, data, visible]);

  const guide = useMemo(
    () =>
      data
        ? buildOAuthIntegrationGuide(data, {
            apiBaseUrl,
            authenticationMethod,
            redirectUri,
            clientTypeLabel: clientTypeLabels.get(data.clientType),
            accountTypeLabel: accountTypeLabels.get(data.accountType || ""),
            authenticationMethodLabel: authenticationMethodLabels.get(
              authenticationMethod || "NONE"
            ),
          })
        : null,
    [
      accountTypeLabels,
      apiBaseUrl,
      authenticationMethod,
      authenticationMethodLabels,
      clientTypeLabels,
      data,
      redirectUri,
    ]
  );

  useEffect(() => {
    if (
      guide &&
      !guide.sections.some((section) => section.grantType === activeGrantType)
    ) {
      setActiveGrantType(guide.sections[0]?.grantType);
    }
  }, [activeGrantType, guide]);

  const handleCopy = async (content: string) => {
    try {
      await navigator.clipboard.writeText(content);
      message.success("已复制");
    } catch {
      message.error("复制失败，请手动选择复制");
    }
  };

  const extra = guide ? (
    <Button
      icon={<DownloadOutlined />}
      onClick={() => downloadIntegrationGuide(guide)}
    >
      导出指南
    </Button>
  ) : null;

  return (
    <Drawer
      title={data ? `${data.clientName} - 对接指南` : "OAuth2对接指南"}
      open={visible}
      width="min(920px, 100vw)"
      extra={extra}
      onClose={onClose}
      destroyOnHidden
    >
      <Spin spinning={loading}>
        {!loading && !data && <Empty description="未能加载应用配置" />}
        {data && guide && (
          <div className={styles.guideContent}>
            {data.status !== "ENABLED" && (
              <Alert
                type="warning"
                showIcon
                message="当前应用已禁用，启用后才能正常完成OAuth2授权。"
              />
            )}

            <Descriptions
              className={styles.summary}
              bordered
              size="small"
              column={{ xs: 1, sm: 2 }}
            >
              <Descriptions.Item label="客户端ID">
                <Typography.Text copyable={{ text: data.clientId || "" }}>
                  {data.clientId || "-"}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="客户端类型">
                {clientTypeLabels.get(data.clientType) || data.clientType}
              </Descriptions.Item>
              <Descriptions.Item label="账号体系">
                {accountTypeLabels.get(data.accountType || "") ||
                  data.accountType ||
                  "-"}
              </Descriptions.Item>
              <Descriptions.Item label="客户端认证方式">
                {authenticationMethodLabels.get(guide.authenticationMethod) ||
                  guide.authenticationMethod}
              </Descriptions.Item>
              <Descriptions.Item label="Access Token有效期">
                {data.accessTokenTtlSeconds || 7200}秒
              </Descriptions.Item>
              <Descriptions.Item label="Refresh Token有效期">
                {data.refreshTokenTtlSeconds || 2592000}秒
              </Descriptions.Item>
            </Descriptions>

            <div className={styles.controls}>
              {authenticationMethods.length > 1 && (
                <div className={styles.controlItem}>
                  <span className={styles.controlLabel}>客户端认证方式</span>
                  <Select
                    aria-label="客户端认证方式"
                    value={authenticationMethod}
                    options={authenticationMethods.map((method) => ({
                      value: method,
                      label: authenticationMethodLabels.get(method) || method,
                    }))}
                    onChange={setAuthenticationMethod}
                  />
                </div>
              )}
              {activeGrantType === "AUTHORIZATION_CODE" &&
                data.redirectUris?.length > 1 && (
                  <div className={styles.controlItem}>
                    <span className={styles.controlLabel}>重定向URI</span>
                    <Select
                      aria-label="重定向URI"
                      value={redirectUri}
                      options={data.redirectUris.map((uri) => ({
                        value: uri,
                        label: uri,
                      }))}
                      onChange={setRedirectUri}
                    />
                  </div>
                )}
            </div>

            {guide.sections.length === 0 ? (
              <Empty description="当前应用未配置授权模式" />
            ) : (
              <Tabs
                activeKey={activeGrantType}
                onChange={(key) => setActiveGrantType(key as OAuthGrantType)}
                items={guide.sections.map((section) => ({
                  key: section.grantType,
                  label:
                    grantTypeLabels.get(section.grantType) || section.title,
                  children: (
                    <div className={styles.sectionContent}>
                      <Typography.Paragraph type="secondary">
                        {section.description}
                      </Typography.Paragraph>
                      <ol className={styles.stepList}>
                        {section.steps.map((step) => (
                          <li key={step.title} className={styles.stepItem}>
                            <Typography.Title level={5}>
                              {step.title}
                            </Typography.Title>
                            {step.description && (
                              <Typography.Paragraph>
                                {step.description}
                              </Typography.Paragraph>
                            )}
                            {step.codeExamples && (
                              <GuideCodeExamples
                                examples={step.codeExamples}
                                onCopy={handleCopy}
                              />
                            )}
                            {step.requests?.map((request) => (
                              <GuideRequestView
                                key={request.key}
                                request={request}
                                onCopy={handleCopy}
                              />
                            ))}
                          </li>
                        ))}
                      </ol>
                    </div>
                  ),
                }))}
              />
            )}
          </div>
        )}
      </Spin>
    </Drawer>
  );
};

interface GuideCodeExamplesProps {
  examples: OAuthGuideCodeExample[];
  onCopy: (content: string) => Promise<void>;
}

const GuideCodeExamples: React.FC<GuideCodeExamplesProps> = ({
  examples,
  onCopy,
}) => (
  <div className={styles.codeExamples}>
    <Tabs
      size="small"
      items={examples.map((example) => ({
        key: example.language,
        label: example.label,
        children: (
          <div className={styles.codeExample}>
            <div className={styles.codeHeader}>
              <span>{example.label}示例</span>
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                aria-label={`复制${example.label} PKCE示例`}
                onClick={() => void onCopy(example.code)}
              />
            </div>
            <pre className={styles.codeBlock}>
              <code>{example.code}</code>
            </pre>
          </div>
        ),
      }))}
    />
  </div>
);

interface GuideRequestViewProps {
  request: OAuthGuideRequest;
  onCopy: (content: string) => Promise<void>;
}

const GuideRequestView: React.FC<GuideRequestViewProps> = ({
  request,
  onCopy,
}) => (
  <section className={styles.requestSection}>
    <div className={styles.requestHeader}>
      <Space>
        <Tag color={request.method === "GET" ? "blue" : "green"}>
          {request.method}
        </Tag>
        <Typography.Text strong>{request.title}</Typography.Text>
      </Space>
      <Typography.Text code copyable={{ text: request.url }}>
        {request.url}
      </Typography.Text>
    </div>
    {request.parameters.length > 0 && (
      <Table
        aria-label={`${request.title}参数`}
        rowKey={(record) => `${record.location}-${record.name}`}
        size="small"
        columns={parameterColumns}
        dataSource={request.parameters}
        pagination={false}
        scroll={{ x: 760 }}
      />
    )}
    <div className={styles.codeHeader}>
      <span>curl示例</span>
      <Button
        type="text"
        size="small"
        icon={<CopyOutlined />}
        aria-label={`复制${request.title}curl示例`}
        onClick={() => void onCopy(request.example)}
      />
    </div>
    <pre className={styles.codeBlock}>
      <code>{request.example}</code>
    </pre>
    {request.responseExample && (
      <>
        <div className={styles.codeHeader}>
          <span>响应示例</span>
          <Button
            type="text"
            size="small"
            icon={<CopyOutlined />}
            aria-label={`复制${request.title}响应示例`}
            onClick={() => void onCopy(request.responseExample!)}
          />
        </div>
        <pre className={styles.codeBlock}>
          <code>{request.responseExample}</code>
        </pre>
      </>
    )}
  </section>
);

export default OAuthClientIntegrationGuideDrawer;

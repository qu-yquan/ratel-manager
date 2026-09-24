import React, { useCallback, useMemo } from "react";
import {
  Alert,
  App,
  Button,
  Drawer,
  Empty,
  Spin,
  Tabs,
  Tag,
  Tooltip,
} from "antd";
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  CloseCircleFilled,
  CopyOutlined,
  DesktopOutlined,
  LinkOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";

import type { LoginLogItem, OperationLogItem } from "../../types";

import styles from "./index.module.less";

type DetailKind = "login" | "operation";

interface LogDetailDrawerProps {
  open: boolean;
  loading: boolean;
  kind: DetailKind;
  data: LoginLogItem | OperationLogItem | null;
  onClose: () => void;
}

interface InfoItem {
  label: string;
  value?: React.ReactNode;
  wide?: boolean;
}

const EMPTY_VALUE = <span className={styles.emptyValue}>—</span>;

function formatTime(value?: string): string {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss.SSS") : "";
}

function formatPayload(value?: string): string {
  if (!value) {
    return "";
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

const InfoGrid: React.FC<{ items: InfoItem[] }> = ({ items }) => (
  <div className={styles.infoGrid}>
    {items.map((item) => (
      <div
        className={`${styles.infoItem} ${item.wide ? styles.infoItemWide : ""}`}
        key={item.label}
      >
        <span className={styles.infoLabel}>{item.label}</span>
        <div className={styles.infoValue}>{item.value || EMPTY_VALUE}</div>
      </div>
    ))}
  </div>
);

const DetailSection: React.FC<{
  icon: React.ReactNode;
  title: string;
  children: React.ReactNode;
}> = ({ icon, title, children }) => (
  <section className={styles.section}>
    <div className={styles.sectionTitle}>
      <span className={styles.sectionIcon} aria-hidden="true">
        {icon}
      </span>
      <h3>{title}</h3>
    </div>
    {children}
  </section>
);

const PayloadBlock: React.FC<{ value?: string; label: string }> = ({
  value,
  label,
}) => {
  const { message } = App.useApp();
  const formatted = useMemo(() => formatPayload(value), [value]);

  const handleCopy = useCallback(async () => {
    if (!formatted) {
      return;
    }
    try {
      await navigator.clipboard.writeText(formatted);
      message.success("已复制");
    } catch {
      message.error("复制失败");
    }
  }, [formatted, message]);

  if (!formatted) {
    return (
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无内容" />
    );
  }

  return (
    <div className={styles.payloadBlock}>
      <div className={styles.payloadToolbar}>
        <span>{label}</span>
        <Tooltip title="复制内容">
          <Button
            aria-label={`复制${label}`}
            icon={<CopyOutlined aria-hidden="true" />}
            onClick={() => void handleCopy()}
            size="small"
            type="text"
          />
        </Tooltip>
      </div>
      <pre className={styles.payloadContent}>{formatted}</pre>
    </div>
  );
};

function renderStatus(status?: boolean) {
  return status ? (
    <Tag color="success" icon={<CheckCircleFilled aria-hidden="true" />}>
      成功
    </Tag>
  ) : (
    <Tag color="error" icon={<CloseCircleFilled aria-hidden="true" />}>
      失败
    </Tag>
  );
}

const LoginDetail: React.FC<{ data: LoginLogItem }> = ({ data }) => {
  const sessionStatus = !data.status
    ? "未创建会话"
    : data.endType === "LOGOUT"
    ? "主动退出"
    : data.endType === "EXPIRE"
    ? "会话过期"
    : "当前在线";

  return (
    <>
      <div className={styles.hero}>
        <div
          className={`${styles.heroIcon} ${
            data.status ? styles.heroSuccess : styles.heroError
          }`}
        >
          <SafetyCertificateOutlined aria-hidden="true" />
        </div>
        <div className={styles.heroContent}>
          <span className={styles.eyebrow}>认证事件</span>
          <h2>{data.loginAccount || "未知账号"}</h2>
          <div className={styles.heroMeta}>
            {renderStatus(data.status)}
            <span>{data.userName || "未识别用户"}</span>
            <span>{formatTime(data.loginTime)}</span>
          </div>
        </div>
      </div>

      {!data.status && data.failureMessage ? (
        <Alert
          className={styles.failureAlert}
          description={data.failureMessage}
          message={data.failureCode || "认证失败"}
          showIcon
          type="error"
        />
      ) : null}

      <DetailSection icon={<UserOutlined />} title="身份与认证">
        <InfoGrid
          items={[
            { label: "登录账号", value: data.loginAccount },
            { label: "用户名称", value: data.userName },
            { label: "账号类型", value: data.accountType },
            { label: "访问者类型", value: data.visitorType },
            { label: "登录方式", value: data.loginType },
            { label: "授权类型", value: data.grantType },
            { label: "客户端标识", value: data.clientId },
            { label: "用户标识", value: data.userId },
          ]}
        />
      </DetailSection>

      <DetailSection icon={<DesktopOutlined />} title="访问环境">
        <InfoGrid
          items={[
            { label: "客户端 IP", value: data.clientIp },
            { label: "终端类型", value: data.terminal },
            { label: "终端详情", value: data.terminalDetail, wide: true },
          ]}
        />
      </DetailSection>

      <DetailSection icon={<ClockCircleOutlined />} title="会话周期">
        <InfoGrid
          items={[
            { label: "会话状态", value: sessionStatus },
            { label: "结束类型", value: data.endType },
            { label: "登录时间", value: formatTime(data.loginTime) },
            { label: "结束时间", value: formatTime(data.endTime) },
            {
              label: "认证会话标识",
              value: <code>{data.authorizationId}</code>,
              wide: true,
            },
          ]}
        />
      </DetailSection>
    </>
  );
};

const OperationDetail: React.FC<{ data: OperationLogItem }> = ({ data }) => {
  const payloadItems = [
    {
      key: "request",
      label: "请求参数",
      children: <PayloadBlock label="请求参数" value={data.requestParam} />,
    },
    {
      key: "response",
      label: "响应数据",
      children: <PayloadBlock label="响应数据" value={data.responseData} />,
    },
    {
      key: "error",
      label: "错误信息",
      children: <PayloadBlock label="错误信息" value={data.errorMsg} />,
    },
  ];

  return (
    <>
      <div className={styles.hero}>
        <div
          className={`${styles.heroIcon} ${
            data.status ? styles.heroSuccess : styles.heroError
          }`}
        >
          <LinkOutlined aria-hidden="true" />
        </div>
        <div className={styles.heroContent}>
          <span className={styles.eyebrow}>链路操作</span>
          <h2>{data.apiDescription || data.requestUrl || "未知操作"}</h2>
          <div className={styles.heroMeta}>
            {renderStatus(data.status)}
            <span>{data.operName || "匿名操作"}</span>
            <span>{data.consumeMill ?? 0} ms</span>
          </div>
        </div>
      </div>

      {data.errorMsg ? (
        <Alert
          className={styles.failureAlert}
          description="该节点执行失败，请在下方错误信息中查看完整上下文。"
          message="链路节点异常"
          showIcon
          type="error"
        />
      ) : null}

      <DetailSection icon={<UserOutlined />} title="操作信息">
        <InfoGrid
          items={[
            { label: "操作人", value: data.operName },
            {
              label: "操作主体",
              value: data.operSubject === 1 ? "智能助手" : "用户",
            },
            { label: "接口模块", value: data.apiModule },
            { label: "接口说明", value: data.apiDescription },
            { label: "请求方式", value: <code>{data.requestMethod}</code> },
            { label: "耗时", value: `${data.consumeMill ?? 0} ms` },
            {
              label: "请求路径",
              value: <code>{data.requestUrl}</code>,
              wide: true,
            },
            {
              label: "方法签名",
              value: <code>{data.method}</code>,
              wide: true,
            },
          ]}
        />
      </DetailSection>

      <DetailSection icon={<LinkOutlined />} title="链路信息">
        <InfoGrid
          items={[
            { label: "来源服务", value: data.fromApp },
            { label: "所属服务", value: data.modulePrefix },
            { label: "请求时间", value: formatTime(data.requestTime) },
            { label: "响应时间", value: formatTime(data.responseTime) },
            {
              label: "日志标识",
              value: <code>{data.operId}</code>,
              wide: true,
            },
            {
              label: "父节点标识",
              value: <code>{data.parentId}</code>,
              wide: true,
            },
            { label: "Trace ID", value: <code>{data.tid}</code>, wide: true },
            {
              label: "认证会话标识",
              value: <code>{data.authorizationId}</code>,
              wide: true,
            },
          ]}
        />
      </DetailSection>

      <DetailSection icon={<DesktopOutlined />} title="终端信息">
        <InfoGrid
          items={[
            { label: "终端类型", value: data.terminal },
            { label: "终端详情", value: data.terminalDetail, wide: true },
          ]}
        />
      </DetailSection>

      <section className={styles.section}>
        <Tabs className={styles.payloadTabs} items={payloadItems} />
      </section>
    </>
  );
};

const LogDetailDrawer: React.FC<LogDetailDrawerProps> = ({
  open,
  loading,
  kind,
  data,
  onClose,
}) => (
  <Drawer
    className={styles.drawer}
    destroyOnHidden
    onClose={onClose}
    open={open}
    title={kind === "login" ? "登录日志详情" : "操作日志详情"}
    width={760}
  >
    <Spin spinning={loading}>
      {data ? (
        kind === "login" ? (
          <LoginDetail data={data as LoginLogItem} />
        ) : (
          <OperationDetail data={data as OperationLogItem} />
        )
      ) : (
        <Empty description="暂无日志详情" />
      )}
    </Spin>
  </Drawer>
);

export default LogDetailDrawer;

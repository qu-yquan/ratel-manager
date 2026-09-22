import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  DatePicker,
  Form,
  Input,
  Select,
  Table,
  Tabs,
  Tag,
} from "antd";
import type { TableProps } from "antd";
import {
  EyeOutlined,
  HistoryOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import dayjs, { type Dayjs } from "dayjs";
import { AuthGate } from "@gwsu/core";
import { history } from "umi";

import LogDetailDrawer from "../log/components/LogDetailDrawer";
import { useLogPage } from "../log/hooks/useLogPage";
import {
  getLoginAccountTypes,
  getLoginLogById,
  getLoginLogPage,
} from "../log/services/log";
import type { KeyValueOption, LoginLogItem, LoginLogQuery } from "../log/types";
import { PERM_VIEW_OPERATION_LOG } from "./permissionConstants";

import styles from "../log/index.module.less";

interface LoginSearchValues {
  loginAccount?: string;
  userName?: string;
  clientIp?: string;
  loginType?: string;
  status?: number;
  loginTime?: [Dayjs, Dayjs];
}

const DEFAULT_QUERY: LoginLogQuery = {
  accountType: "MANAGER",
  pageNum: 1,
  pageSize: 20,
};

const STATUS_OPTIONS = [
  { label: "成功", value: 1 },
  { label: "失败", value: 0 },
];

const { RangePicker } = DatePicker;

function formatTime(value?: string): string {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "—";
}

function getSessionStatus(record: LoginLogItem): React.ReactNode {
  if (!record.status) {
    return <span className={styles.emptyValue}>—</span>;
  }
  if (record.endType === "LOGOUT") {
    return <Tag>已退出</Tag>;
  }
  if (record.endType === "EXPIRE") {
    return <Tag color="warning">已过期</Tag>;
  }
  return <Tag color="processing">在线</Tag>;
}

const LoginLogPage: React.FC = () => {
  const [form] = Form.useForm<LoginSearchValues>();
  const [accountTypes, setAccountTypes] = useState<KeyValueOption[]>([]);
  const [activeAccountType, setActiveAccountType] = useState("MANAGER");
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailData, setDetailData] = useState<LoginLogItem | null>(null);
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    loadPage,
    handlePageChange,
  } = useLogPage(getLoginLogPage, DEFAULT_QUERY);

  useEffect(() => {
    const loadAccountTypes = async () => {
      try {
        setAccountTypes(await getLoginAccountTypes());
      } catch {
        // 请求层已统一提示。
      }
    };
    void loadAccountTypes();
  }, []);

  const buildQuery = useCallback(
    (accountType: string, pageNum = 1): LoginLogQuery => {
      const values = form.getFieldsValue();
      return {
        accountType,
        loginAccount: values.loginAccount,
        userName: values.userName,
        clientIp: values.clientIp,
        loginType: values.loginType,
        status: values.status,
        loginTimeStart: values.loginTime?.[0]?.format("YYYY-MM-DD HH:mm:ss"),
        loginTimeEnd: values.loginTime?.[1]?.format("YYYY-MM-DD HH:mm:ss"),
        pageNum,
        pageSize,
      };
    },
    [form, pageSize]
  );

  const handleSearch = useCallback(() => {
    void loadPage(buildQuery(activeAccountType));
  }, [activeAccountType, buildQuery, loadPage]);

  const handleReset = useCallback(() => {
    form.resetFields();
    void loadPage({
      accountType: activeAccountType,
      pageNum: 1,
      pageSize,
    });
  }, [activeAccountType, form, loadPage, pageSize]);

  const handleAccountTypeChange = useCallback(
    (accountType: string) => {
      setActiveAccountType(accountType);
      void loadPage(buildQuery(accountType));
    },
    [buildQuery, loadPage]
  );

  const handleViewDetail = useCallback(async (record: LoginLogItem) => {
    setDetailData(record);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetailData(await getLoginLogById(record.id));
    } catch {
      // 列表数据仍可用于降级展示。
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const handleViewOperationLog = useCallback((record: LoginLogItem) => {
    if (!record.authorizationId) {
      return;
    }
    history.push(
      `/operation-log?authorizationId=${encodeURIComponent(
        record.authorizationId
      )}`
    );
  }, []);

  const accountTypeTabs = useMemo(
    () =>
      accountTypes.map((item) => ({
        key: item.key,
        label: <span className={styles.tabLabel}>{item.value}</span>,
      })),
    [accountTypes]
  );

  const activeAccountLabel =
    accountTypes.find((item) => item.key === activeAccountType)?.value ?? "";

  const columns: TableProps<LoginLogItem>["columns"] = [
    {
      title: "登录账号",
      dataIndex: "loginAccount",
      width: 160,
      render: (value: string, record) => (
        <div className={styles.primaryCell}>
          <span className={styles.primaryText}>{value || "—"}</span>
          <span className={styles.secondaryText}>
            {record.userName || "未识别用户"}
          </span>
        </div>
      ),
    },
    {
      title: "登录方式",
      dataIndex: "loginType",
      width: 130,
      render: (value: string) => value || "—",
    },
    {
      title: "客户端 IP",
      dataIndex: "clientIp",
      width: 140,
      render: (value: string) => (
        <code className={styles.codeText}>{value || "—"}</code>
      ),
    },
    {
      title: "终端环境",
      dataIndex: "terminalDetail",
      width: 220,
      ellipsis: true,
      render: (value: string, record) => (
        <div className={styles.primaryCell}>
          <span className={styles.primaryText}>
            {record.terminal || "未知终端"}
          </span>
          <span className={styles.secondaryText}>{value || "—"}</span>
        </div>
      ),
    },
    {
      title: "登录时间",
      dataIndex: "loginTime",
      width: 170,
      render: formatTime,
    },
    {
      title: "会话状态",
      width: 100,
      render: (_: unknown, record) => getSessionStatus(record),
    },
    {
      title: "登录结果",
      dataIndex: "status",
      width: 100,
      render: (value: boolean) => (
        <Tag color={value ? "success" : "error"}>{value ? "成功" : "失败"}</Tag>
      ),
    },
    {
      title: "操作",
      fixed: "right",
      width: 220,
      render: (_: unknown, record) => (
        <div className={styles.actionColumn}>
          <Button
            icon={<EyeOutlined aria-hidden="true" />}
            onClick={() => void handleViewDetail(record)}
            size="small"
            type="link"
          >
            详情
          </Button>
          <AuthGate buttonKey={PERM_VIEW_OPERATION_LOG}>
            <Button
              disabled={!record.authorizationId}
              icon={<HistoryOutlined aria-hidden="true" />}
              onClick={() => handleViewOperationLog(record)}
              size="small"
              type="link"
            >
              查看操作日志
            </Button>
          </AuthGate>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <Tabs
        activeKey={activeAccountType}
        className={styles.accountTabs}
        items={accountTypeTabs}
        onChange={handleAccountTypeChange}
      />

      <div className={styles.searchBar}>
        <Form className={styles.searchForm} component={false} form={form}>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="login-account">
              登录账号
            </label>
            <Form.Item name="loginAccount" noStyle>
              <Input
                allowClear
                id="login-account"
                onPressEnter={handleSearch}
                placeholder="请输入登录账号"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="login-user-name">
              用户名称
            </label>
            <Form.Item name="userName" noStyle>
              <Input
                allowClear
                id="login-user-name"
                onPressEnter={handleSearch}
                placeholder="请输入用户名称"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="login-client-ip">
              客户端 IP
            </label>
            <Form.Item name="clientIp" noStyle>
              <Input
                allowClear
                id="login-client-ip"
                onPressEnter={handleSearch}
                placeholder="请输入客户端 IP"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="login-type">
              登录方式
            </label>
            <Form.Item name="loginType" noStyle>
              <Input
                allowClear
                id="login-type"
                onPressEnter={handleSearch}
                placeholder="请输入登录方式"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="login-status">
              登录结果
            </label>
            <Form.Item name="status" noStyle>
              <Select
                allowClear
                id="login-status"
                options={STATUS_OPTIONS}
                placeholder="全部"
              />
            </Form.Item>
          </div>
          <div className={`${styles.searchItem} ${styles.searchItemWide}`}>
            <span className={styles.searchLabel}>登录时间</span>
            <Form.Item name="loginTime" noStyle>
              <RangePicker
                aria-label="登录时间范围"
                className={styles.searchControl}
                showTime
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button
            icon={<SearchOutlined aria-hidden="true" />}
            onClick={handleSearch}
            type="primary"
          >
            查询
          </Button>
          <Button
            icon={<ReloadOutlined aria-hidden="true" />}
            onClick={handleReset}
          >
            重置
          </Button>
        </div>
      </div>

      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>
            {activeAccountLabel}登录记录
          </span>
          <span className={styles.tableHint}>共 {total} 条</span>
        </div>
        <Table<LoginLogItem>
          aria-label="登录日志列表"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (value) => `共 ${value} 条`,
            onChange: handlePageChange,
          }}
          rowKey="id"
          scroll={{ x: 1240 }}
          size="middle"
        />
      </div>

      <LogDetailDrawer
        data={detailData}
        kind="login"
        loading={detailLoading}
        onClose={() => setDetailOpen(false)}
        open={detailOpen}
      />
    </div>
  );
};

export default LoginLogPage;

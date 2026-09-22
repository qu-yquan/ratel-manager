import React, { useCallback, useMemo, useState } from "react";
import {
  Button,
  DatePicker,
  Form,
  Input,
  Select,
  Table,
  Tag,
  Tooltip,
} from "antd";
import type { TableProps } from "antd";
import {
  CaretDownOutlined,
  CaretRightOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  LoadingOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import dayjs, { type Dayjs } from "dayjs";
import { useLocation } from "umi";

import LogDetailDrawer from "../log/components/LogDetailDrawer";
import { useLogPage } from "../log/hooks/useLogPage";
import {
  getOperationLogById,
  getOperationLogPage,
  getOperationLogTreeByTid,
} from "../log/services/log";
import type { OperationLogItem, OperationLogQuery } from "../log/types";

import styles from "../log/index.module.less";

interface OperationSearchValues {
  operName?: string;
  modulePrefix?: string;
  requestMethod?: string;
  requestUrl?: string;
  tid?: string;
  status?: number;
  requestTime?: [Dayjs, Dayjs];
}

const DEFAULT_QUERY: OperationLogQuery = {
  pageNum: 1,
  pageSize: 20,
};

const STATUS_OPTIONS = [
  { label: "成功", value: 1 },
  { label: "失败", value: 0 },
];

const METHOD_OPTIONS = ["GET", "POST", "PUT", "DELETE", "PATCH"].map(
  (value) => ({ label: value, value })
);

const { RangePicker } = DatePicker;

function formatTime(value?: string): string {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss.SSS") : "—";
}

const OperationLogPage: React.FC = () => {
  const location = useLocation();
  const authorizationId = useMemo(
    () =>
      new URLSearchParams(location.search).get("authorizationId")?.trim() ||
      undefined,
    [location.search]
  );
  const initialQuery = useMemo<OperationLogQuery>(
    () => ({ ...DEFAULT_QUERY, authorizationId }),
    [authorizationId]
  );
  const [form] = Form.useForm<OperationSearchValues>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailData, setDetailData] = useState<OperationLogItem | null>(null);
  const [treeLoadingKeys, setTreeLoadingKeys] = useState<React.Key[]>([]);
  const {
    loading,
    dataSource,
    setDataSource,
    total,
    currentPage,
    pageSize,
    loadPage,
    handlePageChange,
  } = useLogPage(getOperationLogPage, initialQuery);

  const buildQuery = useCallback(
    (pageNum = 1): OperationLogQuery => {
      const values = form.getFieldsValue();
      return {
        authorizationId,
        operName: values.operName,
        modulePrefix: values.modulePrefix,
        requestMethod: values.requestMethod,
        requestUrl: values.requestUrl,
        tid: values.tid,
        status: values.status,
        requestTimeStart: values.requestTime?.[0]?.format(
          "YYYY-MM-DD HH:mm:ss"
        ),
        requestTimeEnd: values.requestTime?.[1]?.format("YYYY-MM-DD HH:mm:ss"),
        pageNum,
        pageSize,
      };
    },
    [authorizationId, form, pageSize]
  );

  const handleSearch = useCallback(() => {
    void loadPage(buildQuery());
  }, [buildQuery, loadPage]);

  const handleReset = useCallback(() => {
    form.resetFields();
    void loadPage({ authorizationId, pageNum: 1, pageSize });
  }, [authorizationId, form, loadPage, pageSize]);

  const handleViewDetail = useCallback(async (record: OperationLogItem) => {
    setDetailData(record);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetailData(await getOperationLogById(record.operId));
    } catch {
      // 列表数据仍可用于降级展示。
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const handleExpand = useCallback(
    async (expanded: boolean, record: OperationLogItem) => {
      if (
        !expanded ||
        record.children ||
        !record.tid ||
        treeLoadingKeys.includes(record.operId)
      ) {
        return;
      }
      setTreeLoadingKeys((keys) => [...keys, record.operId]);
      try {
        const tree = await getOperationLogTreeByTid(record.tid);
        setDataSource((items) =>
          items.map((item) => (item.operId === record.operId ? tree : item))
        );
      } catch {
        // 请求层已统一提示。
      } finally {
        setTreeLoadingKeys((keys) =>
          keys.filter((key) => key !== record.operId)
        );
      }
    },
    [setDataSource, treeLoadingKeys]
  );

  const columns: TableProps<OperationLogItem>["columns"] = [
    Table.EXPAND_COLUMN,
    {
      title: "操作人",
      dataIndex: "operName",
      width: 150,
      render: (value: string, record) => (
        <div className={styles.primaryCell}>
          <span className={styles.primaryText}>{value || "匿名操作"}</span>
          <span className={styles.secondaryText}>
            {record.operSubject === 1 ? "智能助手" : "用户"}
          </span>
        </div>
      ),
    },
    {
      title: "调用服务",
      width: 180,
      render: (_: unknown, record) => (
        <div className={styles.primaryCell}>
          <code className={styles.codeText}>{record.fromApp || "—"}</code>
          <span className={styles.secondaryText}>
            目标：{record.modulePrefix || "—"}
          </span>
        </div>
      ),
    },
    {
      title: "功能 / 接口",
      width: 300,
      ellipsis: true,
      render: (_: unknown, record) => (
        <div className={styles.primaryCell}>
          <span className={styles.primaryText}>
            {record.apiDescription || record.apiModule || "未命名操作"}
          </span>
          <span className={styles.secondaryText}>
            {record.requestUrl || "—"}
          </span>
        </div>
      ),
    },
    {
      title: "请求方式",
      dataIndex: "requestMethod",
      width: 100,
      render: (value: string) => (
        <code className={styles.codeText}>{value || "—"}</code>
      ),
    },
    {
      title: "请求时间",
      dataIndex: "requestTime",
      width: 190,
      render: formatTime,
    },
    {
      title: "耗时",
      dataIndex: "consumeMill",
      width: 100,
      render: (value: number) => (
        <span className={value >= 1000 ? styles.durationSlow : undefined}>
          {value ?? 0} ms
        </span>
      ),
    },
    {
      title: "执行结果",
      dataIndex: "status",
      width: 100,
      render: (value: boolean) => (
        <Tag color={value ? "success" : "error"}>{value ? "成功" : "失败"}</Tag>
      ),
    },
    {
      title: "操作",
      fixed: "right",
      width: 100,
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
        </div>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.searchBar}>
        <Form className={styles.searchForm} component={false} form={form}>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-name">
              操作人
            </label>
            <Form.Item name="operName" noStyle>
              <Input
                allowClear
                id="operation-name"
                onPressEnter={handleSearch}
                placeholder="请输入操作人"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-module">
              所属服务
            </label>
            <Form.Item name="modulePrefix" noStyle>
              <Input
                allowClear
                id="operation-module"
                onPressEnter={handleSearch}
                placeholder="例如 security"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-method">
              请求方式
            </label>
            <Form.Item name="requestMethod" noStyle>
              <Select
                allowClear
                id="operation-method"
                options={METHOD_OPTIONS}
                placeholder="全部"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-status">
              执行结果
            </label>
            <Form.Item name="status" noStyle>
              <Select
                allowClear
                id="operation-status"
                options={STATUS_OPTIONS}
                placeholder="全部"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-tid">
              Trace ID
            </label>
            <Form.Item name="tid" noStyle>
              <Input
                allowClear
                id="operation-tid"
                onPressEnter={handleSearch}
                placeholder="请输入 Trace ID"
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <label className={styles.searchLabel} htmlFor="operation-url">
              请求路径
            </label>
            <Form.Item name="requestUrl" noStyle>
              <Input
                allowClear
                id="operation-url"
                onPressEnter={handleSearch}
                placeholder="请输入请求路径"
              />
            </Form.Item>
          </div>
          <div className={`${styles.searchItem} ${styles.searchItemWide}`}>
            <span className={styles.searchLabel}>请求时间</span>
            <Form.Item name="requestTime" noStyle>
              <RangePicker
                aria-label="请求时间范围"
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
          <span className={styles.tableTitle}>操作日志</span>
          <div className={styles.tableMeta}>
            <span className={styles.tableHint}>共 {total} 条链路</span>
            <Tooltip title="点击行首箭头，按 Trace ID 查询并展开后续调用节点">
              <Button
                aria-label="链路展开说明"
                className={styles.hintIcon}
                icon={<InfoCircleOutlined aria-hidden="true" />}
                size="small"
                type="text"
              />
            </Tooltip>
          </div>
        </div>
        <Table<OperationLogItem>
          aria-label="操作日志链路列表"
          columns={columns}
          dataSource={dataSource}
          expandable={{
            expandIcon: ({ expanded, onExpand, record }) => {
              const treeLoading = treeLoadingKeys.includes(record.operId);
              return (
                <Button
                  aria-label={expanded ? "收起日志链路" : "展开日志链路"}
                  icon={
                    treeLoading ? (
                      <LoadingOutlined aria-hidden="true" />
                    ) : expanded ? (
                      <CaretDownOutlined aria-hidden="true" />
                    ) : (
                      <CaretRightOutlined aria-hidden="true" />
                    )
                  }
                  loading={treeLoading}
                  onClick={(event) => onExpand(record, event)}
                  size="small"
                  type="text"
                />
              );
            },
            indentSize: 24,
            onExpand: (expanded, record) => void handleExpand(expanded, record),
            rowExpandable: (record) =>
              Boolean(record.children?.length || record.hasChildren),
          }}
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (value) => `共 ${value} 条链路`,
            onChange: handlePageChange,
          }}
          rowKey="operId"
          scroll={{ x: 1220 }}
          size="middle"
        />
      </div>

      <LogDetailDrawer
        data={detailData}
        kind="operation"
        loading={detailLoading}
        onClose={() => setDetailOpen(false)}
        open={detailOpen}
      />
    </div>
  );
};

export default OperationLogPage;

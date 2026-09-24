import React, { useState, useCallback, useEffect } from "react";
import { Button, Table, Input, Select, Tag, Dropdown, Form, Space } from "antd";
import type { MenuProps, TableProps } from "antd";
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EditOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  ThunderboltOutlined,
  FileSearchOutlined,
  DeleteOutlined,
  CodeOutlined,
} from "@ant-design/icons";
import { AuthGate, useAuth } from "@gwsu/core";
import styles from "./index.module.less";
import JobFormModal from "./components/JobFormModal";
import JobLogDrawer from "./components/JobLogDrawer";
import GlueEditorModal from "./components/GlueEditorModal";
import TriggerJobModal from "./components/TriggerJobModal";
import ClearLogModal from "./components/ClearLogModal";
import NextTriggerTimeModal from "./components/NextTriggerTimeModal";
import { useJob } from "./hooks/useJob";
import type { JobInfo, JobQuery, JobInfoCreateDTO } from "./types";
import { TRIGGER_STATUS_OPTIONS } from "./types";
import {
  PERM_ADD,
  PERM_EDIT,
  PERM_REMOVE,
  PERM_START,
  PERM_TRIGGER,
  PERM_LOG,
  PERM_CLEAR_LOG,
} from "./permissionConstants";
import { clearLog } from "./services/job";
import { buildGlueUpdatePayload, deriveJobMode } from "./utils";

const JobPage: React.FC = () => {
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    ensureInitialized,
    handlePageChange,
    handleAdd,
    handleUpdate,
    handleRemove,
    handleStart,
    handleStop,
    handleTrigger,
  } = useJob();

  const canEdit = useAuth(PERM_EDIT);
  const canRemove = useAuth(PERM_REMOVE);
  const canStart = useAuth(PERM_START);
  const canTrigger = useAuth(PERM_TRIGGER);
  const canLog = useAuth(PERM_LOG);

  const [searchForm] = Form.useForm<JobQuery>();

  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<"create" | "edit">(
    "create"
  );
  const [formModalData, setFormModalData] = useState<JobInfo | null>(null);

  const [logDrawerVisible, setLogDrawerVisible] = useState(false);
  const [logJobId, setLogJobId] = useState("");
  const [logJobName, setLogJobName] = useState("");

  const [glueModalVisible, setGlueModalVisible] = useState(false);
  const [glueRecord, setGlueRecord] = useState<JobInfo | null>(null);
  const [glueJobId, setGlueJobId] = useState("");
  const [glueType, setGlueType] = useState("");
  const [glueSource, setGlueSource] = useState("");
  const [triggerModalVisible, setTriggerModalVisible] = useState(false);
  const [triggerJob, setTriggerJob] = useState<JobInfo | null>(null);
  const [clearLogVisible, setClearLogVisible] = useState(false);
  const [nextTriggerModalVisible, setNextTriggerModalVisible] = useState(false);
  const [nextTriggerJob, setNextTriggerJob] = useState<JobInfo | null>(null);

  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  const handleSearch = useCallback(() => {
    handlePageChange(1, pageSize);
  }, [handlePageChange, pageSize]);

  const handleReset = useCallback(() => {
    searchForm.resetFields();
  }, [searchForm]);

  const handleCreate = useCallback(() => {
    setFormModalMode("create");
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((record: JobInfo) => {
    setFormModalMode("edit");
    setFormModalData(record);
    setFormModalVisible(true);
  }, []);

  const handleGlueEdit = useCallback((record: JobInfo) => {
    setGlueRecord(record);
    setGlueJobId(record.id ?? "");
    setGlueType(record.glueType);
    setGlueSource(record.glueSource ?? "");
    setGlueModalVisible(true);
  }, []);

  const handleViewLog = useCallback((record: JobInfo) => {
    setLogJobId(record.id ?? "");
    setLogJobName(record.name);
    setLogDrawerVisible(true);
  }, []);

  const handleSave = useCallback(
    async (data: JobInfoCreateDTO) => {
      return formModalMode === "create" ? handleAdd(data) : handleUpdate(data);
    },
    [formModalMode, handleAdd, handleUpdate]
  );

  const handleOpenTrigger = useCallback((record: JobInfo) => {
    setTriggerJob(record);
    setTriggerModalVisible(true);
  }, []);

  const handleClearLog = useCallback(async (type: number) => {
    return clearLog("", type);
  }, []);

  const handleOpenNextTriggerTime = useCallback((record: JobInfo) => {
    setNextTriggerJob(record);
    setNextTriggerModalVisible(true);
  }, []);

  const getMoreItems = (record: JobInfo): MenuProps["items"] => {
    const items: MenuProps["items"] = [];
    if (record.triggerStatus === 0 && canStart) {
      items.push({
        key: "start",
        icon: <PlayCircleOutlined />,
        label: "启动",
        onClick: () => handleStart(record.id!),
      });
    }
    if (record.triggerStatus === 1 && canStart) {
      items.push({
        key: "stop",
        icon: <PauseCircleOutlined />,
        label: "停止",
        onClick: () => handleStop(record.id!),
      });
    }
    if (canTrigger) {
      items.push({
        key: "trigger",
        icon: <ThunderboltOutlined />,
        label: "立即执行",
        onClick: () => handleOpenTrigger(record),
      });
    }
    if (canLog) {
      items.push({
        key: "log",
        icon: <FileSearchOutlined />,
        label: "调度日志",
        onClick: () => handleViewLog(record),
      });
    }
    if (record.scheduleType !== "NONE" && record.scheduleConf) {
      items.push({
        key: "next-trigger-time",
        icon: <FileSearchOutlined />,
        label: "预估下次触发时间",
        onClick: () => handleOpenNextTriggerTime(record),
      });
    }
    if (record.glueType !== "BEAN" && canEdit) {
      items.push({
        key: "glue",
        icon: <CodeOutlined />,
        label: "GLUE编辑",
        onClick: () => handleGlueEdit(record),
      });
    }
    if (canRemove) {
      items.push({
        key: "remove",
        icon: <DeleteOutlined />,
        label: "删除",
        danger: true,
        onClick: () => {
          handleRemove(record.id!);
        },
      });
    }
    return items;
  };

  const columns: TableProps<JobInfo>["columns"] = [
    {
      title: "序号",
      width: 60,
      align: "center",
      render: (_: unknown, __: JobInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    { title: "任务名称", dataIndex: "name", width: 160, ellipsis: true },
    { title: "负责人", dataIndex: "author", width: 100 },
    {
      title: "任务类型",
      width: 100,
      render: (_: unknown, record: JobInfo) => {
        const mode = deriveJobMode(record);
        const colorMap: Record<string, string> = {
          URL: "blue",
          BEAN: "green",
          GLUE: "orange",
        };
        const labelMap: Record<string, string> = {
          URL: "平台URL",
          BEAN: "BEAN",
          GLUE: "GLUE",
        };
        return <Tag color={colorMap[mode]}>{labelMap[mode]}</Tag>;
      },
    },
    {
      title: "Handler",
      dataIndex: "executorHandler",
      width: 140,
      ellipsis: true,
    },
    {
      title: "调度类型",
      width: 100,
      render: (_: unknown, record: JobInfo) => {
        const map: Record<string, string> = {
          NONE: "无",
          CRON: "Cron",
          FIX_RATE: "固定速率",
        };
        return map[record.scheduleType] ?? record.scheduleType;
      },
    },
    {
      title: "调度配置",
      dataIndex: "scheduleConf",
      width: 150,
      ellipsis: true,
    },
    {
      title: "状态",
      dataIndex: "triggerStatus",
      width: 100,
      fixed: "right",
      render: (val: number) => (
        <Tag color={val === 1 ? "green" : "red"}>
          {val === 1 ? "运行中" : "已停止"}
        </Tag>
      ),
    },
    {
      title: "操作",
      width: 260,
      fixed: "right",
      render: (_: unknown, record: JobInfo) => (
        <div className={styles.actionColumn}>
          {canEdit && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
          )}
          <Dropdown menu={{ items: getMoreItems(record) }}>
            <Button type="link" size="small" icon={<MoreOutlined />}>
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.jobPage}>
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>任务名称</span>
            <Form.Item name="name" noStyle>
              <Input
                placeholder="请输入"
                allowClear
                style={{ width: 160 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>Handler</span>
            <Form.Item name="executorHandler" noStyle>
              <Input
                placeholder="请输入"
                allowClear
                style={{ width: 160 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="triggerStatus" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 120 }}
                options={[...TRIGGER_STATUS_OPTIONS]}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </div>
      </div>

      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>任务列表</span>
          <Space>
            <AuthGate buttonKey={PERM_ADD}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增任务
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_CLEAR_LOG}>
              <Button
                icon={<DeleteOutlined />}
                onClick={() => setClearLogVisible(true)}
                data-ai-approval
              >
                清理日志
              </Button>
            </AuthGate>
          </Space>
        </div>
        <Table<JobInfo>
          rowKey="id"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 1100 }}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
        />
      </div>

      <JobFormModal
        visible={formModalVisible}
        mode={formModalMode}
        data={formModalData}
        onSave={handleSave}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />

      <JobLogDrawer
        visible={logDrawerVisible}
        jobId={logJobId}
        jobName={logJobName}
        onClose={() => setLogDrawerVisible(false)}
      />

      <GlueEditorModal
        visible={glueModalVisible}
        jobId={glueJobId}
        glueType={glueType}
        glueSource={glueSource}
        onSave={async (_type, source, remark) => {
          if (!glueRecord) {
            return;
          }
          const success = await handleUpdate(
            buildGlueUpdatePayload(glueRecord, _type, source, remark)
          );
          if (success) {
            setGlueModalVisible(false);
            setGlueRecord(null);
          }
        }}
        onClose={() => {
          setGlueModalVisible(false);
          setGlueRecord(null);
        }}
      />

      <TriggerJobModal
        visible={triggerModalVisible}
        job={triggerJob}
        onSubmit={handleTrigger}
        onClose={() => {
          setTriggerModalVisible(false);
          setTriggerJob(null);
        }}
      />

      <ClearLogModal
        visible={clearLogVisible}
        onSubmit={handleClearLog}
        onClose={() => setClearLogVisible(false)}
      />

      <NextTriggerTimeModal
        visible={nextTriggerModalVisible}
        job={nextTriggerJob}
        onClose={() => setNextTriggerModalVisible(false)}
      />
    </div>
  );
};

export default JobPage;

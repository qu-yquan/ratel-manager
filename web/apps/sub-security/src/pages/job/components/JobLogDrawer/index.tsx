import React, { useState, useCallback, useEffect, useRef } from 'react';
import { Drawer, Table, Tag, Button, Space, Popconfirm, Select, DatePicker } from 'antd';
import type { TableProps } from 'antd';
import type { JobLog, JobLogQuery } from '../../types';
import { LOG_STATUS_OPTIONS } from '../../types';
import { getLogPage, killJob } from '../../services/job';
import LogContentModal from '../LogContentModal';
import { App } from 'antd';

const { RangePicker } = DatePicker;

interface JobLogDrawerProps {
  visible: boolean;
  jobId: string;
  jobName: string;
  onClose: () => void;
}

const JobLogDrawer: React.FC<JobLogDrawerProps> = ({ visible, jobId, jobName, onClose }) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<JobLog[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [logContentVisible, setLogContentVisible] = useState(false);
  const [currentLogId, setCurrentLogId] = useState<string>('');

  const queryRef = useRef<JobLogQuery>({});

  const fetchLogPage = useCallback(async (query?: JobLogQuery) => {
    if (query) queryRef.current = query;
    setLoading(true);
    try {
      const params: JobLogQuery = {
        ...queryRef.current,
        jobId,
        pageNum: query?.pageNum ?? currentPage,
        pageSize: query?.pageSize ?? pageSize,
      };
      const page = await getLogPage(params);
      setDataSource(page?.records ?? []);
      setTotal(page?.total ?? 0);
      setCurrentPage(page?.current ?? 1);
      setPageSize(page?.size ?? 10);
    } catch {
      // request层已提示
    } finally {
      setLoading(false);
    }
  }, [jobId, currentPage, pageSize]);

  useEffect(() => {
    if (visible && jobId) {
      queryRef.current = {};
      setCurrentPage(1);
      fetchLogPage({ jobId, pageNum: 1 });
    }
  }, [visible, jobId]);

  const handleKill = useCallback(async (logId: string) => {
    const success = await killJob(logId);
    if (success) {
      message.success('终止成功');
      fetchLogPage();
    }
  }, [fetchLogPage, message]);

  const renderTriggerCode = (code: number) => {
    if (code === 200) return <Tag color="green">成功</Tag>;
    if (code === 0) return <Tag>未触发</Tag>;
    return <Tag color="red">失败</Tag>;
  };

  const renderHandleCode = (code: number) => {
    if (code === 200) return <Tag color="green">成功</Tag>;
    if (code === 0) return <Tag color="blue">运行中</Tag>;
    return <Tag color="red">失败</Tag>;
  };

  const columns: TableProps<JobLog>['columns'] = [
    { title: '调度时间', dataIndex: 'triggerTime', width: 170 },
    { title: '调度结果', dataIndex: 'triggerCode', width: 90, render: renderTriggerCode },
    { title: '执行结果', dataIndex: 'handleCode', width: 90, render: renderHandleCode },
    { title: '执行器地址', dataIndex: 'executorAddress', width: 160, ellipsis: true },
    {
      title: '操作', width: 200,
      render: (_: unknown, record: JobLog) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => { setCurrentLogId(record.id); setLogContentVisible(true); }}>
            执行日志
          </Button>
          {record.handleCode === 0 && (
            <Popconfirm title="确定终止该任务？" onConfirm={() => handleKill(record.id)}>
              <Button type="link" size="small" danger>终止</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Drawer
      title={`调度日志 - ${jobName}`}
      open={visible}
      onClose={onClose}
      size={800}
      destroyOnHidden
    >
      <div
        style={{
          marginBottom: 16,
          display: "flex",
          gap: 12,
          flexWrap: "wrap",
          alignItems: "center",
        }}
      >
        <Select
          placeholder="日志状态"
          allowClear
          style={{ width: 120 }}
          options={[...LOG_STATUS_OPTIONS]}
          onChange={(v) => {
            fetchLogPage({ ...queryRef.current, logStatus: v, pageNum: 1 });
          }}
        />
        <RangePicker
          showTime
          onChange={(_, ds) => {
            const range = ds as unknown as [string, string] | undefined;
            fetchLogPage({
              ...queryRef.current,
              triggerTimeStart: range?.[0],
              triggerTimeEnd: range?.[1],
              pageNum: 1,
            });
          }}
        />
      </div>
      <Table<JobLog>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        size="middle"
        scroll={{ x: 700 }}
        pagination={{
          current: currentPage,
          pageSize,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (page, size) =>
            fetchLogPage({
              ...queryRef.current,
              pageNum: page,
              pageSize: size,
            }),
        }}
      />
      <LogContentModal
        visible={logContentVisible}
        logId={currentLogId}
        onClose={() => setLogContentVisible(false)}
      />
    </Drawer>
  );
};

export default JobLogDrawer;

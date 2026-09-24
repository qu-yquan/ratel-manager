import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getJobPage,
  addJob,
  updateJob,
  removeJob,
  startJob,
  stopJob,
  triggerJob,
} from '../services/job';
import type { JobInfo, JobQuery, JobInfoCreateDTO } from '../types';

/**
 * 任务管理相关 hooks
 * 封装任务列表查询、新增、编辑、删除、启停、触发等常用逻辑
 */
export function useJob() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<JobInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  /** 使用 ref 持有最新查询条件，避免闭包过期 */
  const queryRef = useRef<JobQuery>({});
  /** 标记是否已初始化，防止 useEffect 重复触发 */
  const initializedRef = useRef(false);

  /** 分页查询任务列表 */
  const fetchJobPage = useCallback(
    async (query?: JobQuery) => {
      if (query) {
        queryRef.current = query;
      }
      setLoading(true);
      try {
        const params: JobQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getJobPage(params);
        setDataSource(page?.records ?? []);
        setTotal(page?.total ?? 0);
        setCurrentPage(page?.current ?? 1);
        setPageSize(page?.size ?? 10);
      } catch {
        // request 层已自动提示
      } finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize],
  );

  /** 初始化加载（仅执行一次） */
  const ensureInitialized = useCallback(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      fetchJobPage();
    }
  }, [fetchJobPage]);

  /** 翻页 */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchJobPage({ ...queryRef.current, pageNum: page, pageSize: size });
    },
    [fetchJobPage],
  );

  /** 新增任务 */
  const handleAdd = useCallback(
    async (data: JobInfoCreateDTO) => {
      try {
        await addJob(data);
        message.success('新增成功');
        await fetchJobPage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchJobPage],
  );

  /** 更新任务 */
  const handleUpdate = useCallback(
    async (data: JobInfoCreateDTO) => {
      try {
        await updateJob(data);
        message.success('编辑成功');
        await fetchJobPage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchJobPage],
  );

  /** 删除任务 */
  const handleRemove = useCallback(
    async (id: string) => {
      try {
        await removeJob(id);
        message.success('删除成功');
        await fetchJobPage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchJobPage],
  );

  /** 启动任务 */
  const handleStart = useCallback(
    async (id: string) => {
      try {
        await startJob(id);
        message.success('已启动');
        await fetchJobPage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchJobPage],
  );

  /** 停止任务 */
  const handleStop = useCallback(
    async (id: string) => {
      try {
        await stopJob(id);
        message.success('已停止');
        await fetchJobPage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchJobPage],
  );

  /** 手动触发一次 */
  const handleTrigger = useCallback(
    async (id: string, executorParam?: string) => {
      try {
        await triggerJob(id, executorParam);
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [],
  );

  return {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchJobPage,
    ensureInitialized,
    handlePageChange,
    handleAdd,
    handleUpdate,
    handleRemove,
    handleStart,
    handleStop,
    handleTrigger,
  };
}

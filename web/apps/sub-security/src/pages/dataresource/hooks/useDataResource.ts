import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getDataResourcePage,
  saveOrUpdateDataResource,
  deleteDataResources,
  syncToRedis,
} from '../services/dataResource';
import type { DataResourceInfo, DataResourceQuery } from '../types';

/**
 * 数据权限管理相关 hooks
 * 封装列表查询、保存、删除、同步等常用逻辑
 */
export function useDataResource() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<DataResourceInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const queryRef = useRef<DataResourceQuery>({});
  const initializedRef = useRef(false);

  /** 分页查询 */
  const fetchDataResourcePage = useCallback(
    async (query?: DataResourceQuery) => {
      if (query) {
        queryRef.current = query;
      }
      setLoading(true);
      try {
        const params: DataResourceQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getDataResourcePage(params);
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

  /** 初始化加载 */
  const ensureInitialized = useCallback(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      fetchDataResourcePage();
    }
  }, [fetchDataResourcePage]);

  /** 翻页 */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchDataResourcePage({
        ...queryRef.current,
        pageNum: page,
        pageSize: size,
      });
    },
    [fetchDataResourcePage],
  );

  /** 保存或更新 */
  const handleSaveOrUpdate = useCallback(
    async (data: DataResourceInfo) => {
      try {
        await saveOrUpdateDataResource(data);
        message.success(data.id ? '编辑成功' : '新增成功');
        await fetchDataResourcePage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchDataResourcePage, message],
  );

  /** 批量删除 */
  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteDataResources(ids);
        message.success('删除成功');
        await fetchDataResourcePage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchDataResourcePage, message],
  );

  /** 同步到 Redis */
  const handleSync = useCallback(async () => {
    try {
      await syncToRedis();
      message.success('同步成功');
      return true;
    } catch {
      return false;
    }
  }, [message]);

  return {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchDataResourcePage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleSync,
  };
}

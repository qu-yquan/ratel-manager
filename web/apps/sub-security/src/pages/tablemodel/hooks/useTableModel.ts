import { useState, useCallback } from 'react';
import { message } from 'antd';
import type { TableModelQuery, TableModelPageResult, TableModelInfo, ModuleInfo } from '../types';
import { getTableModelPage, getModuleList, syncTableModel, batchDeleteTableModels } from '../services/tableModel';

/**
 * 表模型管理 Hook
 */
export function useTableModel() {
  const [loading, setLoading] = useState(false);
  const [pageData, setPageData] = useState<TableModelPageResult>({
    records: [],
    total: 0,
    size: 10,
    current: 1,
    pages: 0,
  });
  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [query, setQuery] = useState<TableModelQuery>({
    pageNum: 1,
    pageSize: 10,
  });

  /** 加载模块列表 */
  const loadModules = useCallback(async () => {
    try {
      const list = await getModuleList();
      setModules(list);
    } catch {
      // request 层已自动提示
    }
  }, []);

  /** 加载分页数据 */
  const loadPageData = useCallback(async (searchQuery?: TableModelQuery) => {
    const finalQuery = searchQuery || query;
    setLoading(true);
    try {
      const data = await getTableModelPage(finalQuery);
      setPageData(data);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [query]);

  /** 搜索 */
  const handleSearch = useCallback((values: Partial<TableModelQuery>) => {
    const newQuery = { ...query, ...values, pageNum: 1 };
    setQuery(newQuery);
    loadPageData(newQuery);
  }, [query, loadPageData]);

  /** 翻页 */
  const handlePageChange = useCallback((pageNum: number, pageSize: number) => {
    const newQuery = { ...query, pageNum, pageSize };
    setQuery(newQuery);
    loadPageData(newQuery);
  }, [query, loadPageData]);

  /** 同步表模型 */
  const handleSync = useCallback(async (record: TableModelInfo) => {
    try {
      const mod = modules.find((m) => m.prefix === record.modulePrefix);
      await syncTableModel(record.id, mod?.applicationName ?? '');
      message.success('同步成功');
      loadPageData();
    } catch {
      // request 层已自动提示
    }
  }, [loadPageData, modules]);

  /** 批量删除表模型 */
  const handleBatchDelete = useCallback(async (ids: string[]): Promise<boolean> => {
    try {
      await batchDeleteTableModels(ids);
      message.success('删除成功');
      loadPageData();
      return true;
    } catch {
      return false;
    }
  }, [loadPageData]);

  return {
    loading,
    pageData,
    modules,
    query,
    loadModules,
    loadPageData,
    handleSearch,
    handlePageChange,
    handleSync,
    handleBatchDelete,
  };
}

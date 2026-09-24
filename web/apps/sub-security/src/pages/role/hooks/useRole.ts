import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getRolePage,
  saveOrUpdateRole,
  deleteRoles,
  updateRoleStatus,
} from '../services/role';
import type { RoleInfo, RoleQuery } from '../types';

/**
 * 角色管理相关 hooks
 * 封装角色列表查询、删除、状态切换等常用逻辑
 */
export function useRole() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<RoleInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  /** 使用 ref 持有最新查询条件，避免闭包过期 */
  const queryRef = useRef<RoleQuery>({});
  /** 标记是否已初始化，防止 useEffect 重复触发 */
  const initializedRef = useRef(false);

  /** 分页查询角色列表 */
  const fetchRolePage = useCallback(
    async (query?: RoleQuery) => {
      if (query) {
        queryRef.current = query;
      }
      setLoading(true);
      try {
        const params: RoleQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getRolePage(params);
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
      fetchRolePage();
    }
  }, [fetchRolePage]);

  /** 翻页 */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchRolePage({ ...queryRef.current, pageNum: page, pageSize: size });
    },
    [fetchRolePage],
  );

  /** 保存或更新角色 */
  const handleSaveOrUpdate = useCallback(
    async (data: RoleInfo) => {
      try {
        await saveOrUpdateRole(data);
        message.success(data.id ? '编辑成功' : '新增成功');
        await fetchRolePage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchRolePage],
  );

  /** 批量删除角色 */
  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteRoles(ids);
        message.success('删除成功');
        await fetchRolePage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchRolePage],
  );

  /** 切换角色状态 */
  const handleToggleStatus = useCallback(
    async (id: string, status: boolean) => {
      try {
        await updateRoleStatus(id, status ? 1 : 0);
        message.success(status ? '已启用' : '已禁用');
        await fetchRolePage();
        return true;
      } catch {
        // request 层已自动提示
        return false;
      }
    },
    [fetchRolePage],
  );

  return {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchRolePage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleToggleStatus,
  };
}

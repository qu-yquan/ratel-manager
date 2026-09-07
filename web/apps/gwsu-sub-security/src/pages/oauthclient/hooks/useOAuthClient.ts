import {useCallback, useRef, useState} from 'react';
import {App} from 'antd';
import type {OAuthClientEnums, OAuthClientInfo, OAuthClientQuery} from '../types';
import {
  deleteOAuthClients,
  getOAuthClientEnums,
  getOAuthClientPage,
  resetOAuthClientSecret,
  saveOrUpdateOAuthClient,
} from '../services/oauthClient';

export function useOAuthClient() {
  const {message, modal} = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<OAuthClientInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [enumOptions, setEnumOptions] = useState<OAuthClientEnums>({
    clientTypes: [],
    accountTypes: [],
    statuses: [],
    grantTypes: [],
    authenticationMethods: [],
  });
  const queryRef = useRef<OAuthClientQuery>({});
  const initializedRef = useRef(false);

  const fetchEnums = useCallback(async () => {
    try {
      const options = await getOAuthClientEnums();
      setEnumOptions({
        clientTypes: options?.clientTypes || [],
        accountTypes: options?.accountTypes || [],
        statuses: options?.statuses || [],
        grantTypes: options?.grantTypes || [],
        authenticationMethods: options?.authenticationMethods || [],
      });
    } catch {
      // request 层已自动提示
    }
  }, []);

  const fetchPage = useCallback(async (query?: OAuthClientQuery) => {
    if (query) {
      queryRef.current = query;
    }
    setLoading(true);
    try {
      const params: OAuthClientQuery = {
        ...queryRef.current,
        pageNum: query?.pageNum ?? currentPage,
        pageSize: query?.pageSize ?? pageSize,
      };
      const page = await getOAuthClientPage(params);
      setDataSource(page?.records ?? []);
      setTotal(page?.total ?? 0);
      setCurrentPage(page?.current ?? 1);
      setPageSize(page?.size ?? 10);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  const ensureInitialized = useCallback(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      fetchEnums();
      fetchPage();
    }
  }, [fetchEnums, fetchPage]);

  const handlePageChange = useCallback((page: number, size: number) => {
    fetchPage({...queryRef.current, pageNum: page, pageSize: size});
  }, [fetchPage]);

  const handleSaveOrUpdate = useCallback(async (data: OAuthClientInfo) => {
    try {
      const secret = await saveOrUpdateOAuthClient(data);
      message.success(data.id ? '编辑成功' : '新增成功');
      if (secret?.clientSecret) {
        modal.info({
          title: '客户端凭据',
          content: `客户端ID：${secret.clientId}\n客户端密钥：${secret.clientSecret}`,
        });
      }
      await fetchPage();
      return true;
    } catch {
      return false;
    }
  }, [fetchPage, message, modal]);

  const handleDelete = useCallback(async (ids: string[]) => {
    try {
      await deleteOAuthClients(ids);
      message.success('删除成功');
      await fetchPage();
      return true;
    } catch {
      return false;
    }
  }, [fetchPage, message]);

  const handleResetSecret = useCallback(async (id: string) => {
    try {
      const secret = await resetOAuthClientSecret(id);
      modal.info({
        title: '新客户端密钥',
        content: `客户端ID：${secret.clientId}\n新客户端密钥：${secret.clientSecret}`,
      });
      await fetchPage();
      return true;
    } catch {
      return false;
    }
  }, [fetchPage, modal]);

  return {
    loading,
    enumOptions,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchPage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleResetSecret,
  };
}

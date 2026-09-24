import React, {useCallback, useRef, useState} from 'react';
import {App, Typography} from 'antd';
import {fetchConfigsBatch} from '@gwsu/core';
import type {OAuthClientEnums, OAuthClientInfo, OAuthClientQuery} from '../types';
import {
  deleteOAuthClients,
  getOAuthClientById,
  getOAuthClientEnums,
  getOAuthClientPage,
  resetOAuthClientSecret,
  saveOrUpdateOAuthClient,
} from '../services/oauthClient';

const renderSecretContent = (clientId: string, clientSecret: string) => (
  React.createElement(
    'div',
    null,
    React.createElement('div', null, `客户端ID：${clientId}`),
    React.createElement('div', null, `客户端密钥：${clientSecret}`),
    React.createElement(Typography.Text, {type: 'danger'}, '密钥只能查看一次，请妥善保管。'),
  )
);

export function useOAuthClient() {
  const {message, modal} = App.useApp();
  const [loading, setLoading] = useState(false);
  const [guideLoading, setGuideLoading] = useState(false);
  const [apiBaseUrl, setApiBaseUrl] = useState(() => browserOrigin());
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

  const fetchApiBaseUrl = useCallback(async () => {
    try {
      const configMap = await fetchConfigsBatch(['basic_url_config']);
      const configValue = configMap.basic_url_config?.configValue;
      if (!configValue) {
        return;
      }
      const config = JSON.parse(configValue) as {apiBaseUrl?: string};
      if (config.apiBaseUrl?.trim()) {
        setApiBaseUrl(config.apiBaseUrl.trim());
      }
    } catch {
      // 配置不可用时使用当前站点地址生成指南。
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
      void fetchEnums();
      void fetchApiBaseUrl();
      void fetchPage();
    }
  }, [fetchApiBaseUrl, fetchEnums, fetchPage]);

  const handleLoadGuide = useCallback(async (id: string) => {
    setGuideLoading(true);
    try {
      return await getOAuthClientById(id);
    } catch {
      return null;
    } finally {
      setGuideLoading(false);
    }
  }, []);

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
          content: renderSecretContent(secret.clientId, secret.clientSecret),
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
        content: renderSecretContent(secret.clientId, secret.clientSecret),
      });
      await fetchPage();
      return true;
    } catch {
      return false;
    }
  }, [fetchPage, modal]);

  return {
    loading,
    guideLoading,
    apiBaseUrl,
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
    handleLoadGuide,
  };
}

function browserOrigin(): string {
  return typeof window === 'undefined' ? '' : window.location.origin;
}

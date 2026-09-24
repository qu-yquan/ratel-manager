import { useCallback, useRef, useState } from 'react';
import { deleteApiKey, getApiKeyPage } from '../services/apiKey';
import type { ApiKeyInfo, ApiKeyQuery } from '../types';

const DEFAULT_QUERY: ApiKeyQuery = {
  apiKeyName: '',
  pageNum: 1,
  pageSize: 10,
};

export default function useApiKey() {
  const initializedRef = useRef(false);
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<ApiKeyInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(DEFAULT_QUERY.pageNum);
  const [pageSize, setPageSize] = useState(DEFAULT_QUERY.pageSize);
  const [query, setQuery] = useState<ApiKeyQuery>(DEFAULT_QUERY);

  const fetchApiKeyPage = useCallback(async (nextQuery?: Partial<ApiKeyQuery>) => {
    const mergedQuery = {
      ...query,
      ...nextQuery,
    };
    setLoading(true);
    try {
      const data = await getApiKeyPage(mergedQuery);
      setDataSource(data.records ?? []);
      setTotal(data.total ?? 0);
      setCurrentPage(data.current ?? mergedQuery.pageNum);
      setPageSize(data.size ?? mergedQuery.pageSize);
      setQuery(mergedQuery);
    } finally {
      setLoading(false);
    }
  }, [query]);

  const ensureInitialized = useCallback(async () => {
    if (initializedRef.current) {
      return;
    }
    initializedRef.current = true;
    await fetchApiKeyPage(DEFAULT_QUERY);
  }, [fetchApiKeyPage]);

  const handlePageChange = useCallback(async (page: number, size: number) => {
    await fetchApiKeyPage({ pageNum: page, pageSize: size });
  }, [fetchApiKeyPage]);

  const handleCreate = useCallback(async () => {
    await fetchApiKeyPage({ pageNum: 1 });
  }, [fetchApiKeyPage]);

  const handleDelete = useCallback(async (id: string) => {
    await deleteApiKey(id);
    const nextPage = dataSource.length === 1 && currentPage > 1 ? currentPage - 1 : currentPage;
    await fetchApiKeyPage({ pageNum: nextPage });
  }, [currentPage, dataSource.length, fetchApiKeyPage]);

  return {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    query,
    fetchApiKeyPage,
    ensureInitialized,
    handlePageChange,
    handleCreate,
    handleDelete,
  };
}

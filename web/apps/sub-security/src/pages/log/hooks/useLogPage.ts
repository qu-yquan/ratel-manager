import { useCallback, useEffect, useRef, useState } from "react";

import type { PageQuery, PageResult } from "../types";

type PageLoader<T, Q extends PageQuery> = (query: Q) => Promise<PageResult<T>>;

export function useLogPage<T, Q extends PageQuery>(
  loader: PageLoader<T, Q>,
  defaultQuery: Q
) {
  const defaultQueryRef = useRef(defaultQuery);
  const queryRef = useRef<Q>(defaultQuery);
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<T[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(defaultQuery.pageNum ?? 1);
  const [pageSize, setPageSize] = useState(defaultQuery.pageSize ?? 20);

  const loadPage = useCallback(
    async (query: Q) => {
      queryRef.current = query;
      setLoading(true);
      try {
        const page = await loader(query);
        setDataSource(page?.records ?? []);
        setTotal(page?.total ?? 0);
        setCurrentPage(page?.current ?? query.pageNum ?? 1);
        setPageSize(page?.size ?? query.pageSize ?? 20);
      } catch {
        // 请求层已统一提示。
      } finally {
        setLoading(false);
      }
    },
    [loader]
  );

  useEffect(() => {
    void loadPage(defaultQueryRef.current);
  }, [loadPage]);

  const handlePageChange = useCallback(
    (page: number, size: number) => {
      void loadPage({
        ...queryRef.current,
        pageNum: page,
        pageSize: size,
      });
    },
    [loadPage]
  );

  return {
    loading,
    dataSource,
    setDataSource,
    total,
    currentPage,
    pageSize,
    loadPage,
    handlePageChange,
  };
}

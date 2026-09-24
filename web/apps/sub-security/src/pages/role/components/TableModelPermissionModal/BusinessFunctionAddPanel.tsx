import React from 'react';
import { Empty, Input, Pagination, Table, Tag } from 'antd';
import type { TableProps } from 'antd';
import type { BusinessFunctionInfo, TableModelInfo } from '../../../tablemodel/types';
import styles from './addTableModelModal.module.less';

interface BusinessFunctionAddPanelProps {
  searchText: string;
  businessFunctions: BusinessFunctionInfo[];
  selectedBusinessFunctionId: string | null;
  selectedBusinessFunctionName: string;
  previewTables: TableModelInfo[];
  totalTableCount: number;
  skippedCount: number;
  currentPage: number;
  pageSize: number;
  total: number;
  onSearchChange: (value: string) => void;
  onSearch: () => void;
  onPageChange: (page: number, pageSize: number) => void;
  onSelectBusinessFunction: (id: string) => void;
}

const previewColumns: TableProps<TableModelInfo>['columns'] = [
  { title: '表名', dataIndex: 'tableName', width: 160 },
  { title: '表注释', dataIndex: 'tableComment', ellipsis: true },
  { title: '模块', dataIndex: 'modulePrefix', width: 120 },
  { title: '数据源', dataIndex: 'dataSource', width: 120 },
];

const BusinessFunctionAddPanel: React.FC<BusinessFunctionAddPanelProps> = ({
  searchText,
  businessFunctions,
  selectedBusinessFunctionId,
  selectedBusinessFunctionName,
  previewTables,
  totalTableCount,
  skippedCount,
  currentPage,
  pageSize,
  total,
  onSearchChange,
  onSearch,
  onPageChange,
  onSelectBusinessFunction,
}) => {
  const renderPreviewContent = () => {
    if (!selectedBusinessFunctionId) {
      return (
        <div className={styles.emptyWrapper}>
          <Empty description="请选择一个业务功能，右侧将预览该功能关联的表模型" />
        </div>
      );
    }

    if (previewTables.length === 0) {
      return (
        <div className={styles.emptyWrapper}>
          <Empty description="该业务功能关联的表模型已全部存在，无需重复添加" />
        </div>
      );
    }

    return (
      <Table<TableModelInfo>
        rowKey="id"
        columns={previewColumns}
        dataSource={previewTables}
        size="small"
        pagination={false}
        scroll={{ y: 292 }}
      />
    );
  };

  return (
    <div className={styles.businessPanel}>
      <Input.Search
        placeholder="请输入业务功能名称或简介搜索"
        allowClear
        value={searchText}
        onChange={(e) => onSearchChange(e.target.value)}
        onPressEnter={onSearch}
        onSearch={onSearch}
        aria-label="搜索业务功能"
      />
      <div className={styles.businessContent}>
        <div className={styles.businessList}>
          {businessFunctions.length === 0 ? (
            <div className={styles.emptyWrapper}>
              <Empty description="未找到匹配的业务功能" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          ) : (
            businessFunctions.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`${styles.businessItem} ${
                  selectedBusinessFunctionId === item.id ? styles.businessItemActive : ''
                }`}
                onClick={() => onSelectBusinessFunction(item.id)}
                aria-pressed={selectedBusinessFunctionId === item.id}
              >
                <div className={styles.businessItemHeader}>
                  <span className={styles.businessName}>{item.name}</span>
                  <Tag color="blue">{item.tableCount ?? 0} 张表</Tag>
                </div>
                <div className={styles.businessSummary}>{item.summary}</div>
              </button>
            ))
          )}
        </div>
        <div className={styles.previewPanel}>
          <div className={styles.previewHeader}>
            <div className={styles.previewTitleRow}>
              <span className={styles.previewTitle}>将添加的表模型</span>
              {selectedBusinessFunctionName ? (
                <Tag color="green">{selectedBusinessFunctionName}</Tag>
              ) : null}
            </div>
            <div className={styles.previewStats}>
              共关联 {totalTableCount} 张表，已跳过 {skippedCount} 张已存在表，待新增 {previewTables.length} 张
            </div>
          </div>
          <div className={styles.previewBody}>{renderPreviewContent()}</div>
        </div>
      </div>
      <div className={styles.businessPagination}>
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={total}
          size="small"
          showSizeChanger={false}
          onChange={onPageChange}
        />
      </div>
    </div>
  );
};

export default BusinessFunctionAddPanel;

import React, { useState, useMemo } from 'react';
import { Button, Input, Tag, Popconfirm, Switch } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { RolePermissionTableModelVO } from '../../types';
import styles from './index.module.less';

interface TableListPanelProps {
  tables: RolePermissionTableModelVO[];
  selectedId: string | null;
  onSelect: (tableModelId: string) => void;
  onAdd: () => void;
  onDelete: (tableModelId: string, id: string) => void;
  onToggleEnabled: (tableModelId: string, enabled: boolean) => void;
}

const TableListPanel: React.FC<TableListPanelProps> = ({
  tables,
  selectedId,
  onSelect,
  onAdd,
  onDelete,
  onToggleEnabled,
}) => {
  const [searchText, setSearchText] = useState('');

  const filteredTables = useMemo(() => {
    if (!searchText.trim()) return tables;
    const keyword = searchText.toLowerCase();
    return tables.filter(
      (t) =>
        t.tableName.toLowerCase().includes(keyword) ||
        t.tableComment.toLowerCase().includes(keyword),
    );
  }, [tables, searchText]);

  return (
    <div className={styles.leftPanel}>
      <div className={styles.leftHeader}>
        <span className={styles.leftTitle}>表模型列表</span>
        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={onAdd}>
          新增
        </Button>
      </div>
      <div className={styles.leftSearch}>
        <Input
          placeholder="搜索表名"
          allowClear
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          size="small"
        />
      </div>
      <div className={styles.groupList}>
        {filteredTables.length === 0 ? (
          <div className={styles.leftEmpty}>
            <span className={styles.leftEmptyText}>暂无表模型权限</span>
          </div>
        ) : (
          filteredTables.map((table) => (
            <div
              key={table.tableModelId}
              className={`${styles.groupItem} ${
                selectedId === table.tableModelId ? styles.groupItemActive : ''
              }`}
              onClick={() => onSelect(table.tableModelId)}
            >
              <div className={styles.groupInfo}>
                <div className={styles.groupLabel}>
                  {table.tableComment || table.tableName}
                </div>
                <div className={styles.groupCount}>
                  <span>{table.tableName}</span>
                  <Tag
                    color={table.type === 1 ? 'green' : 'blue'}
                    style={{ marginLeft: 4, fontSize: 11 }}
                  >
                    {table.type === 1 ? '自定义' : '接口关联'}
                  </Tag>
                </div>
              </div>
              {table.type === 1 && table.id && (
                <Popconfirm
                  title="确认删除"
                  description="删除后该角色的表模型自定义权限将被清除"
                  onConfirm={(e) => {
                    e?.stopPropagation();
                    onDelete(table.tableModelId, table.id!);
                  }}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    className={styles.groupDeleteBtn}
                    onClick={(e) => e.stopPropagation()}
                  />
                </Popconfirm>
              )}
              {table.type === 0 && (
                <Switch
                  size="small"
                  checked={table.enabled !== false}
                  onClick={(_, e) => e.stopPropagation()}
                  onChange={(checked) => onToggleEnabled(table.tableModelId, checked)}
                />
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TableListPanel;

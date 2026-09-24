import React, { useCallback } from 'react';
import { Table, Switch, Select, Input, InputNumber, Tooltip, Tag } from 'antd';
import type { TableProps } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import type { RolePermissionTableModelVO } from '../../types';
import type { FieldEditRow } from './types';
import styles from './index.module.less';

const STRATEGY_OPTIONS = [
  { label: '无', value: 'NONE' },
  { label: '用户名（张**）', value: 'USERNAME' },
  { label: '身份证（3301**********1234）', value: 'ID_CARD' },
  { label: '手机号（138****1234）', value: 'PHONE' },
  { label: '邮箱（a****b@example.com）', value: 'EMAIL' },
  { label: '地址（浙江省****杭州市****）', value: 'ADDRESS' },
  { label: '自定义', value: 'CUSTOM' },
];

interface FieldConfigTableProps {
  table: RolePermissionTableModelVO | null;
  fieldRows: FieldEditRow[];
  onUpdateRow: (columnName: string, updates: Partial<FieldEditRow>) => void;
}

const FieldConfigTable: React.FC<FieldConfigTableProps> = ({
  table,
  fieldRows,
  onUpdateRow,
}) => {
  const handleShowChange = useCallback(
    (columnName: string, checked: boolean) => {
      onUpdateRow(columnName, { show: checked });
    },
    [onUpdateRow],
  );

  const handleDesensitizeChange = useCallback(
    (columnName: string, checked: boolean) => {
      onUpdateRow(columnName, {
        desensitize: checked,
        strategy: checked ? 'PHONE' : 'NONE',
      });
    },
    [onUpdateRow],
  );

  const handleStrategyChange = useCallback(
    (columnName: string, strategy: string) => {
      onUpdateRow(columnName, { strategy });
    },
    [onUpdateRow],
  );

  const columns: TableProps<FieldEditRow>['columns'] = [
    {
      title: '字段名',
      dataIndex: 'columnName',
      width: 140,
      render: (val: string, row: FieldEditRow) => (
        <span className={row.locked ? styles.lockedText : undefined}>
          {row.locked && (
            <Tooltip title="默认配置，禁止修改">
              <LockOutlined style={{ marginRight: 4, color: '#faad14' }} />
            </Tooltip>
          )}
          {val}
        </span>
      ),
    },
    {
      title: '字段注释',
      dataIndex: 'columnComment',
      width: 160,
      ellipsis: true,
    },
    {
      title: '允许查询',
      dataIndex: 'show',
      width: 90,
      align: 'center',
      render: (val: boolean, row: FieldEditRow) => (
        <Switch
          size="small"
          checked={val}
          disabled={row.locked}
          onChange={(checked) => handleShowChange(row.columnName, checked)}
        />
      ),
    },
    {
      title: '是否脱敏',
      dataIndex: 'desensitize',
      width: 90,
      align: 'center',
      render: (val: boolean, row: FieldEditRow) => (
        <Switch
          size="small"
          checked={val}
          disabled={row.locked}
          onChange={(checked) => handleDesensitizeChange(row.columnName, checked)}
        />
      ),
    },
    {
      title: '脱敏策略',
      dataIndex: 'strategy',
      width: 160,
      render: (val: string, row: FieldEditRow) => (
        <Select
          size="small"
          value={val}
          options={STRATEGY_OPTIONS}
          disabled={row.locked || !row.desensitize}
          onChange={(strategy) => handleStrategyChange(row.columnName, strategy)}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '自定义参数',
      width: 240,
      render: (_: unknown, row: FieldEditRow) => {
        if (!row.desensitize || row.strategy !== 'CUSTOM') return '-';
        if (row.locked) {
          return (
            <span className={styles.customParams}>
              前{row.prefixNoMaskLen ?? 0} 后{row.suffixNoMaskLen ?? 0} 符号{row.symbol ?? '*'}
            </span>
          );
        }
        return (
          <div className={styles.customParams}>
            <InputNumber
              size="small"
              min={0}
              value={row.prefixNoMaskLen ?? 0}
              onChange={(v) => onUpdateRow(row.columnName, { prefixNoMaskLen: v ?? 0 })}
              addonBefore="前"
              style={{ width: 80 }}
            />
            <InputNumber
              size="small"
              min={0}
              value={row.suffixNoMaskLen ?? 0}
              onChange={(v) => onUpdateRow(row.columnName, { suffixNoMaskLen: v ?? 0 })}
              addonBefore="后"
              style={{ width: 80 }}
            />
            <Input
              size="small"
              value={row.symbol ?? '*'}
              onChange={(e) => onUpdateRow(row.columnName, { symbol: e.target.value })}
              addonBefore="符号"
              style={{ width: 80 }}
            />
          </div>
        );
      },
    },
  ];

  if (!table) {
    return (
      <div className={styles.rightEmpty}>
        <span className={styles.rightEmptyText}>请选择左侧表模型</span>
      </div>
    );
  }

  return (
    <div className={styles.fieldConfigWrapper}>
      <div className={styles.tableInfoSection}>
        <span className={styles.tableInfoName}>
          {table.tableComment || table.tableName}
          {table.tableComment && table.tableName !== table.tableComment && (
            <span className={styles.tableInfoTableName}>（{table.tableName}）</span>
          )}
        </span>
        <span className={styles.tableInfoMeta}>
          模块：{table.modulePrefix} &nbsp; 数据源：{table.datasource}
        </span>
        {table.id && (
          <Tag color="green" style={{ marginLeft: 8 }}>
            已自定义配置
          </Tag>
        )}
      </div>
      <div className={styles.fieldTableWrapper}>
        <Table<FieldEditRow>
          rowKey="columnName"
          columns={columns}
          dataSource={fieldRows}
          size="small"
          pagination={false}
          scroll={{ y: 420 }}
          rowClassName={(row) => (row.locked ? styles.lockedRow : '')}
        />
      </div>
    </div>
  );
};

export default FieldConfigTable;

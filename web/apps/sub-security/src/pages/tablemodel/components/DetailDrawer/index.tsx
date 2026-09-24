import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Table, Tag, Descriptions } from 'antd';
import type { TableModelInfo, TableModelDetail, TableModelColumnInfo } from '../../types';
import { getTableModelDetail } from '../../services/tableModel';
import { SOURCE_TYPE_MAP } from '../../types';
import styles from './index.module.less';

interface DetailDrawerProps {
  visible: boolean;
  record: TableModelInfo | null;
  onClose: () => void;
}

const DetailDrawer: React.FC<DetailDrawerProps> = ({ visible, record, onClose }) => {
  const [detail, setDetail] = useState<TableModelDetail | null>(null);
  const [loading, setLoading] = useState(false);

  const loadDetail = useCallback(async () => {
    if (!record) return;
    setLoading(true);
    try {
      const data = await getTableModelDetail(
        record.modulePrefix,
        record.dataSource,
        record.tableName,
      );
      setDetail(data);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [record]);

  useEffect(() => {
    if (visible && record) {
      loadDetail();
    }
  }, [visible, record, loadDetail]);

  /** 解析字段权限配置 */
  const parseFieldConfig = (fieldConfig?: string) => {
    if (!fieldConfig) return { show: true, desensitize: false };
    try {
      return JSON.parse(fieldConfig);
    } catch {
      return { show: true, desensitize: false };
    }
  };

  /** 字段表格列定义 */
  const columnDefs = [
    { title: '序号', dataIndex: 'ordinalPosition', key: 'ordinalPosition', width: 60 },
    { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 150 },
    { title: '类型', dataIndex: 'columnType', key: 'columnType', width: 120 },
    {
      title: '长度',
      key: 'length',
      width: 80,
      render: (_: unknown, r: TableModelColumnInfo) =>
        r.columnLength != null ? `${r.columnLength}${r.columnScale != null ? `,${r.columnScale}` : ''}` : '-',
    },
    {
      title: '可空',
      dataIndex: 'isNullable',
      key: 'isNullable',
      width: 60,
      render: (v: boolean) => v ? <Tag color="default">YES</Tag> : <Tag color="red">NO</Tag>,
    },
    {
      title: '主键',
      dataIndex: 'isPrimaryKey',
      key: 'isPrimaryKey',
      width: 60,
      render: (v: boolean) => v ? <Tag color="blue">PK</Tag> : '-',
    },
    {
      title: '允许查询',
      key: 'show',
      width: 80,
      render: (_: unknown, r: TableModelColumnInfo) => {
        const { show } = parseFieldConfig(r.fieldConfig);
        return show ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>;
      },
    },
    {
      title: '是否脱敏',
      key: 'desensitize',
      width: 80,
      render: (_: unknown, r: TableModelColumnInfo) => {
        const { desensitize } = parseFieldConfig(r.fieldConfig);
        return desensitize ? <Tag color="orange">是</Tag> : <Tag color="default">否</Tag>;
      },
    },
    { title: '默认值', dataIndex: 'defaultValue', key: 'defaultValue', width: 100, ellipsis: true },
    {
      title: '枚举值',
      dataIndex: 'dictKey',
      key: 'dictKey',
      width: 150,
      render: (text: string | null) => text ? <Tag color="purple">{text}</Tag> : '无',
    },
    { title: '注释', dataIndex: 'columnComment', key: 'columnComment', width: 180, ellipsis: true },
  ];

  /** 外键表格列定义 */
  const fkColumnDefs = [
    { title: '约束名', dataIndex: 'constraintName', key: 'constraintName', width: 160 },
    { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 120 },
    { title: '引用表', dataIndex: 'referencedTableName', key: 'referencedTableName', width: 150 },
    { title: '引用字段', dataIndex: 'referencedColumnName', key: 'referencedColumnName', width: 120 },
    {
      title: '类型',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 80,
      render: (v: number) =>
        v === 0 ? <Tag color="blue">采集</Tag> : <Tag color="green">自定义</Tag>,
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', width: 180, ellipsis: true },
  ];

  return (
    <Drawer
      title="表模型详情"
      open={visible}
      size={900}
      onClose={onClose}
      loading={loading}
      destroyOnHidden
    >
      <div className={styles.drawerContent}>
        {/* 基本信息 */}
        {record && (
          <Descriptions
            column={2}
            bordered
            size="small"
            style={{ marginBottom: 24 }}
          >
            <Descriptions.Item label="表名">
              {record.tableName}
            </Descriptions.Item>
            <Descriptions.Item label="所属模块">
              {record.modulePrefix}
            </Descriptions.Item>
            <Descriptions.Item label="数据源">
              {record.dataSource}
            </Descriptions.Item>
            <Descriptions.Item label="来源类型">
              <Tag color={SOURCE_TYPE_MAP[record.sourceType]?.color}>
                {SOURCE_TYPE_MAP[record.sourceType]?.text}
              </Tag>
            </Descriptions.Item>
            {record.tableComment && (
              <Descriptions.Item label="表注释" span={2}>
                {record.tableComment}
              </Descriptions.Item>
            )}
          </Descriptions>
        )}

        {/* 字段信息 */}
        <div className={styles.columnTable}>
          <div className={styles.sectionTitle}>
            <span>字段信息（{detail?.columns.length || 0}）</span>
          </div>
          <Table
            rowKey="id"
            size="small"
            dataSource={detail?.columns || []}
            columns={columnDefs}
            pagination={false}
            scroll={{ y: 300 }}
          />
        </div>

        {/* 外键信息 */}
        <div>
          <div className={styles.sectionTitle}>
            <span>外键信息（{detail?.foreignKeys.length || 0}）</span>
          </div>
          <Table
            rowKey="id"
            size="small"
            dataSource={detail?.foreignKeys || []}
            columns={fkColumnDefs}
            pagination={false}
            scroll={{ y: 200 }}
          />
        </div>
      </div>
    </Drawer>
  );
};

export default DetailDrawer;

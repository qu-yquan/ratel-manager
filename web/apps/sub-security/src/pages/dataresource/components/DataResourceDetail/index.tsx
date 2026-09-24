import React from 'react';
import { Drawer, Descriptions, Tag, Table } from 'antd';
import type { TableProps } from 'antd';
import styles from './index.module.less';
import type {
  DataResourceInfo,
  DataResourceCondition,
  StringEnumOption,
  ResourceAttribute,
} from '../../types';

interface DataResourceDetailProps {
  visible: boolean;
  data: DataResourceInfo | null;
  assertTypeOptions: StringEnumOption[];
  conditionTypeOptions: StringEnumOption[];
  resourceAttributes: ResourceAttribute[];
  onClose: () => void;
}

const getAssertTypeLabel = (
  options: StringEnumOption[],
  value: string,
): string => {
  return options.find((o) => o.value === value)?.label ?? value;
};

const getConditionTypeLabel = (
  options: StringEnumOption[],
  value: string,
): string => {
  return options.find((o) => o.value === value)?.label ?? value;
};

const getResourceFieldLabel = (
  attributes: ResourceAttribute[],
  key: string,
): string => {
  const attr = attributes.find((a) => a.key === key);
  return attr ? `${attr.desc}(${key})` : key;
};

const DataResourceDetail: React.FC<DataResourceDetailProps> = ({
  visible,
  data,
  assertTypeOptions,
  conditionTypeOptions,
  resourceAttributes,
  onClose,
}) => {
  const conditionColumns: TableProps<DataResourceCondition>['columns'] = [
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: DataResourceCondition, index: number) =>
        index + 1,
    },
    {
      title: '字段名',
      dataIndex: 'fieldName',
      width: 140,
    },
    {
      title: '用户资源字段',
      dataIndex: 'userResourceFields',
      width: 200,
      render: (val: string[]) =>
        val?.map((v) => (
          <Tag key={v}>{getResourceFieldLabel(resourceAttributes, v)}</Tag>
        )),
    },
    {
      title: '断言类型',
      dataIndex: 'assertType',
      width: 100,
      render: (val: string) => getAssertTypeLabel(assertTypeOptions, val),
    },
    {
      title: '关联关系',
      dataIndex: 'relationship',
      width: 100,
      render: (val: string) =>
        val ? getConditionTypeLabel(conditionTypeOptions, val) : '-',
    },
    {
      title: '显示Null',
      dataIndex: 'showNull',
      width: 80,
      render: (val: boolean) => (val ? '是' : '否'),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: 60,
    },
  ];

  return (
    <Drawer
      title="数据资源详情"
      placement="right"
      size={640}
      open={visible}
      onClose={onClose}
      className={styles.drawerBody}
      destroyOnHidden
    >
      {data && (
        <>
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Catalog">
              {data.catalogName || '（全部）'}
            </Descriptions.Item>
            <Descriptions.Item label="库名/Schema">
              {data.schemaName || '（全部）'}
            </Descriptions.Item>
            <Descriptions.Item label="表名">
              <code>{data.tableName}</code>
            </Descriptions.Item>
            <Descriptions.Item label="描述">
              {data.description || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="支持SELF_ONLY过滤">
              {data.supportSelfOnly ? '是' : '否'}
            </Descriptions.Item>
            {data.supportSelfOnly && (
              <Descriptions.Item label="SELF_ONLY过滤字段">
                {data.selfOnlyField ? <code>{data.selfOnlyField}</code> : '-'}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="状态">
              <Tag color={data.status ? 'green' : 'red'}>
                {data.status ? '启用' : '禁用'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {data.createTime || '-'}
            </Descriptions.Item>
          </Descriptions>
          <div className={styles.conditionSection}>
            <div className={styles.sectionTitle}>字段条件</div>
            <Table<DataResourceCondition>
              rowKey="id"
              columns={conditionColumns}
              dataSource={data.conditions ?? []}
              size="small"
              pagination={false}
            />
          </div>
        </>
      )}
    </Drawer>
  );
};

export default DataResourceDetail;

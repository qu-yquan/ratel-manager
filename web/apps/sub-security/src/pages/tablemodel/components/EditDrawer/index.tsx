import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Table, Tag, Input, Button, Space, message, Form, Descriptions, Popconfirm, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { TableModelInfo, TableModelDetail, TableModelColumnInfo, TableModelForeignKeyInfo } from '../../types';
import { getTableModelDetail, updateColumnComment, updateColumnDictKey, updateForeignKeyRemark, saveForeignKey, deleteForeignKeys, updateTableComment } from '../../services/tableModel';
import { post } from '@gwsu/core';
import { SOURCE_TYPE_MAP } from '../../types';
import styles from './index.module.less';

interface EditDrawerProps {
  visible: boolean;
  record: TableModelInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const EditDrawer: React.FC<EditDrawerProps> = ({ visible, record, onClose, onSuccess }) => {
  const [detail, setDetail] = useState<TableModelDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [editingColumnId, setEditingColumnId] = useState<string | null>(null);
  const [editingFkId, setEditingFkId] = useState<string | null>(null);
  const [editingTableComment, setEditingTableComment] = useState(false);
  const [fkFormVisible, setFkFormVisible] = useState(false);
  const [fkForm] = Form.useForm();
  const [editingFkData, setEditingFkData] = useState<TableModelForeignKeyInfo | null>(null);
  const [dictOptions, setDictOptions] = useState<{ label: string; value: string }[]>([]);
  const [dictFetching, setDictFetching] = useState(false);

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
      setEditingTableComment(false);
      setEditingColumnId(null);
      setEditingFkId(null);
    }
  }, [visible, record, loadDetail]);

  /** 修改表注释 */
  const handleTableCommentSave = useCallback(async (comment: string) => {
    if (!record) return;
    try {
      await updateTableComment(record.id, comment);
      message.success('表注释修改成功');
      setEditingTableComment(false);
      loadDetail();
      onSuccess();
    } catch {
      // request 层已自动提示
    }
  }, [record, loadDetail, onSuccess]);

  /** 修改字段注释 */
  const handleColumnCommentSave = useCallback(async (columnId: string, comment: string) => {
    try {
      await updateColumnComment(columnId, comment);
      message.success('注释修改成功');
      setEditingColumnId(null);
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

  /** 修改字段字典键 */
  const handleColumnDictKeySave = useCallback(async (columnId: string, dictKey: string | null) => {
    try {
      await updateColumnDictKey(columnId, dictKey);
      message.success('枚举值修改成功');
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

  /** 搜索字典键 */
  const handleDictSearch = useCallback(async (searchText: string) => {
    setDictFetching(true);
    try {
      const res = await post<{ records: { dictKey: string; dictName: string }[] }>('/security/dict/page', {
        dictKey: searchText || undefined,
        dictName: searchText || undefined,
        pageNum: 1,
        pageSize: 50,
      });
      const records = res.data?.records ?? [];
      setDictOptions(records.map((item) => ({ label: `${item.dictKey}（${item.dictName}）`, value: item.dictKey })));
    } catch {
      setDictOptions([]);
    } finally {
      setDictFetching(false);
    }
  }, []);

  /** 修改外键备注 */
  const handleFkRemarkSave = useCallback(async (fkId: string, remark: string) => {
    try {
      await updateForeignKeyRemark(fkId, remark);
      message.success('备注修改成功');
      setEditingFkId(null);
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

  /** 保存外键 */
  const handleFkSave = useCallback(async () => {
    try {
      const values = await fkForm.validateFields();
      const data = editingFkData
        ? { ...values, id: editingFkData.id, tableId: record?.id }
        : { ...values, tableId: record?.id, dataType: 1 };
      await saveForeignKey(data);
      message.success(editingFkData ? '修改成功' : '添加成功');
      setFkFormVisible(false);
      setEditingFkData(null);
      fkForm.resetFields();
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [fkForm, editingFkData, record, loadDetail]);

  /** 删除外键 */
  const handleFkDelete = useCallback(async (fkId: string) => {
    try {
      await deleteForeignKeys([fkId]);
      message.success('删除成功');
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

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
      width: 180,
      fixed: 'right' as const,
      render: (text: string | null, r: TableModelColumnInfo) => (
        <Select
          size="small"
          value={text || undefined}
          placeholder="无"
          allowClear
          showSearch
          filterOption={false}
          onSearch={handleDictSearch}
          onChange={(value) => handleColumnDictKeySave(r.id, value ?? null)}
          loading={dictFetching}
          options={dictOptions}
          style={{ width: '100%' }}
          onDropdownVisibleChange={(open) => {
            if (open && dictOptions.length === 0) {
              handleDictSearch('');
            }
          }}
        />
      ),
    },
    {
      title: '注释',
      dataIndex: 'columnComment',
      key: 'columnComment',
      width: 200,
      fixed: 'right' as const,
      render: (text: string | null, r: TableModelColumnInfo) => {
        if (editingColumnId === r.id) {
          return (
            <Input
              size="small"
              defaultValue={text || ''}
              autoFocus
              onPressEnter={(e) => handleColumnCommentSave(r.id, (e.target as HTMLInputElement).value)}
              onBlur={(e) => handleColumnCommentSave(r.id, e.target.value)}
            />
          );
        }
        return (
          <span
            className={styles.editableCell}
            onClick={() => setEditingColumnId(r.id)}
          >
            {text || '-'}
            <EditOutlined style={{ marginLeft: 4, fontSize: 12, color: 'var(--text-secondary-color)' }} />
          </span>
        );
      },
    },
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
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 180,
      fixed: 'right' as const,
      render: (text: string | null, r: TableModelForeignKeyInfo) => {
        if (editingFkId === r.id) {
          return (
            <Input
              size="small"
              defaultValue={text || ''}
              autoFocus
              onPressEnter={(e) => handleFkRemarkSave(r.id, (e.target as HTMLInputElement).value)}
              onBlur={(e) => handleFkRemarkSave(r.id, e.target.value)}
            />
          );
        }
        return (
          <span
            className={styles.editableCell}
            onClick={() => setEditingFkId(r.id)}
          >
            {text || '-'}
            <EditOutlined style={{ marginLeft: 4, fontSize: 12, color: 'var(--text-secondary-color)' }} />
          </span>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right' as const,
      render: (_: unknown, r: TableModelForeignKeyInfo) => {
        if (r.dataType === 1) {
          return (
            <Space size={0}>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setEditingFkData(r);
                  fkForm.setFieldsValue(r);
                  setFkFormVisible(true);
                }}
              >
                编辑
              </Button>
              <Popconfirm
                title="确认删除"
                description="确定删除该外键？"
                onConfirm={() => handleFkDelete(r.id)}
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          );
        }
        return null;
      },
    },
  ];

  return (
    <Drawer
      title="编辑表模型"
      open={visible}
      size={900}
      onClose={onClose}
      loading={loading}
      destroyOnHidden
    >
      <div className={styles.drawerContent}>
        {/* 基本信息（表注释可编辑） */}
        {record && detail && (
          <div className={styles.basicInfoSection}>
            <Descriptions column={2} bordered size="small">
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
              <Descriptions.Item label="表注释" span={2}>
                {editingTableComment ? (
                  <Input
                    size="small"
                    defaultValue={record.tableComment || ""}
                    autoFocus
                    onPressEnter={(e) =>
                      handleTableCommentSave(
                        (e.target as HTMLInputElement).value
                      )
                    }
                    onBlur={(e) => handleTableCommentSave(e.target.value)}
                  />
                ) : (
                  <span
                    className={styles.editableCell}
                    onClick={() => setEditingTableComment(true)}
                  >
                    {record.tableComment || "-"}
                    <EditOutlined
                      style={{
                        marginLeft: 4,
                        fontSize: 12,
                        color: "var(--text-secondary-color)",
                      }}
                    />
                  </span>
                )}
              </Descriptions.Item>
            </Descriptions>
          </div>
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
            scroll={{ x: 1170, y: 300 }}
          />
        </div>

        {/* 外键信息 */}
        <div>
          <div className={styles.sectionTitle}>
            <span>外键信息（{detail?.foreignKeys.length || 0}）</span>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingFkData(null);
                fkForm.resetFields();
                setFkFormVisible(true);
              }}
            >
              添加外键
            </Button>
          </div>
          <Table
            rowKey="id"
            size="small"
            dataSource={detail?.foreignKeys || []}
            columns={fkColumnDefs}
            pagination={false}
            scroll={{ x: 830, y: 200 }}
          />
        </div>

        {/* 外键编辑弹窗 */}
        <Drawer
          title={editingFkData ? "编辑外键" : "添加外键"}
          open={fkFormVisible}
          size={420}
          onClose={() => {
            setFkFormVisible(false);
            setEditingFkData(null);
            fkForm.resetFields();
          }}
          extra={
            <Space>
              <Button onClick={() => setFkFormVisible(false)}>取消</Button>
              <Button type="primary" onClick={handleFkSave}>
                保存
              </Button>
            </Space>
          }
        >
          <Form form={fkForm} layout="vertical">
            <Form.Item
              name="constraintName"
              label="约束名"
              rules={[{ required: true, message: "请输入约束名" }]}
            >
              <Input
                placeholder="请输入约束名"
                disabled={editingFkData?.dataType === 0}
              />
            </Form.Item>
            <Form.Item
              name="columnName"
              label="字段名"
              rules={[{ required: true, message: "请输入字段名" }]}
            >
              <Input
                placeholder="请输入字段名"
                disabled={editingFkData?.dataType === 0}
              />
            </Form.Item>
            <Form.Item
              name="referencedTableName"
              label="引用表"
              rules={[{ required: true, message: "请输入引用表" }]}
            >
              <Input
                placeholder="请输入引用表"
                disabled={editingFkData?.dataType === 0}
              />
            </Form.Item>
            <Form.Item
              name="referencedColumnName"
              label="引用字段"
              rules={[{ required: true, message: "请输入引用字段" }]}
            >
              <Input
                placeholder="请输入引用字段"
                disabled={editingFkData?.dataType === 0}
              />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea placeholder="请输入备注" rows={3} />
            </Form.Item>
          </Form>
        </Drawer>
      </div>
    </Drawer>
  );
};

export default EditDrawer;

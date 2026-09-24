import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Form, Input, Button, App, Checkbox, Empty, Space } from 'antd';
import { getBusinessFunctionDetail, saveOrUpdateBusinessFunction } from '../../services/businessFunction';
import { getTableModelPage } from '../../services/tableModel';
import type { BusinessFunctionInfo, TableModelInfo, ModuleInfo } from '../../types';
import { getModuleList } from '../../services/tableModel';
import MarkdownEditor from '@/components/MarkdownEditor';
import styles from './index.module.less';

interface BusinessFunctionDrawerProps {
  visible: boolean;
  editData: BusinessFunctionInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const DEFAULT_DETAIL_TEMPLATE = `## 业务描述
[描述该业务的核心功能和目标]

## 业务规则
- [规则1]
- [规则2]

## 状态说明
| 状态值 | 含义 | 触发条件 |
|--------|------|---------|
| 0 | xxx | xxx |

## 典型示例
### Q: [常见问题]
A: [SQL/操作示例]
`;

const BusinessFunctionDrawer: React.FC<BusinessFunctionDrawerProps> = ({
  visible,
  editData,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState('');

  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [activeModule, setActiveModule] = useState<string>('');
  const [allTables, setAllTables] = useState<TableModelInfo[]>([]);
  const [selectedTableIds, setSelectedTableIds] = useState<string[]>([]);

  const isEdit = !!editData?.id;

  useEffect(() => {
    if (visible) {
      if (editData) {
        form.setFieldsValue({
          name: editData.name,
          summary: editData.summary,
          detail: editData.detail,
          sortOrder: editData.sortOrder,
        });
        setDetail(editData.detail || '');
        if (editData.id) {
          getBusinessFunctionDetail(editData.id)
            .then((res) => {
              setSelectedTableIds(res.tables?.map((t) => t.id) || []);
            })
            .catch(() => {});
        }
      } else {
        form.resetFields();
        setDetail(DEFAULT_DETAIL_TEMPLATE);
        form.setFieldValue('detail', DEFAULT_DETAIL_TEMPLATE);
        setSelectedTableIds([]);
      }
      getModuleList()
        .then((mods) => {
          setModules(mods);
          if (mods.length > 0) {
            setActiveModule(mods[0].prefix);
          }
        })
        .catch(() => {});
    }
  }, [visible, editData, form]);

  useEffect(() => {
    if (activeModule) {
      getTableModelPage({ modulePrefix: activeModule, pageNum: 1, pageSize: 500 })
        .then((res) => {
          setAllTables(res.records);
        })
        .catch(() => {
          setAllTables([]);
        });
    }
  }, [activeModule]);

  const handleDetailChange = useCallback(
    (value: string) => {
      setDetail(value);
      form.setFieldValue('detail', value);
    },
    [form],
  );

  const handleTableCheck = useCallback(
    (tableId: string, checked: boolean) => {
      setSelectedTableIds((prev) =>
        checked ? [...prev, tableId] : prev.filter((id) => id !== tableId),
      );
    },
    [],
  );

  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      if (!detail.trim()) {
        message.warning('请输入详细介绍');
        return;
      }
      setLoading(true);
      const data: BusinessFunctionInfo = {
        ...editData,
        ...values,
        detail,
        tableIds: selectedTableIds,
      };
      await saveOrUpdateBusinessFunction(data);
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch {} finally {
      setLoading(false);
    }
  }, [form, editData, detail, selectedTableIds, isEdit, onSuccess, message]);

  return (
    <Drawer
      title={isEdit ? '编辑业务功能' : '新增业务功能'}
      size={720}
      open={visible}
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" data-ai-approval loading={loading} onClick={handleOk}>
            保存
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" className={styles.drawerBody}>
        <Form.Item
          name="name"
          label="业务名称"
          rules={[{ required: true, message: '请输入业务名称' }]}
        >
          <Input placeholder="请输入业务名称" maxLength={128} />
        </Form.Item>

        <Form.Item
          name="summary"
          label="业务简介"
          rules={[{ required: true, message: '请输入业务简介' }]}
        >
          <Input.TextArea
            placeholder="请输入业务简介"
            maxLength={512}
            rows={2}
            showCount
          />
        </Form.Item>

        <div className={styles.mdEditorLabel}><span className={styles.requiredMark}>*</span>详细介绍（Markdown）</div>
        <MarkdownEditor
          value={detail}
          onChange={handleDetailChange}
          placeholder="请输入Markdown格式的详细介绍，支持业务描述、规则说明、示例等内容"
        />

        <Form.Item name="sortOrder" label="排序号">
          <Input type="number" placeholder="请输入排序号" />
        </Form.Item>

        <Form.Item label="关联表模型">
          <div className={styles.tableSelector}>
            <div style={{ display: 'flex', height: 320 }}>
              <div className={styles.moduleTree} style={{ width: 180 }}>
                {modules.map((mod) => (
                  <div
                    key={mod.prefix}
                    className={`${styles.moduleItem} ${
                      activeModule === mod.prefix ? styles.moduleItemActive : ''
                    }`}
                    onClick={() => setActiveModule(mod.prefix)}
                  >
                    {mod.note || mod.prefix}
                  </div>
                ))}
              </div>
              <div className={styles.tableCheckList} style={{ flex: 1 }}>
                {allTables.length === 0 ? (
                  <Empty description="暂无表模型" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  allTables.map((table) => (
                    <div key={table.id} className={styles.tableCheckItem}>
                      <Checkbox
                        checked={selectedTableIds.includes(table.id)}
                        onChange={(e) =>
                          handleTableCheck(table.id, e.target.checked)
                        }
                      />
                      <span className={styles.tableName}>{table.tableName}</span>
                      <span className={styles.tableComment}>
                        {table.tableComment}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
            <div className={styles.selectedInfo}>
              已选择 {selectedTableIds.length} 张表
            </div>
          </div>
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default BusinessFunctionDrawer;

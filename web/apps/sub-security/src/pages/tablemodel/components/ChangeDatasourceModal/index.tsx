import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Form, Select, Checkbox, Space, Alert, message, Empty, Spin } from 'antd';
import type { TableModelInfo, ApiResourceSimple, ModuleInfo } from '../../types';
import { changeDatasource, getDatasourceList, listApiByTableModel } from '../../services/tableModel';
import styles from './index.module.less';

interface ChangeDatasourceModalProps {
  visible: boolean;
  record: TableModelInfo | null;
  modules: ModuleInfo[];
  onClose: () => void;
  onSuccess: () => void;
}

const ChangeDatasourceModal: React.FC<ChangeDatasourceModalProps> = ({ visible, record, modules, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [apiList, setApiList] = useState<ApiResourceSimple[]>([]);
  const [apiLoading, setApiLoading] = useState(false);
  const [selectedApiIds, setSelectedApiIds] = useState<string[]>([]);
  const [datasourceOptions, setDatasourceOptions] = useState<string[]>([]);
  const [applyToAll, setApplyToAll] = useState(true);

  /** 加载关联接口 */
  const loadApiList = useCallback(async () => {
    if (!record) return;
    setApiLoading(true);
    try {
      const list = await listApiByTableModel({
        modulePrefix: record.modulePrefix,
        datasource: record.dataSource,
        tableName: record.tableName,
      });
      setApiList(list);
    } catch {
      // request 层已自动提示
    } finally {
      setApiLoading(false);
    }
  }, [record]);

  /** 加载数据源列表 */
  const loadDatasources = useCallback(async () => {
    if (!record) return;
    const mod = modules.find((m) => m.prefix === record.modulePrefix);
    if (!mod) return;
    try {
      const dsList = await getDatasourceList(mod.applicationName);
      setDatasourceOptions(dsList.filter((ds) => ds !== record.dataSource));
    } catch {
      // request 层已自动提示
    }
  }, [record, modules]);

  useEffect(() => {
    if (visible && record) {
      loadApiList();
      loadDatasources();
      form.resetFields();
      setSelectedApiIds([]);
      setApplyToAll(true);
    }
  }, [visible, record, loadApiList, loadDatasources, form]);

  /** 全选/取消全选 */
  const handleSelectAll = useCallback((checked: boolean) => {
    if (checked) {
      setSelectedApiIds(apiList.map((api) => api.id));
    } else {
      setSelectedApiIds([]);
    }
  }, [apiList]);

  /** 单选 */
  const handleSelectApi = useCallback((apiId: string, checked: boolean) => {
    setSelectedApiIds((prev) =>
      checked ? [...prev, apiId] : prev.filter((id) => id !== apiId),
    );
  }, []);

  /** 提交 */
  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const mod = modules.find((m) => m.prefix === record!.modulePrefix);

      await changeDatasource({
        applicationName: mod?.applicationName ?? '',
        tableModelId: record!.id,
        newDatasource: values.newDatasource,
        apiIds: applyToAll ? undefined : selectedApiIds,
      });

      message.success('数据源修改成功');
      onSuccess();
      onClose();
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [form, record, modules, applyToAll, selectedApiIds, onSuccess, onClose]);

  return (
    <Modal
      title="修改数据源"
      open={visible}
      width={600}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item label="当前数据源">
          <span style={{ fontWeight: 500 }}>{record?.dataSource}</span>
        </Form.Item>
        <Form.Item
          name="newDatasource"
          label="新数据源"
          rules={[{ required: true, message: "请选择新数据源" }]}
        >
          <Select
            placeholder="请选择新数据源"
            options={datasourceOptions.map((ds) => ({ label: ds, value: ds }))}
          />
        </Form.Item>
      </Form>

      {/* 采集类型的表模型才有关联接口 */}
      {record?.sourceType === 0 && apiList.length > 0 && (
        <div className={styles.apiListSection}>
          <Alert
            message="此表模型关联了接口，请选择修改范围"
            description={
              applyToAll
                ? "将修改所有关联接口的数据源"
                : `已选择 ${selectedApiIds.length} / ${apiList.length} 个接口`
            }
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
          />

          <div style={{ marginBottom: 8 }}>
            <Checkbox
              checked={applyToAll}
              onChange={(e) => setApplyToAll(e.target.checked)}
            >
              修改所有关联接口的数据源
            </Checkbox>
          </div>

          {!applyToAll && (
            <>
              <div className={styles.selectAllRow}>
                <Checkbox
                  checked={selectedApiIds.length === apiList.length}
                  indeterminate={
                    selectedApiIds.length > 0 &&
                    selectedApiIds.length < apiList.length
                  }
                  onChange={(e) => handleSelectAll(e.target.checked)}
                >
                  全选
                </Checkbox>
              </div>
              <Spin spinning={apiLoading}>
                {apiList.length === 0 ? (
                  <Empty description="暂无关联接口" />
                ) : (
                  <div style={{ maxHeight: 200, overflow: "auto" }}>
                    {apiList.map((api) => (
                      <div key={api.id} className={styles.apiItem}>
                        <Checkbox
                          checked={selectedApiIds.includes(api.id)}
                          onChange={(e) =>
                            handleSelectApi(api.id, e.target.checked)
                          }
                        >
                          <Space size={4}>
                            <span>{api.reqMethod}</span>
                            <span>{api.reqPath}</span>
                            {api.summary && (
                              <span
                                style={{ color: "var(--text-color-secondary)" }}
                              >
                                - {api.summary}
                              </span>
                            )}
                          </Space>
                        </Checkbox>
                      </div>
                    ))}
                  </div>
                )}
              </Spin>
            </>
          )}
        </div>
      )}

      {record?.sourceType === 1 && (
        <Alert
          message="自定义添加的表模型不关联接口，将直接修改数据源"
          type="info"
          showIcon
        />
      )}
    </Modal>
  );
};

export default ChangeDatasourceModal;

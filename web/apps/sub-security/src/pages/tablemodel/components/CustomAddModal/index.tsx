import React, { useState, useCallback } from 'react';
import { Modal, Form, Select, message } from 'antd';
import type { ModuleInfo } from '../../types';
import { customSaveTableModel, getDatasourceList, getTableList } from '../../services/tableModel';
import styles from './index.module.less';

interface CustomAddModalProps {
  visible: boolean;
  modules: ModuleInfo[];
  onClose: () => void;
  onSuccess: () => void;
}

const CustomAddModal: React.FC<CustomAddModalProps> = ({ visible, modules, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [datasourceOptions, setDatasourceOptions] = useState<string[]>([]);
  const [tableOptions, setTableOptions] = useState<{ name: string; remark: string }[]>([]);

  const selectedModulePrefix = Form.useWatch('modulePrefix', form);

  /** 模块变化时加载数据源 */
  const handleModuleChange = useCallback(async (modulePrefix: string) => {
    form.setFieldsValue({ datasource: undefined, tableName: undefined });
    setDatasourceOptions([]);
    setTableOptions([]);
    if (!modulePrefix) return;

    const mod = modules.find((m) => m.prefix === modulePrefix);
    if (!mod) return;

    try {
      const dsList = await getDatasourceList(mod.applicationName);
      setDatasourceOptions(dsList);
    } catch {
      // request 层已自动提示
    }
  }, [form, modules]);

  /** 数据源变化时加载表列表 */
  const handleDatasourceChange = useCallback(async (datasource: string) => {
    form.setFieldsValue({ tableName: undefined });
    setTableOptions([]);
    if (!datasource || !selectedModulePrefix) return;

    const mod = modules.find((m) => m.prefix === selectedModulePrefix);
    if (!mod) return;

    try {
      const tables = await getTableList(mod.applicationName, datasource);
      setTableOptions(tables);
    } catch {
      // request 层已自动提示
    }
  }, [form, modules, selectedModulePrefix]);

  /** 提交 */
  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const mod = modules.find((m) => m.prefix === values.modulePrefix);
      if (!mod) return;

      await customSaveTableModel({
        applicationName: mod.applicationName,
        modulePrefix: values.modulePrefix,
        datasource: values.datasource,
        tableName: values.tableName,
      });

      message.success('添加成功');
      form.resetFields();
      setDatasourceOptions([]);
      setTableOptions([]);
      onSuccess();
      onClose();
    } catch (err) {
      if (err instanceof Error) {
        message.error(err.message);
      }
    } finally {
      setLoading(false);
    }
  }, [form, modules, onSuccess, onClose]);

  /** 关闭 */
  const handleClose = useCallback(() => {
    form.resetFields();
    setDatasourceOptions([]);
    setTableOptions([]);
    onClose();
  }, [form, onClose]);

  return (
    <Modal
      title="自定义添加表模型"
      open={visible}
      width={520}
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={loading}
      destroyOnHidden
      className={styles.customAddModal}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="modulePrefix"
          label="所属模块"
          rules={[{ required: true, message: "请选择所属模块" }]}
        >
          <Select
            placeholder="请选择所属模块"
            onChange={handleModuleChange}
            options={modules.map((m) => ({
              label: m.note || m.prefix,
              value: m.prefix,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="datasource"
          label="数据源"
          rules={[{ required: true, message: "请选择数据源" }]}
        >
          <Select
            placeholder="请选择数据源"
            onChange={handleDatasourceChange}
            disabled={!datasourceOptions.length}
            options={datasourceOptions.map((ds) => ({ label: ds, value: ds }))}
          />
        </Form.Item>
        <Form.Item
          name="tableName"
          label="表名"
          rules={[{ required: true, message: "请选择表名" }]}
        >
          <Select
            placeholder="请选择表名"
            showSearch
            disabled={!tableOptions.length}
            options={tableOptions.map((t) => ({
              label: t.remark ? `${t.name}（${t.remark}）` : t.name,
              value: t.name,
            }))}
            filterOption={(input, option) =>
              (option?.label as string)
                ?.toLowerCase()
                .includes(input.toLowerCase())
            }
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CustomAddModal;

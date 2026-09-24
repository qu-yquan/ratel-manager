import React from 'react';
import {Button, Drawer, Form, Input, App, Select, TreeSelect} from 'antd';
import styles from './index.module.less';
import type {SysUserVO} from '../../types';
import type {DeptTreeNode} from '../../../dept/types';
import {saveOrUpdateUser} from '@/services/user';
import {encryptPassword} from '@gwsu/core';

interface UserFormProps {
    visible: boolean;
    onClose: () => void;
    onSuccess: () => void;
    treeData?: DeptTreeNode[];
    defaultDeptId?: string;
}

const UserForm: React.FC<UserFormProps> = ({visible, onClose, onSuccess, treeData = [], defaultDeptId}) => {
    const { message } = App.useApp();
    const [form] = Form.useForm();
    const [loading, setLoading] = React.useState(false);

    // 转换部门树为 TreeSelect 所需的格式
    const convertTreeData = (data: DeptTreeNode[]): any[] => {
        return data.map((node) => ({
            key: node.id,
            value: node.id,
            title: node.name,
            children: node.children ? convertTreeData(node.children) : undefined,
        }));
    };

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields();
            setLoading(true);
            const submitData = {
                ...values,
                password: values.password ? encryptPassword(values.password) : undefined,
            };
            await saveOrUpdateUser(submitData as SysUserVO);
            message.success('创建成功');
            form.resetFields();
            onSuccess();
        } catch {
            // 验证失败
        } finally {
            setLoading(false);
        }
    };

    return (
      <Drawer
        title="新增用户"
        open={visible}
        onClose={() => {
          form.resetFields();
          onClose();
        }}
        size={480}
        footer={
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
            <Button
              onClick={() => {
                form.resetFields();
                onClose();
              }}
            >
              取消
            </Button>
            <Button
              type="primary"
              data-ai-approval
              loading={loading}
              onClick={handleSubmit}
            >
              创建
            </Button>
          </div>
        }
      >
        <div className={styles.userForm}>
          <Form
            form={form}
            layout="vertical"
            initialValues={{ gender: 0, deptId: defaultDeptId }}
          >
            <Form.Item
              label="用户名"
              name="userName"
              rules={[{ required: true, message: "请输入用户名" }]}
            >
              <Input placeholder="登录用户名，创建后不可修改" />
            </Form.Item>
            <Form.Item
              label="昵称"
              name="nickname"
              rules={[{ required: true, message: "请输入昵称" }]}
            >
              <Input placeholder="显示名称" />
            </Form.Item>
            <Form.Item
              label="初始密码"
              name="password"
              rules={[
                { required: true, message: "请输入初始密码" },
                { min: 6, message: "密码至少6位" },
              ]}
            >
              <Input.Password placeholder="至少6位" />
            </Form.Item>
            <Form.Item
              label="所属部门"
              name="deptId"
              rules={[{ required: true, message: "请选择所属部门" }]}
            >
              <TreeSelect
                placeholder="请选择所属部门"
                treeData={convertTreeData(treeData)}
                treeDefaultExpandAll
                allowClear
                showSearch
                filterTreeNode={(inputValue, treeNode) => {
                  let title = treeNode.title as string;
                  return title.toLowerCase().includes(inputValue.toLowerCase());
                }}
              />
            </Form.Item>
            <Form.Item label="邮箱" name="email">
              <Input placeholder="选填" />
            </Form.Item>
            <Form.Item label="手机号" name="phone">
              <Input placeholder="选填" />
            </Form.Item>
            <Form.Item label="性别" name="gender">
              <Select
                options={[
                  { label: "未知", value: 0 },
                  { label: "男", value: 1 },
                  { label: "女", value: 2 },
                ]}
              />
            </Form.Item>
          </Form>
        </div>
      </Drawer>
    );
};

export default UserForm;

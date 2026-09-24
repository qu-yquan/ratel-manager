import React from "react";
import { Descriptions, Tag, Button, Space, Popconfirm, App } from "antd";
import { EditOutlined, DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { deleteDept, removeParentDept } from "@/services/dept";
import type { DeptDetail, DeptTypeOption } from "../../types";
import { AuthGate } from "@gwsu/core";
import { PERM_EDIT, PERM_REMOVE } from '../../permissionConstants';

interface BasicInfoTabProps {
  dept: DeptDetail;
  deptTypes: DeptTypeOption[];
  onEdit: () => void;
  onDeleteSuccess: () => void;
  onRefresh: () => void;
  onAddParent: () => void;
}

const BasicInfoTab: React.FC<BasicInfoTabProps> = ({
  dept,
  deptTypes,
  onEdit,
  onDeleteSuccess,
  onRefresh,
  onAddParent,
}) => {
  const { message } = App.useApp();
  const getTypeName = (type: number) => {
    const found = deptTypes.find((t) => t.code === type);
    return found?.name || "未知";
  };

  const handleDelete = async () => {
    try {
      await deleteDept(dept.id);
      message.success("删除成功");
      onDeleteSuccess();
    } catch {
      message.error("删除失败");
    }
  };

  const handleRemoveParent = async (parentId: string) => {
    try {
      await removeParentDept(dept.id, parentId);
      message.success("移除父部门成功");
      onRefresh();
    } catch {
      message.error("移除父部门失败");
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <AuthGate buttonKey={PERM_EDIT}>
            <Button type="primary" icon={<EditOutlined />} onClick={onEdit}>
              编辑
            </Button>
          </AuthGate>
          <AuthGate buttonKey={PERM_REMOVE}>
            <Popconfirm
              title="确定要删除该部门吗？"
              description="删除后不可恢复，子部门将移至上级部门"
              onConfirm={handleDelete}
              okText="确定"
              cancelText="取消"
            >
              <Button danger data-ai-approval icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          </AuthGate>
        </Space>
      </div>

      <Descriptions column={2} bordered>
        <Descriptions.Item label="部门名称">{dept.name}</Descriptions.Item>
        <Descriptions.Item label="部门类型">
          <Tag color="blue">{getTypeName(dept.type)}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={dept.enabled ? "success" : "error"}>
            {dept.enabled ? "启用" : "禁用"}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="排序号">{dept.sort}</Descriptions.Item>
        <Descriptions.Item label="主父部门">
          {dept.parentName || "无（根部门）"}
        </Descriptions.Item>
        <Descriptions.Item label="层级路径">
          <span style={{ fontFamily: "monospace", fontSize: 12 }}>
            {dept.path}
          </span>
        </Descriptions.Item>
        <Descriptions.Item label="额外父部门" span={2}>
          <div>
            {dept.extraParents && dept.extraParents.length > 0 ? (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                {dept.extraParents.map((p) => (
                  <Tag
                    key={p.id}
                    closable
                    onClose={(e) => {
                      e.preventDefault();
                      void handleRemoveParent(p.id);
                    }}
                  >
                    {p.name}
                  </Tag>
                ))}
                <AuthGate buttonKey={PERM_EDIT}>
                  <Button
                    type="dashed"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={onAddParent}
                  >
                    添加父部门
                  </Button>
                </AuthGate>
              </div>
            ) : (
              <Button
                type="dashed"
                size="small"
                icon={<PlusOutlined />}
                onClick={onAddParent}
              >
                添加父部门
              </Button>
            )}
          </div>
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
};

export default BasicInfoTab;

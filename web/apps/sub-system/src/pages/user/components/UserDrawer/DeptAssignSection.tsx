import React, { useState } from "react";
import { Tree, App } from "antd";
import type { TreeProps } from "antd";
import {
  BankOutlined,
  ShopOutlined,
  ApartmentOutlined,
  TeamOutlined,
  UsergroupAddOutlined,
} from "@ant-design/icons";
import type { SysUserDeptVO } from "../../types";
import type { DeptTreeNode } from "../../../dept/types";
import { setUserDept, setPrimaryDept, removeUserDept } from "@/services/dept";
import { AuthGate } from "@gwsu/core";
import { PERM_EDIT } from '../../permissionConstants';

interface DeptAssignSectionProps {
  userId: string;
  depts: SysUserDeptVO[];
  treeData: DeptTreeNode[];
  onRefresh: () => void;
  readOnly?: boolean;
}

const DeptAssignSection: React.FC<DeptAssignSectionProps> = ({
  userId,
  depts,
  treeData,
  onRefresh,
  readOnly = false,
}) => {
  const { message } = App.useApp();
  const [showTree, setShowTree] = useState(false);

  const deptIdSet = new Set(depts.map((d) => d.deptId));
  const primaryDeptId = depts.find((d) => d.isPrimary)?.deptId;

  const getDeptIcon = (type: number) => {
    const iconMap: Record<number, React.ReactNode> = {
      1: <BankOutlined />,
      2: <ShopOutlined />,
      3: <ApartmentOutlined />,
      4: <TeamOutlined />,
      5: <UsergroupAddOutlined />,
    };
    return iconMap[type] || <ApartmentOutlined />;
  };

  const handleSetPrimary = async (deptId: string) => {
    try {
      await setPrimaryDept({ userId, deptId });
      message.success("已设为主部门");
      onRefresh();
    } catch {
      message.error("操作失败");
    }
  };

  const handleRemove = async (deptId: string) => {
    if (deptId === primaryDeptId) {
      message.warning("主部门不能直接移除，请先设置其他部门为主部门");
      return;
    }
    try {
      await removeUserDept({ userId, deptIds: [deptId] });
      message.success("已移除");
      onRefresh();
    } catch {
      message.error("操作失败");
    }
  };

  const handleCheck: TreeProps["onCheck"] = async (_checkedKeys, info) => {
    const deptId = info.node.key as string;
    if (info.checked) {
      const newDeptIds = [...depts.map((d) => d.deptId), deptId];
      try {
        await setUserDept({
          userId,
          deptIds: newDeptIds,
          primaryDeptId: primaryDeptId || deptId,
        });
        message.success("已添加部门");
        onRefresh();
      } catch {
        message.error("操作失败");
      }
    }
  };

  const convertToTreeData = (data: DeptTreeNode[]): TreeProps["treeData"] => {
    return data.map((node) => ({
      key: node.id,
      title: (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 4,
            fontSize: 12,
          }}
        >
          {getDeptIcon(node.type)}
          <span>{node.name}</span>
          {deptIdSet.has(node.id) && (
            <span style={{ color: "#52c41a", fontSize: 10 }}>已关联</span>
          )}
        </div>
      ),
      children: node.children ? convertToTreeData(node.children) : undefined,
    }));
  };

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 10,
        }}
      >
        <span style={{ fontSize: 13, fontWeight: 600 }}>部门关联</span>
        {!readOnly && (
          <AuthGate buttonKey={PERM_EDIT}>
            <a onClick={() => setShowTree(!showTree)}>
              {showTree ? "收起" : "+ 添加部门"}
            </a>
          </AuthGate>
        )}
      </div>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 6,
          marginBottom: 10,
        }}
      >
        {depts.map((dept) => (
          <div
            key={dept.deptId}
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "8px 12px",
              background: dept.isPrimary ? "#fff7e6" : "#fafafa",
              borderRadius: 6,
              border: `1px solid ${dept.isPrimary ? "#ffe58f" : "#f0f0f0"}`,
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                fontSize: 12,
              }}
            >
              <span>🏢</span>
              <span>{dept.deptName}</span>
              {dept.isPrimary && (
                <span
                  style={{
                    background: "#fa8c16",
                    color: "white",
                    fontSize: 9,
                    padding: "1px 6px",
                    borderRadius: 8,
                  }}
                >
                  主部门
                </span>
              )}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              {!readOnly && !dept.isPrimary && (
                <AuthGate buttonKey={PERM_EDIT}>
                  <a
                    onClick={() => handleSetPrimary(dept.deptId)}
                    data-ai-approval
                    style={{ fontSize: 11 }}
                  >
                    设为主部门
                  </a>
                </AuthGate>
              )}
              {!readOnly && (
                <AuthGate buttonKey={PERM_EDIT}>
                  <a
                    style={{ color: "#ff4d4f", fontSize: 11 }}
                    data-ai-approval
                    onClick={() => handleRemove(dept.deptId)}
                  >
                    移除
                  </a>
                </AuthGate>
              )}
            </div>
          </div>
        ))}
      </div>
      {showTree && (
        <div
          style={{
            background: "#fafafa",
            border: "1px dashed #d9d9d9",
            borderRadius: 6,
            padding: 10,
            fontSize: 11,
          }}
        >
          <div style={{ color: "#999", marginBottom: 6, fontSize: 10 }}>
            勾选部门后自动关联
          </div>
          <Tree
            checkable
            checkStrictly
            checkedKeys={[...deptIdSet]}
            onCheck={handleCheck}
            treeData={convertToTreeData(treeData)}
            defaultExpandAll
          />
        </div>
      )}
    </div>
  );
};

export default DeptAssignSection;

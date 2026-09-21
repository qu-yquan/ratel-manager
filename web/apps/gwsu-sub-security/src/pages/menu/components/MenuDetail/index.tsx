import React, { useState, useEffect, useCallback } from 'react';
import { Button, Tag, Table, Popconfirm, App } from 'antd';
import type { TableProps } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  SelectOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import { getMenuButtons, deleteMenu } from '../../services/menu';
import { post } from '@gwsu/core';
import type { MenuTreeNode, ButtonItem, MenuSaveRequest } from '../../types';
import ApiResourcePicker from '../ApiResourcePicker';
import ButtonFormModal from '../ButtonFormModal';
import {AuthGate} from '@gwsu/core'
import { PERM_EDIT, PERM_REMOVE, PERM_ADD_BUTTON, PERM_EDIT_BUTTON, PERM_REMOVE_BUTTON } from '../../permissionConstants';

const METHOD_COLORS: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
};

interface MenuDetailProps {
  menu: MenuTreeNode;
  owner: number;
  position: number;
  onEdit: (menu: MenuTreeNode) => void;
  onDeleteSuccess: () => void;
  onRefresh: () => void;
}

const MenuDetail: React.FC<MenuDetailProps> = ({
  menu,
  owner,
  position,
  onEdit,
  onDeleteSuccess,
  onRefresh,
}) => {
  const { message } = App.useApp();
  const [buttons, setButtons] = useState<ButtonItem[]>([]);
  const [buttonsLoading, setButtonsLoading] = useState(false);
  const [pickerVisible, setPickerVisible] = useState(false);
  const [buttonFormVisible, setButtonFormVisible] = useState(false);
  const [buttonFormData, setButtonFormData] = useState<ButtonItem | null>(null);

  const loadButtons = useCallback(async () => {
    if (!menu || menu.menuType === 3) return;
    setButtonsLoading(true);
    try {
      const result = await getMenuButtons(owner, menu.id);
      setButtons(result);
    } catch {
      message.error('加载按钮列表失败');
    } finally {
      setButtonsLoading(false);
    }
  }, [menu, owner]);

  useEffect(() => {
    if (menu) {
      loadButtons().catch(() => {});
    }
  }, [menu, loadButtons]);

  const handleDeleteMenu = useCallback(async () => {
    try {
      await deleteMenu([menu.id]);
      message.success('删除成功');
      onDeleteSuccess();
    } catch {
      // request 层已自动提示
    }
  }, [menu, onDeleteSuccess]);

  const handleDeleteButton = useCallback(
    async (btn: ButtonItem) => {
      try {
        await deleteMenu([btn.id]);
        message.success('删除按钮成功');
        await loadButtons();
      } catch {
        // request 层已自动提示
      }
    },
    [loadButtons],
  );

  const handleViewPermissionConfirm = useCallback(
    async (perm: string) => {
      try {
        const reqData: MenuSaveRequest = {
          id: menu.id,
          parentId: menu.parentId,
          menuName: menu.menuName,
          menuType: menu.menuType,
          sort: menu.sort,
          icon: menu.icon || undefined,
          path: menu.path || undefined,
          microApp: menu.microApp || undefined,
          visible: menu.visible,
          status: menu.status,
          permission: perm || undefined,
          buttonKey: menu.buttonKey || undefined,
          description: menu.description || undefined,
          position: menu.position ?? undefined,
          owner: menu.owner ?? undefined,
        };
        await post('/security/menu', reqData);
        message.success('权限配置已更新');
        onRefresh();
      } catch {
        // request 层已自动提示
      }
    },
    [menu, onRefresh],
  );

  /** 渲染权限标识 Tag */
  const renderPermissionTag = (tag: string, idx: number) => {
    const method = tag.split(':')[0] || '';
    return (
      <Tag key={idx} color={METHOD_COLORS[method] || 'default'} style={{ marginBottom: 2 }}>
        {tag}
      </Tag>
    );
  };

  const permissionTags = menu.permission
    ? menu.permission.split(';').filter(Boolean)
    : [];

  const isMenuType = menu.menuType === 2;

  const buttonColumns: TableProps<ButtonItem>["columns"] = [
    {
      title: "按钮名称",
      dataIndex: "menuName",
      width: 120,
    },
    {
      title: "功能描述",
      dataIndex: "description",
      ellipsis: true,
      render: (val: string | null) => val || "-",
    },
    {
      title: "按钮标识",
      dataIndex: "buttonKey",
      width: 180,
      render: (val: string) => <code style={{ fontSize: 12 }}>{val}</code>,
    },
    {
      title: "接口权限",
      dataIndex: "permission",
      className: styles.permissionCol,
      render: (val: string | null) => {
        if (!val) return "-";
        return (
          <div className={styles.permissionTagsInCell}>
            {val
              .split(";")
              .filter(Boolean)
              .map((tag, idx) => renderPermissionTag(tag, idx))}
          </div>
        );
      },
    },
    {
      title: "操作",
      width: 120,
      render: (_: unknown, record: ButtonItem) => (
        <>
          <AuthGate buttonKey={PERM_EDIT_BUTTON}>
            <Button
              type="link"
              size="small"
              onClick={() => {
                setButtonFormData(record);
                setButtonFormVisible(true);
              }}
            >
              编辑
            </Button>
          </AuthGate>
          <AuthGate buttonKey={PERM_REMOVE_BUTTON}>
            <Popconfirm
              title="确定删除此按钮？"
              onConfirm={() => handleDeleteButton(record)}
            >
              <Button type="link" data-ai-approval size="small" danger>
                删除
              </Button>
            </Popconfirm>
          </AuthGate>
        </>
      ),
    },
  ];

  return (
    <div className={styles.detailPanel}>
      {/* 基本信息 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTitle}>基本信息</span>
          <div style={{ display: "flex", gap: 8 }}>
            <AuthGate buttonKey={PERM_EDIT}>
              <Button icon={<EditOutlined />} onClick={() => onEdit(menu)}>
                编辑
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_REMOVE}>
              <Popconfirm
                title="确定删除此菜单？删除后不可恢复"
                onConfirm={handleDeleteMenu}
              >
                <Button data-ai-approval icon={<DeleteOutlined />} danger>
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </div>
        </div>
        <div className={styles.infoGrid}>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>菜单名称</span>
            <span className={styles.infoValue}>{menu.menuName}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>菜单类型</span>
            <span className={styles.infoValue}>
              {menu.menuType === 1
                ? "目录"
                : menu.menuType === 2
                ? "菜单"
                : "按钮"}
            </span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>路由路径</span>
            <span className={styles.infoValue}>{menu.path || "-"}</span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>排序号</span>
            <span className={styles.infoValue}>{menu.sort}</span>
          </div>
          {menu.menuType === 1 && (
            <div className={styles.infoItem}>
              <span className={styles.infoLabel}>图标</span>
              <span className={styles.infoValue}>{menu.icon || "-"}</span>
            </div>
          )}
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>状态</span>
            <span className={styles.infoValue}>
              {menu.status ? "启用" : "禁用"}
            </span>
          </div>
          <div className={styles.infoItem}>
            <span className={styles.infoLabel}>是否显示</span>
            <span className={styles.infoValue}>
              {menu.visible ? "显示" : "隐藏"}
            </span>
          </div>
          <div className={styles.infoItem} style={{ gridColumn: "1 / -1" }}>
            <span className={styles.infoLabel}>功能描述</span>
            <span className={styles.infoValue}>{menu.description || "-"}</span>
          </div>
        </div>
      </div>

      {/* 界面接口权限（仅菜单类型显示） */}
      {isMenuType && (
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionTitle}>界面接口权限</span>
            <AuthGate buttonKey={PERM_EDIT}>
              <Button
                icon={<SelectOutlined />}
                onClick={() => setPickerVisible(true)}
              >
                配置接口
              </Button>
            </AuthGate>
          </div>
          <div className={styles.permissionArea}>
            <div className={styles.permissionTags}>
              {permissionTags.length > 0 ? (
                permissionTags.map((tag, idx) => renderPermissionTag(tag, idx))
              ) : (
                <span className={styles.emptyPermission}>暂未配置接口权限</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 按钮管理（仅菜单类型显示） */}
      {isMenuType && (
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionTitle}>按钮管理</span>
            <AuthGate buttonKey={PERM_ADD_BUTTON}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setButtonFormData(null);
                  setButtonFormVisible(true);
                }}
              >
                新增按钮
              </Button>
            </AuthGate>
          </div>
          <Table<ButtonItem>
            rowKey="id"
            columns={buttonColumns}
            dataSource={buttons}
            loading={buttonsLoading}
            size="small"
            pagination={false}
          />
        </div>
      )}

      {/* 查看权限配置弹窗 */}
      <ApiResourcePicker
        visible={pickerVisible}
        currentPermission={menu.permission || ""}
        onClose={() => setPickerVisible(false)}
        onConfirm={handleViewPermissionConfirm}
      />

      {/* 按钮编辑弹窗 */}
      <ButtonFormModal
        visible={buttonFormVisible}
        mode={buttonFormData ? "edit" : "create"}
        owner={owner}
        position={position}
        parentMenuId={menu.id}
        data={buttonFormData}
        onClose={() => setButtonFormVisible(false)}
        onSuccess={() => {
          setButtonFormVisible(false);
          loadButtons().catch(() => {});
          onRefresh();
        }}
      />
    </div>
  );
};

export default MenuDetail;

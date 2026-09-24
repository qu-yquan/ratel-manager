import React, { useState, useCallback, useEffect, useRef } from 'react';
import { App } from 'antd';
import styles from './index.module.less';
import DeptTreeSelector from '../dept/components/DeptTreeSelector';
import UserTable from './components/UserTable';
import UserDrawer from './components/UserDrawer';
import UserForm from './components/UserForm';
import PasswordDrawer from './components/PasswordDrawer';
import AssignRoleModal from './components/AssignRoleModal';
import { getDeptTree } from '@/services/dept';
import { getUserPage, getDeptUserCount } from '@/services/user';
import type { DeptTreeNode } from '../dept/types';
import type { SysUserVO, SysUserQueryDTO, DeptUserCountMap } from './types';

const UserPage: React.FC = () => {
  const { message } = App.useApp();
  const [treeData, setTreeData] = useState<DeptTreeNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [selectedDeptId, setSelectedDeptId] = useState<string | null>(null);
  const [selectedDeptName, setSelectedDeptName] = useState<string>('');
  const [deptUserCount, setDeptUserCount] = useState<DeptUserCountMap>({});

  const [users, setUsers] = useState<SysUserVO[]>([]);
  const [total, setTotal] = useState(0);
  const [tableLoading, setTableLoading] = useState(false);
  const [query, setQuery] = useState<SysUserQueryDTO>({ pageNum: 1, pageSize: 10 });

  const [drawerVisible, setDrawerVisible] = useState(false);
  const [drawerUserId, setDrawerUserId] = useState<string | null>(null);
  const [drawerMode, setDrawerMode] = useState<'detail' | 'edit'>('detail');
  const [formVisible, setFormVisible] = useState(false);
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [passwordUserId, setPasswordUserId] = useState<string | null>(null);
  const [passwordNickname, setPasswordNickname] = useState<string>('');

  const [assignRoleVisible, setAssignRoleVisible] = useState(false);
  const [assignRoleUserId, setAssignRoleUserId] = useState<string | null>(null);
  const [assignRoleNickname, setAssignRoleNickname] = useState<string>('');

  const [treeWidth, setTreeWidth] = useState(280);
  const isResizing = useRef(false);
  const startX = useRef(0);
  const startWidth = useRef(280);

  // 加载部门树
  const loadTreeData = useCallback(async () => {
    setTreeLoading(true);
    try {
      const [tree, counts] = await Promise.all([
        getDeptTree(),
        getDeptUserCount(),
      ]);
      setTreeData(tree);
      setDeptUserCount(counts);
    } catch {
      message.error('加载部门树失败');
    } finally {
      setTreeLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTreeData();
  }, [loadTreeData]);

  // 加载用户列表
  const loadUsers = useCallback(async () => {
    setTableLoading(true);
    try {
      const result = await getUserPage(query);
      setUsers(result.records || []);
      setTotal(result.total || 0);
    } catch {
      message.error('加载用户列表失败');
    } finally {
      setTableLoading(false);
    }
  }, [query]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // 选择部门
  const handleDeptSelect = useCallback((deptId: string) => {
    setSelectedDeptId(deptId);
    const findName = (nodes: DeptTreeNode[]): string => {
      for (const node of nodes) {
        if (node.id === deptId) return node.name;
        if (node.children) {
          const found = findName(node.children);
          if (found) return found;
        }
      }
      return '';
    };
    setSelectedDeptName(findName(treeData));
    setQuery((prev) => ({ ...prev, deptId, pageNum: 1 }));
  }, [treeData]);

  // 查询条件变更
  const handleQueryChange = useCallback((partial: Partial<SysUserQueryDTO>) => {
    setQuery((prev) => ({ ...prev, ...partial }));
  }, []);

  // 查看详情
  const handleDetail = useCallback((userId: string) => {
    setDrawerUserId(userId);
    setDrawerMode('detail');
    setDrawerVisible(true);
  }, []);

  // 编辑（打开详情抽屉，编辑模式）
  const handleEdit = useCallback((userId: string) => {
    setDrawerUserId(userId);
    setDrawerMode('edit');
    setDrawerVisible(true);
  }, []);

  // 新增用户
  const handleCreate = useCallback(() => {
    setFormVisible(true);
  }, []);

  // 新增成功回调
  const handleFormSuccess = useCallback(() => {
    setFormVisible(false);
    loadUsers();
    loadTreeData();
  }, [loadUsers, loadTreeData]);

  // 抽屉关闭
  const handleDrawerClose = useCallback(() => {
    setDrawerVisible(false);
    setDrawerUserId(null);
  }, []);

  // 修改密码
  const handleResetPassword = useCallback((userId: string, nickname: string) => {
    setPasswordUserId(userId);
    setPasswordNickname(nickname);
    setPasswordVisible(true);
  }, []);

  // 修改密码成功回调
  const handlePasswordSuccess = useCallback(() => {
    setPasswordVisible(false);
    setPasswordUserId(null);
    setPasswordNickname('');
  }, []);

  // 修改密码抽屉关闭
  const handlePasswordClose = useCallback(() => {
    setPasswordVisible(false);
    setPasswordUserId(null);
    setPasswordNickname('');
  }, []);

  // 分配角色
  const handleAssignRole = useCallback((userId: string, nickname: string) => {
    setAssignRoleUserId(userId);
    setAssignRoleNickname(nickname);
    setAssignRoleVisible(true);
  }, []);

  // 分配角色成功回调
  const handleAssignRoleSuccess = useCallback(() => {
    setAssignRoleVisible(false);
    setAssignRoleUserId(null);
    setAssignRoleNickname('');
  }, []);

  // 分配角色弹窗关闭
  const handleAssignRoleClose = useCallback(() => {
    setAssignRoleVisible(false);
    setAssignRoleUserId(null);
    setAssignRoleNickname('');
  }, []);

  // 拖拽分割线
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    isResizing.current = true;
    startX.current = e.clientX;
    startWidth.current = treeWidth;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  }, [treeWidth]);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing.current) return;
    const diff = e.clientX - startX.current;
    const newWidth = Math.max(200, Math.min(400, startWidth.current + diff));
    setTreeWidth(newWidth);
  }, []);

  const handleMouseUp = useCallback(() => {
    isResizing.current = false;
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
  }, [handleMouseMove]);

  return (
    <div className={styles.userPage}>
      <div className={styles.treePanel} style={{ width: treeWidth }}>
        <DeptTreeSelector
          treeData={treeData}
          loading={treeLoading}
          selectedKey={selectedDeptId}
          deptUserCount={deptUserCount}
          showUserCount
          onSelect={handleDeptSelect}
        />
      </div>
      <div className={styles.resizeHandle} onMouseDown={handleMouseDown}>
        <div className={styles.handleBar} />
      </div>
      <div className={styles.listPanel}>
        <UserTable
          users={users}
          total={total}
          loading={tableLoading}
          current={query.pageNum || 1}
          pageSize={query.pageSize || 10}
          deptName={selectedDeptName}
          onQueryChange={handleQueryChange}
          onDetail={handleDetail}
          onEdit={handleEdit}
          onCreate={handleCreate}
          onResetPassword={handleResetPassword}
          onAssignRole={handleAssignRole}
          onRefresh={loadUsers}
        />
      </div>
      <UserDrawer
        visible={drawerVisible}
        userId={drawerUserId}
        treeData={treeData}
        onClose={handleDrawerClose}
        mode={drawerMode}
      />
      <UserForm
        visible={formVisible}
        onClose={() => setFormVisible(false)}
        onSuccess={handleFormSuccess}
        treeData={treeData}
        defaultDeptId={selectedDeptId || undefined}
      />
      <PasswordDrawer
        visible={passwordVisible}
        userId={passwordUserId}
        nickname={passwordNickname}
        onClose={handlePasswordClose}
        onSuccess={handlePasswordSuccess}
      />
      <AssignRoleModal
        visible={assignRoleVisible}
        userId={assignRoleUserId}
        nickname={assignRoleNickname}
        onClose={handleAssignRoleClose}
        onSuccess={handleAssignRoleSuccess}
      />
    </div>
  );
};

export default UserPage;

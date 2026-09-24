import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Button, Spin } from 'antd';
import styles from './index.module.less';
import TableListPanel from './TableListPanel';
import FieldConfigTable from './FieldConfigTable';
import AddTableModelModal from './AddTableModelModal';
import { useTableModelPermission } from './hooks/useTableModelPermission';

interface TableModelPermissionModalProps {
  visible: boolean;
  roleId: string | null;
  roleName: string;
  onClose: () => void;
}

const TableModelPermissionModal: React.FC<TableModelPermissionModalProps> = ({
  visible,
  roleId,
  roleName,
  onClose,
}) => {
  const {
    tables,
    selectedTableId,
    selectedTable,
    fieldRows,
    loading,
    saving,
    loadData,
    selectTable,
    updateFieldRow,
    handleSave,
    handleReset,
    handleDeleteTable,
    toggleEnabled,
    removeTableFromList,
    setRoleId,
  } = useTableModelPermission();

  const [addModalVisible, setAddModalVisible] = useState(false);

  useEffect(() => {
    if (visible && roleId) {
      setRoleId(roleId);
      loadData(roleId);
    }
  }, [visible, roleId, loadData, setRoleId]);

  const onSave = useCallback(async () => {
    const success = await handleSave();
    if (success && roleId) {
      loadData(roleId);
    }
  }, [handleSave, roleId, loadData]);

  const onDeleteTable = useCallback(
    async (tableModelId: string, id: string) => {
      const success = await handleDeleteTable(id);
      if (success) {
        removeTableFromList(tableModelId);
      }
    },
    [handleDeleteTable, removeTableFromList],
  );

  const onAddSuccess = useCallback(
    () => {
      if (roleId) {
        void loadData(roleId);
      }
    },
    [loadData, roleId],
  );

  const existingTableIds = new Set(tables.map((t) => t.tableModelId));

  return (
    <Modal
      title={`表模型权限配置 - ${roleName}`}
      open={visible}
      onCancel={onClose}
      width={1080}
      className={styles.modal}
      footer={null}
      destroyOnHidden
    >
      <Spin spinning={loading}>
        <div className={styles.container}>
          <TableListPanel
            tables={tables}
            selectedId={selectedTableId}
            onSelect={selectTable}
            onAdd={() => setAddModalVisible(true)}
            onDelete={onDeleteTable}
            onToggleEnabled={toggleEnabled}
          />
          <div className={styles.rightPanel}>
            <FieldConfigTable
              table={selectedTable}
              fieldRows={fieldRows}
              onUpdateRow={updateFieldRow}
            />
            <div className={styles.footer}>
              <Button onClick={handleReset}>重置</Button>
              <Button type="primary" loading={saving} onClick={onSave} data-ai-approval>
                保存
              </Button>
            </div>
          </div>
        </div>
      </Spin>

      <AddTableModelModal
        visible={addModalVisible}
        roleId={roleId}
        existingTableIds={existingTableIds}
        onClose={() => setAddModalVisible(false)}
        onSuccess={onAddSuccess}
      />
    </Modal>
  );
};

export default TableModelPermissionModal;

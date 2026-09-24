import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getTableModelPermission,
  saveOrUpdateRoleTableModel,
  deleteRoleTableModels,
} from '../../../services/role';
import type { RolePermissionTableModelVO, FieldConfigItem } from '../../../types';
import { columnToEditRow, editRowToFieldConfig } from '../types';
import type { FieldEditRow } from '../types';

/**
 * 角色表模型权限数据逻辑 hook
 */
export function useTableModelPermission() {
  const { message } = App.useApp();

  const [tables, setTables] = useState<RolePermissionTableModelVO[]>([]);
  const [selectedTableId, setSelectedTableId] = useState<string | null>(null);
  const [fieldRows, setFieldRows] = useState<FieldEditRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const initialRowsRef = useRef<FieldEditRow[]>([]);
  const roleIdRef = useRef<string | null>(null);

  const selectedTable = tables.find((t) => t.tableModelId === selectedTableId) ?? null;

  /** 设置当前角色 ID */
  const setRoleId = useCallback((id: string | null) => {
    roleIdRef.current = id;
  }, []);

  /** 加载表模型权限数据 */
  const loadData = useCallback(async (roleId: string) => {
    setLoading(true);
    try {
      const data = await getTableModelPermission(roleId);
      setTables(data);
      if (data.length > 0) {
        setSelectedTableId(data[0].tableModelId);
        const rows = data[0].columns.map(columnToEditRow);
        setFieldRows(rows);
        initialRowsRef.current = rows;
      } else {
        setSelectedTableId(null);
        setFieldRows([]);
        initialRowsRef.current = [];
      }
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, []);

  /** 选中某张表 */
  const selectTable = useCallback(
    (tableModelId: string) => {
      const table = tables.find((t) => t.tableModelId === tableModelId);
      if (!table) return;
      setSelectedTableId(tableModelId);
      const rows = table.columns.map(columnToEditRow);
      setFieldRows(rows);
      initialRowsRef.current = rows;
    },
    [tables],
  );

  /** 更新某个字段的配置 */
  const updateFieldRow = useCallback(
    (columnName: string, updates: Partial<FieldEditRow>) => {
      setFieldRows((prev) =>
        prev.map((row) =>
          row.columnName === columnName && !row.locked
            ? { ...row, ...updates }
            : row,
        ),
      );
    },
    [],
  );

  /** 保存当前表的字段配置 */
  const handleSave = useCallback(async () => {
    if (!selectedTable) return false;

    const fields: FieldConfigItem[] = [];
    for (const row of fieldRows) {
      const item = editRowToFieldConfig(row);
      if (item) fields.push(item);
    }

    setSaving(true);
    try {
      await saveOrUpdateRoleTableModel({
        id: selectedTable.id,
        roleId: roleIdRef.current ?? '',
        modulePrefix: selectedTable.modulePrefix,
        tableName: selectedTable.tableName,
        datasource: selectedTable.datasource,
        fields,
      });
      message.success('保存成功');
      initialRowsRef.current = [...fieldRows];
      return true;
    } catch {
      return false;
    } finally {
      setSaving(false);
    }
  }, [selectedTable, fieldRows, message]);

  /** 重置到初始数据 */
  const handleReset = useCallback(() => {
    setFieldRows([...initialRowsRef.current]);
  }, []);

  /** 切换接口关联表的启用/禁用状态 */
  const toggleEnabled = useCallback(
    async (tableModelId: string, enabled: boolean) => {
      const table = tables.find((t) => t.tableModelId === tableModelId);
      if (!table) return false;

      setSaving(true);
      try {
        await saveOrUpdateRoleTableModel({
          id: table.id,
          roleId: roleIdRef.current ?? '',
          modulePrefix: table.modulePrefix,
          tableName: table.tableName,
          datasource: table.datasource,
          enabled,
          fields: [],
        });
        setTables((prev) =>
          prev.map((t) =>
            t.tableModelId === tableModelId ? { ...t, enabled } : t,
          ),
        );
        message.success(enabled ? '已启用' : '已禁用');
        return true;
      } catch {
        return false;
      } finally {
        setSaving(false);
      }
    },
    [tables, message],
  );

  /** 删除某个表的权限 */
  const handleDeleteTable = useCallback(
    async (id: string) => {
      try {
        await deleteRoleTableModels([id]);
        message.success('删除成功');
        return true;
      } catch {
        return false;
      }
    },
    [message],
  );

  /** 添加新表到列表 */
  const addTablesToList = useCallback(
    (newTables: RolePermissionTableModelVO[]) => {
      if (newTables.length === 0) return;

      setTables((prev) => {
        const tableMap = new Map(prev.map((table) => [table.tableModelId, table]));
        newTables.forEach((table) => {
          tableMap.set(table.tableModelId, table);
        });
        return Array.from(tableMap.values());
      });

      if (!selectedTableId) {
        setSelectedTableId(newTables[0].tableModelId);
        const rows = newTables[0].columns.map(columnToEditRow);
        setFieldRows(rows);
        initialRowsRef.current = rows;
      }
    },
    [selectedTableId],
  );

  /** 从列表中移除表 */
  const removeTableFromList = useCallback(
    (tableModelId: string) => {
      setTables((prev) => prev.filter((t) => t.tableModelId !== tableModelId));
      if (selectedTableId === tableModelId) {
        const remaining = tables.filter((t) => t.tableModelId !== tableModelId);
        if (remaining.length > 0) {
          selectTable(remaining[0].tableModelId);
        } else {
          setSelectedTableId(null);
          setFieldRows([]);
          initialRowsRef.current = [];
        }
      }
    },
    [selectedTableId, tables, selectTable],
  );

  return {
    tables,
    selectedTableId,
    selectedTable,
    fieldRows,
    loading,
    saving,
    setRoleId,
    loadData,
    selectTable,
    updateFieldRow,
    handleSave,
    handleReset,
    handleDeleteTable,
    toggleEnabled,
    addTablesToList,
    removeTableFromList,
  };
}

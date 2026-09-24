import React, { useState, useEffect, useCallback } from 'react';
import { App, Input, Modal, Segmented, Spin, Table } from 'antd';
import type { TableProps } from 'antd';
import { getBusinessFunctionPage, getBusinessFunctionDetail } from '../../../tablemodel/services/businessFunction';
import { getTableModelPage } from '../../../tablemodel/services/tableModel';
import type { BusinessFunctionInfo, TableModelInfo } from '../../../tablemodel/types';
import { saveOrUpdateRoleTableModel } from '../../services/role';
import BusinessFunctionAddPanel from './BusinessFunctionAddPanel';
import styles from './addTableModelModal.module.less';

type AddMode = 'table' | 'businessFunction';

interface AddTableModelModalProps {
  visible: boolean;
  roleId: string | null;
  existingTableIds: Set<string>;
  onClose: () => void;
  onSuccess: () => void;
}

const tableColumns: TableProps<TableModelInfo>['columns'] = [
  { title: '表名', dataIndex: 'tableName', width: 160 },
  { title: '表注释', dataIndex: 'tableComment', ellipsis: true },
  { title: '模块', dataIndex: 'modulePrefix', width: 100 },
  { title: '数据源', dataIndex: 'dataSource', width: 100 },
];

const AddTableModelModal: React.FC<AddTableModelModalProps> = ({
  visible,
  roleId,
  existingTableIds,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [addMode, setAddMode] = useState<AddMode>('table');
  const [tableSearchText, setTableSearchText] = useState('');
  const [businessSearchText, setBusinessSearchText] = useState('');
  const [tableLoading, setTableLoading] = useState(false);
  const [businessLoading, setBusinessLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TableModelInfo[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [businessFunctions, setBusinessFunctions] = useState<BusinessFunctionInfo[]>([]);
  const [businessPageNum, setBusinessPageNum] = useState(1);
  const [businessPageSize] = useState(10);
  const [businessTotal, setBusinessTotal] = useState(0);
  const [selectedBusinessFunctionId, setSelectedBusinessFunctionId] = useState<string | null>(null);
  const [selectedBusinessFunctionName, setSelectedBusinessFunctionName] = useState('');
  const [previewTables, setPreviewTables] = useState<TableModelInfo[]>([]);
  const [totalTableCount, setTotalTableCount] = useState(0);
  const [skippedCount, setSkippedCount] = useState(0);

  const loadAvailableTables = useCallback(
    async (keyword = '') => {
      setTableLoading(true);
      try {
        const result = await getTableModelPage({
          tableName: keyword || undefined,
          pageNum: 1,
          pageSize: 100,
        });
        setDataSource((result?.records ?? []).filter((item) => !existingTableIds.has(item.id)));
      } catch {
        // request 层已自动提示
      } finally {
        setTableLoading(false);
      }
    },
    [existingTableIds],
  );

  const loadBusinessFunctions = useCallback(
    async (page = 1, name = '') => {
      setBusinessLoading(true);
      try {
        const result = await getBusinessFunctionPage({
          pageNum: page,
          pageSize: businessPageSize,
          name: name || undefined,
        });
        setBusinessFunctions(result.records ?? []);
        setBusinessTotal(result.total ?? 0);
        setBusinessPageNum(page);
      } catch {
        setBusinessFunctions([]);
        setBusinessTotal(0);
      } finally {
        setBusinessLoading(false);
      }
    },
    [businessPageSize],
  );

  const handleSelectBusinessFunction = useCallback(
    async (businessFunctionId: string) => {
      setSelectedBusinessFunctionId(businessFunctionId);
      setBusinessLoading(true);
      try {
        const detail = await getBusinessFunctionDetail(businessFunctionId);
        const allTables = detail.tables ?? [];
        const nextPreviewTables = allTables.filter((table) => !existingTableIds.has(table.id));
        setSelectedBusinessFunctionName(detail.name);
        setPreviewTables(nextPreviewTables);
        setTotalTableCount(allTables.length);
        setSkippedCount(allTables.length - nextPreviewTables.length);
      } catch {
        setSelectedBusinessFunctionName('');
        setPreviewTables([]);
        setTotalTableCount(0);
        setSkippedCount(0);
      } finally {
        setBusinessLoading(false);
      }
    },
    [existingTableIds],
  );

  useEffect(() => {
    if (!visible) {
      return;
    }

    setAddMode('table');
    setTableSearchText('');
    setBusinessSearchText('');
    setBusinessPageNum(1);
    setSelectedRowKeys([]);
    setSelectedBusinessFunctionId(null);
    setSelectedBusinessFunctionName('');
    setPreviewTables([]);
    setTotalTableCount(0);
    setSkippedCount(0);

    void loadAvailableTables('');
    void loadBusinessFunctions(1, '');
  }, [visible, loadAvailableTables, loadBusinessFunctions]);

  const canSubmit = addMode === 'table'
    ? selectedRowKeys.length > 0
    : !!selectedBusinessFunctionId && previewTables.length > 0;

  const handleBusinessSearch = useCallback(() => {
    setSelectedBusinessFunctionId(null);
    setSelectedBusinessFunctionName('');
    setPreviewTables([]);
    setTotalTableCount(0);
    setSkippedCount(0);
    void loadBusinessFunctions(1, businessSearchText);
  }, [businessSearchText, loadBusinessFunctions]);

  const handleBusinessPageChange = useCallback(
    (page: number) => {
      setSelectedBusinessFunctionId(null);
      setSelectedBusinessFunctionName('');
      setPreviewTables([]);
      setTotalTableCount(0);
      setSkippedCount(0);
      void loadBusinessFunctions(page, businessSearchText);
    },
    [businessSearchText, loadBusinessFunctions],
  );

  const handleConfirm = useCallback(async () => {
    if (!roleId || !canSubmit) {
      return;
    }

    const tablesToAdd = addMode === 'table'
      ? dataSource.filter((item) => selectedRowKeys.includes(item.id))
      : previewTables;

    setConfirmLoading(true);
    try {
      for (const table of tablesToAdd) {
        await saveOrUpdateRoleTableModel({
          roleId,
          modulePrefix: table.modulePrefix,
          tableName: table.tableName,
          datasource: table.dataSource,
          fields: [],
        });
      }

      if (addMode === 'table') {
        message.success(`成功添加 ${tablesToAdd.length} 个表模型权限`);
      } else {
        message.success(
          `已通过业务功能“${selectedBusinessFunctionName}”新增 ${tablesToAdd.length} 张表模型权限，跳过 ${skippedCount} 张已存在表`,
        );
      }
      onClose();
      onSuccess();
    } catch {
      // request 层已自动提示
    } finally {
      setConfirmLoading(false);
    }
  }, [
    roleId,
    canSubmit,
    addMode,
    dataSource,
    selectedRowKeys,
    previewTables,
    message,
    selectedBusinessFunctionName,
    skippedCount,
    onClose,
    onSuccess,
  ]);

  return (
    <Modal
      title="新增模型权限"
      open={visible}
      onCancel={onClose}
      onOk={handleConfirm}
      okText="确认添加"
      cancelText="取消"
      okButtonProps={{ disabled: !canSubmit, 'data-ai-approval': 'true' }}
      confirmLoading={confirmLoading}
      width={960}
      destroyOnHidden
    >
      <Segmented<AddMode>
        className={styles.modeSwitch}
        value={addMode}
        onChange={setAddMode}
        options={[
          { label: '按表模型添加', value: 'table' },
          { label: '按业务功能添加', value: 'businessFunction' },
        ]}
      />

      {addMode === 'table' ? (
        <>
          <Input.Search
            className={styles.tableSearch}
            placeholder="请输入表名搜索"
            allowClear
            value={tableSearchText}
            onChange={(e) => setTableSearchText(e.target.value)}
            onSearch={(value) => {
              void loadAvailableTables(value);
            }}
            aria-label="搜索表模型"
          />
          <Table<TableModelInfo>
            rowKey="id"
            columns={tableColumns}
            dataSource={dataSource}
            loading={tableLoading}
            size="small"
            scroll={{ y: 392 }}
            rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
            pagination={false}
          />
        </>
      ) : (
        <Spin spinning={businessLoading}>
          <BusinessFunctionAddPanel
            searchText={businessSearchText}
            businessFunctions={businessFunctions}
            selectedBusinessFunctionId={selectedBusinessFunctionId}
            selectedBusinessFunctionName={selectedBusinessFunctionName}
            previewTables={previewTables}
            totalTableCount={totalTableCount}
            skippedCount={skippedCount}
            currentPage={businessPageNum}
            pageSize={businessPageSize}
            total={businessTotal}
            onSearchChange={setBusinessSearchText}
            onSearch={handleBusinessSearch}
            onPageChange={handleBusinessPageChange}
            onSelectBusinessFunction={handleSelectBusinessFunction}
          />
        </Spin>
      )}
    </Modal>
  );
};

export default AddTableModelModal;

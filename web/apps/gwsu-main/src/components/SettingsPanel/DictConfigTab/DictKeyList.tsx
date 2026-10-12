import { useState, useEffect, useCallback } from 'react';
import { Button, Input, Space, Popconfirm, App, Pagination } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { getDictPage, deleteDicts } from '../services/dict';
import type { DictInfo, DictQuery } from '../services/dict';
import DictFormModal from './DictFormModal';
import styles from './index.module.less';

interface DictKeyListProps {
  onSelect: (dict: DictInfo) => void;
  selectedId?: string;
}

const DictKeyList: React.FC<DictKeyListProps> = ({ onSelect, selectedId }) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<DictInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [searchKey, setSearchKey] = useState('');

  const [formVisible, setFormVisible] = useState(false);
  const [editingDict, setEditingDict] = useState<DictInfo | null>(null);

  const fetchData = useCallback(
    async (pageNum = currentPage, pSize = pageSize, keyword = searchKey) => {
      setLoading(true);
      try {
        const query: DictQuery = { pageNum, pageSize: pSize };
        if (keyword) {
          query.dictKey = keyword;
          query.dictName = keyword;
        }
        const result = await getDictPage(query);
        if (result) {
          setDataSource(result.records);
          setTotal(result.total);
          setCurrentPage(pageNum);
          setPageSize(pSize);
        }
      } catch {
        // error handled by request util
      } finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize, searchKey],
  );

  useEffect(() => {
    fetchData(1);
  }, []);

  const handleAdd = () => {
    setEditingDict(null);
    setFormVisible(true);
  };

  const handleEdit = (record: DictInfo, e?: React.MouseEvent) => {
    e?.stopPropagation();
    setEditingDict(record);
    setFormVisible(true);
  };

  const handleDelete = async (id: string, e?: React.MouseEvent) => {
    e?.stopPropagation();
    const success = await deleteDicts([id]);
    if (success) {
      message.success('删除成功');
      fetchData(currentPage);
      if (selectedId === id) {
        onSelect(null as unknown as DictInfo);
      }
    }
  };

  const handleFormSuccess = () => {
    fetchData(currentPage);
    setFormVisible(false);
  };

  const handleSearch = () => {
    fetchData(1, pageSize, searchKey);
  };

  return (
    <div className={styles.leftPanel}>
      <div className={styles.panelHeader}>
        <span className={styles.panelTitle}>字典列表</span>
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={handleAdd}
          >
            新增
          </Button>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => fetchData(1)}
          />
        </Space>
      </div>
      <div
        style={{
          padding: '8px 12px',
          borderBottom: '1px solid var(--border-color, #f0f0f0)',
        }}
      >
        <Input.Search
          size="small"
          placeholder="搜索字典"
          value={searchKey}
          onChange={(e) => setSearchKey(e.target.value)}
          onSearch={handleSearch}
          allowClear
        />
      </div>
      <div className={styles.panelBody}>
        {loading ? (
          <div className={styles.emptyHint}>加载中...</div>
        ) : dataSource.length === 0 ? (
          <div className={styles.emptyHint}>暂无字典</div>
        ) : (
          dataSource.map((item) => (
            <div
              key={item.id}
              className={`${styles.dictItem} ${
                selectedId === item.id ? styles.active : ''
              }`}
              onClick={() => onSelect(item)}
            >
              <div className={styles.dictItemInfo}>
                <div className={styles.dictItemKey}>{item.dictKey}</div>
                <div className={styles.dictItemName}>{item.dictName}</div>
              </div>
              <div className={styles.dictItemActions}>
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={(e) => handleEdit(item, e)}
                />
                {item.dictType !== 1 && (
                  <Popconfirm
                    title="确定删除此字典？"
                    onConfirm={(e) =>
                      handleDelete(item.id!, e as React.MouseEvent)
                    }
                  >
                    <Button
                      type="link"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Popconfirm>
                )}
              </div>
            </div>
          ))
        )}
        <div className={styles.paginationWrap}>
          <Pagination
            size="small"
            current={currentPage}
            pageSize={pageSize}
            total={total}
            showSizeChanger={false}
            onChange={(page) => fetchData(page)}
          />
        </div>
      </div>
      <DictFormModal
        visible={formVisible}
        dict={editingDict}
        onClose={() => setFormVisible(false)}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
};

export default DictKeyList;

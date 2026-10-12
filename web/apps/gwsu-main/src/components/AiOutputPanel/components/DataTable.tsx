import { useEffect, useRef, useState } from 'react';
import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './DataTable.module.less';

interface ColumnDef {
  key: string;
  label: string;
  width?: string | null;
}

interface DataTableProps {
  title?: string | null;
  columns: ColumnDef[];
  data: Record<string, string | number | boolean | null>[];
  bordered?: boolean | null;
  striped?: boolean | null;
}

const DataTable: React.FC<BaseComponentProps<DataTableProps>> = ({ props }) => {
  const isBordered = props.bordered !== false;
  const isStriped = props.striped !== false;

  // 记录上次渲染时的行数，用于判断哪些是新增行
  const prevLengthRef = useRef(0);
  // 新增行的起始索引
  const [newRowFrom, setNewRowFrom] = useState(Infinity);

  const data = props.data ?? [];
  const columns = props.columns ?? [];
  const hasColumns = columns.length > 0;
  const hasData = data.length > 0;

  useEffect(() => {
    const prevLen = prevLengthRef.current;
    const curLen = data.length;

    if (curLen > prevLen) {
      // 有新行追加，标记新增行的起始索引
      setNewRowFrom(prevLen);
    }
    prevLengthRef.current = curLen;
  }, [data.length]);

  // columns 变化意味着新表格，重置
  useEffect(() => {
    prevLengthRef.current = 0;
    setNewRowFrom(Infinity);
  }, [columns]);

  return (
    <div className={styles.tableContainer}>
      {props.title && (
        <div className={styles.tableTitle}>
          <KnowledgeCitationText text={props.title} />
        </div>
      )}
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className={`${styles.th} ${
                  isBordered ? styles.borderedTd : ''
                }`}
                style={col.width ? { width: col.width } : undefined}
              >
                <KnowledgeCitationText text={col.label} />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {hasData ? (
            data.map((row, rowIdx) => (
              <tr
                key={rowIdx}
                className={`${
                  isStriped && rowIdx % 2 === 1 ? styles.stripedRow : ''
                } ${rowIdx >= newRowFrom ? styles.rowAnimated : ''}`}
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`${styles.td} ${
                      isBordered ? styles.borderedTd : ''
                    }`}
                  >
                    <KnowledgeCitationText text={String(row[col.key] ?? '')} />
                  </td>
                ))}
              </tr>
            ))
          ) : hasColumns ? (
            <tr className={styles.loadingRow}>
              <td colSpan={columns.length}>数据加载中...</td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  );
};

export default DataTable;

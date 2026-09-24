import type { FieldConfigItem, ColumnInfo } from '../../types';

/** 字段编辑行数据（合并 fixed + custom 后的展示数据） */
export interface FieldEditRow {
  columnName: string;
  columnComment: string;
  /** 是否被 fixedFieldConfig 锁定 */
  locked: boolean;
  /** 当前有效配置值（fixed 优先，否则取 custom，否则默认） */
  show: boolean;
  desensitize: boolean;
  strategy: string;
  prefixNoMaskLen?: number;
  suffixNoMaskLen?: number;
  symbol?: string;
}

/** 将 ColumnInfo 转换为 FieldEditRow */
export function columnToEditRow(column: ColumnInfo): FieldEditRow {
  const fixed = column.fixedFieldConfig;
  const custom = column.customFieldConfig;

  if (fixed) {
    return {
      columnName: column.columnName,
      columnComment: column.columnComment,
      locked: true,
      show: fixed.show,
      desensitize: fixed.desensitize,
      strategy: fixed.strategy,
      prefixNoMaskLen: fixed.prefixNoMaskLen,
      suffixNoMaskLen: fixed.suffixNoMaskLen,
      symbol: fixed.symbol,
    };
  }

  if (custom) {
    return {
      columnName: column.columnName,
      columnComment: column.columnComment,
      locked: false,
      show: custom.show,
      desensitize: custom.desensitize,
      strategy: custom.strategy,
      prefixNoMaskLen: custom.prefixNoMaskLen,
      suffixNoMaskLen: custom.suffixNoMaskLen,
      symbol: custom.symbol,
    };
  }

  return {
    columnName: column.columnName,
    columnComment: column.columnComment,
    locked: false,
    show: true,
    desensitize: false,
    strategy: 'NONE',
  };
}

/** 将编辑行转换回保存 DTO 的 FieldConfigItem（排除锁定字段） */
export function editRowToFieldConfig(row: FieldEditRow): FieldConfigItem | null {
  if (row.locked) return null;
  return {
    fieldName: row.columnName,
    show: row.show,
    desensitize: row.desensitize,
    strategy: row.strategy,
    prefixNoMaskLen: row.prefixNoMaskLen,
    suffixNoMaskLen: row.suffixNoMaskLen,
    symbol: row.symbol,
  };
}

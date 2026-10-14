/**
 * DOM 序列化器
 * 将 DomNode 树转为 LLM 可读的文本格式
 * 获取页面信息
 */

import type { DomNode } from './dom-builder';

/** 页面信息 */
export interface PageInfo {
  /** 当前页面 URL */
  url: string;
  /** 页面标题 */
  title: string;
  /** 窗口宽度 */
  windowWidth: number;
  /** 窗口高度 */
  windowHeight: number;
  /** 水平滚动位置 */
  scrollX: number;
  /** 垂直滚动位置 */
  scrollY: number;
}

/** 保留的语义标签 */
const SEMANTIC_TAGS = new Set(['nav', 'menu', 'header', 'footer', 'aside', 'dialog', 'form']);

/**
 * 将 DomNode 树序列化为 LLM 可读的文本
 *
 * 输出格式：
 * - 可交互元素：`[index]<tag attrs>text</tag>` 或 `[index]<tag attrs />`
 * - 不可交互的语义容器：`<nav>...</nav>`（保留结构上下文）
 * - 普通表格：Markdown Table；复杂表格：带列名的逐行格式
 * - 普通文本：直接输出文本行
 * - 缩进表示父子关系
 *
 * 示例：
 * ```
 * [0]<button type=submit>登录</button>
 * [1]<input type=text placeholder=请输入用户名/>
 *   [2]<label for=password>密码</label>
 * ```
 */
export function flatTreeToString(root: DomNode): string {
  const result: string[] = [];
  processNode(root, 0, result);
  return result.join('\n');
}

function processNode(node: DomNode, depth: number, result: string[]): void {
  const indent = '\t'.repeat(depth);

  if (node.type === 'text') {
    // 文本节点：只有在没有可交互父元素时才直接输出
    if (node.textContent) {
      result.push(`${indent}${node.textContent}`);
    }
    return;
  }

  // 元素节点
  if (!node.tagName) return;

  // 表格需要保留列与单元格之间的对应关系，不能按普通 DOM 文本扁平化。
  if (node.tagName === 'table') {
    serializeTable(node, result);
    return;
  }

  const isSemantic = SEMANTIC_TAGS.has(node.tagName);
  let nextDepth = depth;

  // 可交互元素：输出带索引和标签的格式
  if (node.isInteractive && node.highlightIndex !== undefined) {
    nextDepth = depth + 1;

    const indexStr = `[${node.highlightIndex}]`;
    const attrsStr = buildAttrsString(node.attributes);
    const uploadId = node.attributes?.['data-ai-upload-id'];
    const text = node.textContent?.trim() || (uploadId ? '上传控件' : '');

    let line = `${indent}${indexStr}<${node.tagName}`;
    if (attrsStr) line += ` ${attrsStr}`;

    if (text) {
      line += `>${text}</${node.tagName}>`;
    } else {
      line += ' />';
    }

    result.push(line);
  }

  // 语义标签（不可交互但保留结构上下文）
  const emitSemantic = isSemantic && node.highlightIndex === undefined;
  const mark = emitSemantic ? result.length : -1;

  if (emitSemantic) {
    result.push(`${indent}<${node.tagName}>`);
    nextDepth = depth + 1;
  }

  // 递归处理子节点
  if (node.children) {
    for (const child of node.children) {
      processNode(child, nextDepth, result);
    }
  }

  // 语义标签闭合
  if (emitSemantic) {
    // 空标签移除
    if (result.length === mark + 1) {
      result.pop();
    } else {
      result.push(`${indent}</${node.tagName}>`);
    }
  }
}

interface TableStructure {
  headerRows: DomNode[];
  bodyRows: DomNode[];
}

/**
 * 将普通表格序列化为 Markdown Table。
 * 合并单元格、多级表头等复杂结构改用逐行键值格式，避免列和值错位。
 */
function serializeTable(table: DomNode, result: string[]): void {
  const structure = extractTableStructure(table);

  if (structure.headerRows.length === 0 && structure.bodyRows.length === 0) {
    return;
  }

  if (isSimpleTable(structure)) {
    serializeMarkdownTable(structure, result);
    return;
  }

  serializeComplexTable(structure, result);
}

function extractTableStructure(table: DomNode): TableStructure {
  const headerRows: DomNode[] = [];
  const bodyRows: DomNode[] = [];

  for (const child of getElementChildren(table)) {
    if (child.tagName === 'thead') {
      headerRows.push(...getDirectRows(child));
    } else if (child.tagName === 'tbody' || child.tagName === 'tfoot') {
      bodyRows.push(...getDirectRows(child));
    } else if (child.tagName === 'tr') {
      bodyRows.push(child);
    }
  }

  // 兼容没有 thead、使用首行 th 作为表头的原生表格。
  if (headerRows.length === 0 && bodyRows.length > 0) {
    const firstRowCells = getRowCells(bodyRows[0]);
    if (
      firstRowCells.length > 0 &&
      firstRowCells.every((cell) => cell.tagName === 'th')
    ) {
      headerRows.push(bodyRows.shift()!);
    }
  }

  return { headerRows, bodyRows };
}

function getDirectRows(section: DomNode): DomNode[] {
  return getElementChildren(section).filter((node) => node.tagName === 'tr');
}

function getRowCells(row: DomNode): DomNode[] {
  return getElementChildren(row).filter(
    (node) => node.tagName === 'th' || node.tagName === 'td',
  );
}

function getElementChildren(node: DomNode): DomNode[] {
  return (node.children ?? []).filter((child) => child.type === 'element');
}

function isSimpleTable(structure: TableStructure): boolean {
  if (structure.headerRows.length !== 1) return false;

  const headerCells = getRowCells(structure.headerRows[0]);
  if (headerCells.length === 0 || headerCells.some(hasMergedCell)) return false;

  return structure.bodyRows.every((row) => {
    const cells = getRowCells(row);
    return cells.length === headerCells.length && !cells.some(hasMergedCell);
  });
}

function hasMergedCell(cell: DomNode): boolean {
  return getSpan(cell, 'colspan') > 1 || getSpan(cell, 'rowspan') > 1;
}

function getSpan(cell: DomNode, name: 'colspan' | 'rowspan'): number {
  const value = Number.parseInt(cell.attributes?.[name] ?? '1', 10);
  return Number.isFinite(value) && value > 0 ? value : 1;
}

function serializeMarkdownTable(
  structure: TableStructure,
  result: string[],
): void {
  const headerCells = getRowCells(structure.headerRows[0]);
  const headers = headerCells.map((cell, index) => getHeaderText(cell, index));
  const includeRowIndex = structure.bodyRows.some(
    (row) => row.isInteractive && row.highlightIndex !== undefined,
  );
  const outputHeaders = includeRowIndex ? ['AI行索引', ...headers] : headers;

  result.push('[表格]');
  result.push(`| ${outputHeaders.map(escapeMarkdownCell).join(' | ')} |`);
  result.push(`| ${outputHeaders.map(() => '---').join(' | ')} |`);

  for (const row of structure.bodyRows) {
    const values = getRowCells(row).map((cell) => renderCellContent(cell));
    if (includeRowIndex) {
      values.unshift(formatRowIndex(row));
    }
    result.push(`| ${values.map(escapeMarkdownCell).join(' | ')} |`);
  }
}

function serializeComplexTable(
  structure: TableStructure,
  result: string[],
): void {
  const candidateHeader = structure.headerRows[structure.headerRows.length - 1];
  const candidateHeaders = candidateHeader
    ? getRowCells(candidateHeader).map((cell, index) =>
        getHeaderText(cell, index),
      )
    : [];

  result.push('[复杂表格：逐行展示]');

  for (let rowIndex = 0; rowIndex < structure.bodyRows.length; rowIndex++) {
    const row = structure.bodyRows[rowIndex];
    const cells = getRowCells(row);
    const rowElementIndex = formatRowIndex(row);
    const suffix = rowElementIndex ? `，AI行索引=${rowElementIndex}` : '';
    result.push(`[行 ${rowIndex + 1}${suffix}]`);

    for (let cellIndex = 0; cellIndex < cells.length; cellIndex++) {
      const cell = cells[cellIndex];
      const columnName =
        candidateHeaders.length === cells.length
          ? candidateHeaders[cellIndex]
          : `第${cellIndex + 1}列`;
      const spanDescription = formatSpanDescription(cell);
      result.push(
        `\t${columnName}${spanDescription}: ${renderCellContent(cell) || '-'}`,
      );
    }
  }
}

function getHeaderText(cell: DomNode, index: number): string {
  return renderCellContent(cell) || `第${index + 1}列`;
}

function formatRowIndex(row: DomNode): string {
  if (!row.isInteractive || row.highlightIndex === undefined) return '';
  const tags = row.tags?.length ? `{${row.tags.join(',')}}` : '';
  return `[${row.highlightIndex}]${tags}`;
}

function formatSpanDescription(cell: DomNode): string {
  const spans: string[] = [];
  const colspan = getSpan(cell, 'colspan');
  const rowspan = getSpan(cell, 'rowspan');
  if (colspan > 1) spans.push(`跨${colspan}列`);
  if (rowspan > 1) spans.push(`跨${rowspan}行`);
  return spans.length > 0 ? `（${spans.join('，')}）` : '';
}

function renderCellContent(cell: DomNode): string {
  const parts: string[] = [];
  collectCellParts(cell, parts, true);
  return normalizeCellText(parts.join(' '));
}

function collectCellParts(
  node: DomNode,
  parts: string[],
  isCellRoot = false,
): void {
  if (node.type === 'text') {
    if (node.textContent) parts.push(node.textContent);
    return;
  }

  if (!isCellRoot && node.isInteractive && node.highlightIndex !== undefined) {
    parts.push(formatCompactInteractiveElement(node));
    return;
  }

  if (!isCellRoot && node.tagName === 'table') {
    parts.push('[嵌套表格]');
    return;
  }

  const partCountBeforeChildren = parts.length;
  for (const child of node.children ?? []) {
    collectCellParts(child, parts);
  }

  // 原子交互节点会短路构建子节点；普通空节点也可能只保留汇总文本。
  if (parts.length === partCountBeforeChildren && node.textContent) {
    parts.push(node.textContent);
  }
}

function formatCompactInteractiveElement(node: DomNode): string {
  const index = node.highlightIndex;
  const tags = node.tags?.length ? `{${node.tags.join(',')}}` : '';
  const label =
    node.textContent?.trim() ||
    node.attributes?.['aria-label'] ||
    node.attributes?.title ||
    node.attributes?.value ||
    node.attributes?.placeholder ||
    node.attributes?.icon ||
    node.tagName ||
    '元素';
  const states = getInteractiveStates(node);
  return `[${index}]${tags}${label}${states}`;
}

function getInteractiveStates(node: DomNode): string {
  const stateKeys = [
    'checked',
    'aria-checked',
    'aria-selected',
    'disabled',
    'aria-disabled',
  ];
  const states = stateKeys
    .filter((key) => node.attributes?.[key] !== undefined)
    .map((key) => `${key}=${node.attributes?.[key]}`);
  return states.length > 0 ? `(${states.join(',')})` : '';
}

function normalizeCellText(value: string): string {
  return value
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => line.replace(/[\t\f\v ]+/g, ' ').trim())
    .filter(Boolean)
    .join('<br>');
}

function escapeMarkdownCell(value: string): string {
  return (value || '-').replace(/\\/g, '\\\\').replace(/\|/g, '\\|');
}

/**
 * 构建属性字符串
 */
function buildAttrsString(attrs?: Record<string, string>): string {
  if (!attrs || Object.keys(attrs).length === 0) return '';

  // 去重：属性值超过5字符的重复值只保留一个
  const filtered = { ...attrs };
  const keys = Object.keys(filtered);
  const preservedKeys = new Set(['data-ai-upload-id']);
  if (keys.length > 1) {
    const seenValues: Record<string, string> = {};
    for (const key of keys) {
      if (preservedKeys.has(key)) {
        continue;
      }
      const value = filtered[key];
      if (value.length > 5) {
        if (value in seenValues) {
          delete filtered[key];
        } else {
          seenValues[value] = key;
        }
      }
    }
  }

  // 去掉与tagName重复的role
  // (在 dom-builder 中已处理，此处不再重复)

  // 移除与文本内容重复的属性
  // (简化处理，不在此处判断文本)

  return Object.entries(filtered)
    .sort(([leftKey], [rightKey]) => {
      if (leftKey === 'data-ai-upload-id') return -1;
      if (rightKey === 'data-ai-upload-id') return 1;
      return 0;
    })
    .map(([key, value]) => `${key}=${value}`)
    .join(' ');
}

/**
 * 获取当前页面信息
 */
export function getPageInfo(): PageInfo {
  return {
    url: window.location.href,
    title: document.title,
    windowWidth: window.innerWidth,
    windowHeight: window.innerHeight,
    scrollX: Math.round(window.scrollX || document.documentElement.scrollLeft || 0),
    scrollY: Math.round(window.scrollY || document.documentElement.scrollTop || 0),
  };
}

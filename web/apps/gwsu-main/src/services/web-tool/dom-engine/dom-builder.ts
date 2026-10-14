/**
 * DOM 树构建器
 * 从指定根元素遍历 DOM 树，判断元素是否可交互，构建简化的 DOM 节点树
 */

import { isElementVisible, shouldSkipTag, truncateText } from './dom-utils';
import { isExcluded } from './scope-resolver';
import type { SelectorMap } from './selector-map';

/** DOM 节点简化表示 */
export interface DomNode {
  /** 节点类型 */
  type: 'element' | 'text';
  /** 标签名（小写，仅element类型） */
  tagName?: string;
  /** 关键属性（仅element类型） */
  attributes?: Record<string, string>;
  /** 是否可交互（仅element类型） */
  isInteractive?: boolean;
  /** 可交互时分配的索引（仅element类型） */
  highlightIndex?: number;
  /** 元素标签列表，如 ['approval']（仅element类型，表示需要人工审批等语义） */
  tags?: string[];
  /** 文本内容（text类型为文本值，element类型为子元素文本汇总） */
  textContent?: string;
  /** 子节点 */
  children?: DomNode[];
  /** 原始DOM元素引用（仅内部使用，不序列化） */
  _element?: Element;
}

/** 需要保留的关键属性 */
const KEEP_ATTRS = new Set([
  'id',
  'type',
  'name',
  'value',
  'placeholder',
  'href',
  'title',
  'role',
  'aria-label',
  'aria-expanded',
  'aria-selected',
  'aria-checked',
  'aria-disabled',
  'aria-haspopup',
  'aria-controls',
  'disabled',
  'readonly',
  'required',
  'checked',
  'selected',
  'multiple',
  'for',
  'tabindex',
  'contenteditable',
  'data-date-format',
  'colspan',
  'rowspan',
  'alt',
  'target',
  'data-ai-approval',
  'data-ai-upload-id',
]);

/** HTML 布尔属性集合（值为空字符串时也需保留） */
const BOOLEAN_ATTRS = new Set([
  'disabled',
  'readonly',
  'required',
  'checked',
  'selected',
  'multiple',
]);

/** 可交互的标签名集合 */
const INTERACTIVE_TAGS = new Set([
  'a',
  'button',
  'input',
  'select',
  'textarea',
  'summary',
  'details',
  'option',
  'label',
]);

/** 可交互的 ARIA role 集合 */
const INTERACTIVE_ROLES = new Set([
  'button',
  'link',
  'tab',
  'menuitem',
  'option',
  'checkbox',
  'radio',
  'switch',
  'slider',
  'combobox',
  'searchbox',
  'spinbutton',
  'textbox',
  'treeitem',
]);

/** Ant Design 可交互组件的 class 标识 */
const ANTD_INTERACTIVE_CLASSES = [
  'ant-select',
  'ant-picker',
  'ant-switch',
  'ant-radio-wrapper',
  'ant-checkbox-wrapper',
  'ant-pagination-item',
  'ant-pagination-prev',
  'ant-pagination-next',
  'ant-tabs-tab',
  'ant-collapse-header',
  'ant-tree-treenode',
  'ant-table-row',
  'ant-dropdown-trigger',
];

/** 需要排除的遮罩/装饰性 class（不可交互但可能被误判） */
const EXCLUDED_MASK_CLASSES = [
  'ant-modal-mask',
  'ant-drawer-mask',
  'ant-mask',
  'ant-popover-mask',
];

/**
 * 原子交互标签集合
 * 这些标签是原子操作单元，其内部子元素属于同一操作，不应重复注册为独立交互元素。
 * label 排除：label 可能包裹 input，input 是独立交互元素。
 */
const ATOMIC_INTERACTIVE_TAGS = new Set([
  'button',
  'a',
  'input',
  'select',
  'textarea',
  'summary',
  'option',
]);

/** 最大遍历深度 */
const MAX_DEPTH = 30;

/** 文本截断长度 */
const TEXT_MAX_LENGTH = 200;

/**
 * 从元素及其子元素中提取 Ant Design 图标类名
 * 图标类名格式为 anticon-xxx，如 anticon-user、anticon-delete
 * 提取后返回图标名称（去掉 anticon- 前缀），多个图标用逗号分隔
 */
function extractIconName(element: Element): string | null {
  const iconNames: string[] = [];

  // 检查元素自身
  collectIconNames(element, iconNames);

  // 检查所有后代元素（图标可能嵌套多层，如 button > span.ant-btn-icon > span.anticon-xxx）
  const descendants = element.querySelectorAll('*');
  for (const descendant of descendants) {
    collectIconNames(descendant, iconNames);
  }

  return iconNames.length > 0 ? iconNames.join(',') : null;
}

function collectIconNames(element: Element, names: string[]): void {
  if (!element.classList) return;
  for (const cls of element.classList) {
    if (cls.startsWith('anticon-') && cls !== 'anticon') {
      const name = cls.replace('anticon-', '');
      if (name && !names.includes(name)) {
        names.push(name);
      }
    }
  }
}

/**
 * 检测元素的语义标签
 * 通过 data-ai-approval 属性标记需要人工审批的元素
 */
function detectElementTags(element: Element): string[] {
  const tags: string[] = [];

  if (element.hasAttribute('data-ai-approval')) {
    tags.push('approval');
  }
  if (element.hasAttribute('data-ai-upload-id')) {
    tags.push('upload');
  }

  return tags;
}

/**
 * 判断元素是否为遮罩/装饰性元素（应跳过）
 */
function isMaskElement(element: Element): boolean {
  if (!element.classList) return false;
  return EXCLUDED_MASK_CLASSES.some((cls) => element.classList.contains(cls));
}

/**
 * 判断元素是否可交互
 */
export function isInteractiveElement(element: Element): boolean {
  if (!(element instanceof HTMLElement)) return false;

  const tagName = element.tagName.toLowerCase();

  if (element.hasAttribute('data-ai-upload-id')) {
    return true;
  }

  // 1. 标签名判断
  if (INTERACTIVE_TAGS.has(tagName)) {
    // label 需要有 for 属性或包裹可交互元素才视为可交互
    if (tagName === 'label') {
      return !!element.getAttribute('for') || !!element.querySelector('input,select,textarea');
    }
    // option 只有在可见的下拉面板中才有意义
    if (tagName === 'option') {
      return true;
    }
    return true;
  }

  // 2. CSS cursor 判断
  const style = window.getComputedStyle(element);
  if (['pointer', 'move', 'text', 'grab'].includes(style.cursor)) {
    return true;
  }

  // 3. ARIA 角色判断
  const role = element.getAttribute('role');
  if (role && INTERACTIVE_ROLES.has(role)) {
    return true;
  }

  // 4. tabIndex 判断
  if (element.tabIndex >= 0) {
    return true;
  }

  // 5. contenteditable 判断
  if (element.isContentEditable) {
    return true;
  }

  // 6. ARIA 属性判断
  if (
    element.hasAttribute('aria-expanded') ||
    element.hasAttribute('aria-checked') ||
    element.hasAttribute('aria-selected')
  ) {
    return true;
  }

  // 7. Ant Design 组件标识判断
  const classList = element.classList;
  if (classList) {
    for (const cls of ANTD_INTERACTIVE_CLASSES) {
      if (classList.contains(cls)) {
        return true;
      }
    }
  }

  return false;
}

/**
 * Ant Design 组件语义增强
 * 为复选框、单选框、开关等组件补充语义属性，使 AI 能理解其交互语义
 */
function enhanceAntdComponentAttrs(element: Element, attrs: Record<string, string>): void {
  const classList = element.classList;
  if (!classList) return;

  // 复选框：ant-checkbox-wrapper
  if (classList.contains('ant-checkbox-wrapper')) {
    const input = element.querySelector('input[type="checkbox"]') as HTMLInputElement | null;
    if (input) {
      attrs['role'] = 'checkbox';
      attrs['checked'] = input.checked ? 'true' : 'false';
      if (input.indeterminate) {
        attrs['indeterminate'] = 'true';
      }
    }
    return;
  }

  // 单选框：ant-radio-wrapper
  if (classList.contains('ant-radio-wrapper')) {
    const input = element.querySelector('input[type="radio"]') as HTMLInputElement | null;
    if (input) {
      attrs['role'] = 'radio';
      attrs['checked'] = input.checked ? 'true' : 'false';
    }
    return;
  }

  // 开关：ant-switch
  if (classList.contains('ant-switch')) {
    attrs['role'] = 'switch';
    attrs['checked'] = classList.contains('ant-switch-checked') ? 'true' : 'false';
    if (classList.contains('ant-switch-disabled')) {
      attrs['disabled'] = 'true';
    }
    return;
  }
}

/**
 * 提取元素的关键属性
 */
function extractAttributes(
  element: Element,
  preserveFullValue = false,
): Record<string, string> {
  const attrs: Record<string, string> = {};
  const attributes = element.attributes;

  for (let i = 0; i < attributes.length; i++) {
    const attr = attributes[i];
    if (KEEP_ATTRS.has(attr.name)) {
      const value = attr.value.trim();
      if (value) {
        // 表格中的输入值也属于单元格数据，需要完整提供给 AI。
        attrs[attr.name] =
          preserveFullValue || value.length <= 30
            ? value
            : value.substring(0, 30) + '...';
      } else if (BOOLEAN_ATTRS.has(attr.name)) {
        // 布尔属性：HTML 中 disabled="" 等价于 disabled=true，值即使为空也保留
        attrs[attr.name] = 'true';
      }
    }
  }

  // 去掉与tagName重复的role
  if (attrs.role === element.tagName.toLowerCase()) {
    delete attrs.role;
  }

  // Ant Design 组件语义增强：为复选框、单选框、开关补充语义属性
  enhanceAntdComponentAttrs(element, attrs);

  // 提取 Ant Design 图标类名（anticon-xxx），作为 icon 属性保留
  const iconName = extractIconName(element);
  if (iconName) {
    attrs['icon'] = iconName;
  }

  return attrs;
}

/**
 * 获取元素下的可见文本（直到遇到下一个可交互子元素为止）
 */
/**
 * 获取元素下的可见文本
 * @param element 目标元素
 * @param ignoreChildInteractive 是否忽略子元素的可交互检查（原子交互标签短路时使用）
 */
function getElementText(element: Element, ignoreChildInteractive = false): string {
  const parts: string[] = [];
  collectText(element, parts, 0, true, ignoreChildInteractive);
  return parts.join(' ').trim();
}

function collectText(
  node: Element | Text, parts: string[], depth: number,
  isRoot = false, ignoreChildInteractive = false,
): void {
  if (depth > 3) return;

  if (node instanceof Text) {
    const text = node.textContent?.trim();
    if (text) parts.push(text);
    return;
  }

  // 如果是可交互子元素且不是根节点本身，停止递归
  // 忽略模式下（原子交互标签内部收集文本）跳过此检查，收集所有子元素文本
  if (!ignoreChildInteractive && !isRoot && isInteractiveElement(node) && depth > 0) {
    return;
  }

  for (const child of node.childNodes) {
    if (child instanceof Text) {
      const text = child.textContent?.trim();
      if (text) parts.push(text);
    } else if (child instanceof Element) {
      collectText(child, parts, depth + 1, false, ignoreChildInteractive);
    }
  }
}

/**
 * 构建 DOM 树
 * @param root 根元素
 * @param selectorMap 索引管理器
 * @param depth 当前深度
 * @param preserveTableContent 是否保留表格子树中的完整文本和属性值
 */
export function buildDomTree(
  root: Element,
  selectorMap: SelectorMap,
  depth = 0,
  preserveTableContent = false,
): DomNode {
  const shouldPreserveContent =
    preserveTableContent || root.tagName.toLowerCase() === 'table';
  const node: DomNode = {
    type: 'element',
    tagName: root.tagName.toLowerCase(),
    attributes: extractAttributes(root, shouldPreserveContent),
    children: [],
    _element: root,
  };

  // 判断可交互性
  if (depth > 0 && isInteractiveElement(root)) {
    node.isInteractive = true;
    node.highlightIndex = selectorMap.register(root);

    // 检测元素标签（如 data-ai-approval 审批标记）
    const tags = detectElementTags(root);
    if (tags.length > 0) {
      node.tags = tags;
    }

    // 原子交互标签短路：button/a/input 等是原子操作单元，
    // 内部子元素属于同一操作，不再递归注册为独立交互元素
    if (ATOMIC_INTERACTIVE_TAGS.has(root.tagName.toLowerCase())) {
      const text = getElementText(root, true);
      if (text) {
        node.textContent = shouldPreserveContent
          ? text
          : truncateText(text, TEXT_MAX_LENGTH);
      }
      return node;
    }
  }

  // 处理子节点
  const childNodes = root.childNodes;

  for (let i = 0; i < childNodes.length; i++) {
    const child = childNodes[i];

    if (child instanceof Text) {
      const text = child.textContent?.trim();
      if (text && node.children) {
        node.children.push({
          type: 'text',
          textContent: shouldPreserveContent
            ? text
            : truncateText(text, TEXT_MAX_LENGTH),
        });
      }
    } else if (child instanceof Element) {
      // 跳过不可见、排除区域、特殊标签、遮罩层
      if (
        !isElementVisible(child) ||
        isExcluded(child) ||
        shouldSkipTag(child.tagName.toLowerCase()) ||
        isMaskElement(child)
      ) {
        continue;
      }

      // SVG 内部跳过（保留svg标签本身）
      if (child.tagName.toLowerCase() === 'svg') {
        if (node.children) {
          const svgNode: DomNode = {
            type: 'element',
            tagName: 'svg',
            attributes: {},
          };
          const ariaLabel = child.getAttribute('aria-label');
          if (ariaLabel) {
            svgNode.attributes = { 'aria-label': ariaLabel };
          }
          node.children.push(svgNode);
        }
        continue;
      }

      // 深度限制
      if (depth >= MAX_DEPTH) continue;

      const childNode = buildDomTree(
        child,
        selectorMap,
        depth + 1,
        shouldPreserveContent,
      );
      // Tags 向下传播：如果子元素是交互元素但没有 tags，继承父元素的 tags
      if (
        childNode.isInteractive &&
        (!childNode.tags || childNode.tags.length === 0) &&
        node.tags &&
        node.tags.length > 0
      ) {
        childNode.tags = [...node.tags];
      }
      node.children?.push(childNode);
    }
  }

  // 获取元素文本
  const text = getElementText(root);
  if (text) {
    node.textContent = shouldPreserveContent
      ? text
      : truncateText(text, TEXT_MAX_LENGTH);
  }

  return node;
}

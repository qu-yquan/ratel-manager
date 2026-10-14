import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import {
  selectorMap,
  buildDomTree,
  type DomNode,
  flatTreeToString,
  getPageInfo,
  resolveScope,
  clearHighlights,
} from '../dom-engine';

function collectApprovalIndexes(node: DomNode, indexes: number[]): void {
  if (
    node.type === 'element'
    && node.highlightIndex !== undefined
    && node.tags?.includes('approval')
  ) {
    indexes.push(node.highlightIndex);
  }

  if (!node.children?.length) {
    return;
  }

  for (const child of node.children) {
    collectApprovalIndexes(child, indexes);
  }
}

/**
 * 获取界面状态工具
 * 获取当前Web界面的状态信息，返回页面中可见的交互元素列表和页面基本信息
 */
const getPageStateTool: WebToolExecutor = {
  async execute(): Promise<WebToolResult> {
    // 1. 重置索引映射
    selectorMap.reset();
    clearHighlights();

    // 2. 解析DOM范围
    const scope = resolveScope();
    const hasOperableContent = Boolean(scope.mainRoot || scope.modalRoots.length);

    // 3. 构建DOM树（主内容 + 弹框）
    let domText = '';
    const approval: number[] = [];
    if (scope.mainRoot) {
      const mainTree = buildDomTree(scope.mainRoot, selectorMap);
      collectApprovalIndexes(mainTree, approval);
      domText += flatTreeToString(mainTree);
    }

    for (const modalRoot of scope.modalRoots) {
      const modalTree = buildDomTree(modalRoot, selectorMap);
      collectApprovalIndexes(modalTree, approval);
      if (domText) domText += '\n';
      domText += '[弹框区域]\n' + flatTreeToString(modalTree);
    }

    // 4. 获取页面信息
    const pageInfo = getPageInfo();

    // 5. 组装结果
    const result = JSON.stringify({
      pageInfo: {
        url: pageInfo.url,
        title: pageInfo.title,
        窗口尺寸: `${pageInfo.windowWidth}x${pageInfo.windowHeight}`,
        滚动位置: `x=${pageInfo.scrollX}, y=${pageInfo.scrollY}`,
      },
      可交互元素数量: selectorMap.size,
      dom: domText.trim(),
      approval,
      hasOperableContent,
      ...(!hasOperableContent ? { message: '未找到可操作界面内容' } : {}),
    });

    return { success: true, result };
  },
};

// 注册工具
registerWebTool('GetPageState', getPageStateTool);

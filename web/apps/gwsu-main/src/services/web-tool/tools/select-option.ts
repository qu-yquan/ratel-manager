import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import {
  selectorMap,
  isElementVisible,
  sleep,
  waitForCondition,
} from '../dom-engine';

/**
 * 选择下拉框选项工具
 * 支持原生select元素和Ant Design Select组件
 */
const selectOptionTool: WebToolExecutor = {
  async execute(params): Promise<WebToolResult> {
    const index = Number(params.index);
    const text = String(params.text ?? '');
    if (isNaN(index)) {
      return { success: false, result: '参数index必须是数字' };
    }
    if (!text) {
      return { success: false, result: '参数text不能为空' };
    }

    const element = selectorMap.getElement(index);
    if (!element) {
      return {
        success: false,
        result: `未找到索引为 ${index} 的元素，请先调用GetPageState刷新界面状态`,
      };
    }

    if (!isElementVisible(element)) {
      return {
        success: false,
        result: `索引 ${index} 的元素已不可见，请重新调用GetPageState`,
      };
    }

    try {
      const isNativeSelect = element.tagName.toLowerCase() === 'select';

      if (isNativeSelect) {
        return await handleNativeSelect(element as HTMLSelectElement, text);
      }

      return await handleAntSelect(element, text);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `选择失败: ${message}` };
    }
  },
};

/**
 * 处理原生 select 元素
 */
async function handleNativeSelect(
  selectElement: HTMLSelectElement,
  optionText: string,
): Promise<WebToolResult> {
  const options = Array.from(selectElement.options);
  const option = options.find(
    (opt) => opt.textContent?.trim() === optionText || opt.value === optionText,
  );

  if (!option) {
    const availableOptions = options
      .map((o) => o.textContent?.trim())
      .join(', ');
    return {
      success: false,
      result: `未找到选项 "${optionText}"，可用选项: ${availableOptions}`,
    };
  }

  selectElement.value = option.value;
  selectElement.dispatchEvent(new Event('change', { bubbles: true }));
  await sleep(100);

  return { success: true, result: `已选择: ${optionText}` };
}

/**
 * 处理 Ant Design Select 组件
 * 策略：click 打开下拉面板 → 等待面板渲染 → click 选择选项
 */
async function handleAntSelect(
  element: Element,
  optionText: string,
): Promise<WebToolResult> {
  // 1. 点击 Select 组件打开下拉面板
  const trigger = element.classList.contains('ant-select')
    ? element.querySelector('.ant-select-selector') || element
    : element;

  if (trigger instanceof HTMLElement) {
    trigger.scrollIntoView({ behavior: 'instant', block: 'center' });
  }
  await sleep(100);

  trigger.dispatchEvent(
    new MouseEvent('mousedown', { bubbles: true, cancelable: true, button: 0 }),
  );
  trigger.dispatchEvent(
    new MouseEvent('mouseup', { bubbles: true, cancelable: true, button: 0 }),
  );
  trigger.dispatchEvent(
    new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }),
  );

  // 2. 等待下拉面板渲染
  const dropdownFound = await waitForCondition(
    () => {
      const dropdowns = document.querySelectorAll('.ant-select-dropdown');
      for (const d of dropdowns) {
        if (d instanceof HTMLElement && d.offsetParent !== null) {
          return true;
        }
      }
      return false;
    },
    { interval: 100, timeout: 2000 },
  );

  if (!dropdownFound) {
    return { success: false, result: '下拉面板未出现，可能不是下拉选择组件' };
  }

  await sleep(200);

  // 3. 在下拉面板中查找并点击目标选项
  const visibleDropdowns = Array.from(
    document.querySelectorAll('.ant-select-dropdown'),
  ).filter((d) => d instanceof HTMLElement && d.offsetParent !== null);

  for (const dropdown of visibleDropdowns) {
    const options = dropdown.querySelectorAll('.ant-select-item-option');
    for (const option of options) {
      const optionTextContent = option.textContent?.trim();
      if (
        optionTextContent === optionText ||
        optionTextContent?.includes(optionText)
      ) {
        option.dispatchEvent(
          new MouseEvent('mousedown', {
            bubbles: true,
            cancelable: true,
            button: 0,
          }),
        );
        option.dispatchEvent(
          new MouseEvent('mouseup', {
            bubbles: true,
            cancelable: true,
            button: 0,
          }),
        );
        option.dispatchEvent(
          new MouseEvent('click', {
            bubbles: true,
            cancelable: true,
            button: 0,
          }),
        );

        await sleep(300);
        return { success: true, result: `已选择: ${optionText}` };
      }
    }
  }

  // 收集可用选项信息
  const availableOptions: string[] = [];
  for (const dropdown of visibleDropdowns) {
    dropdown.querySelectorAll('.ant-select-item-option').forEach((opt) => {
      const t = opt.textContent?.trim();
      if (t) availableOptions.push(t);
    });
  }

  return {
    success: false,
    result: `下拉面板中未找到选项 "${optionText}"，可用选项: ${availableOptions.join(
      ', ',
    )}`,
  };
}

// 注册工具
registerWebTool('SelectOption', selectOptionTool);

import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import {
  selectorMap,
  isElementVisible,
  sleep,
  elementDescription,
} from '../dom-engine';

/**
 * 输入文本工具
 * 在指定索引的输入框中输入文本，兼容React受控组件和contenteditable
 */
const inputTextTool: WebToolExecutor = {
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
      const tagName = element.tagName.toLowerCase();

      // 先点击聚焦
      if (element instanceof HTMLElement) {
        element.scrollIntoView({ behavior: 'instant', block: 'center' });
      }
      await sleep(100);
      element.dispatchEvent(
        new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }),
      );
      await sleep(50);

      if (tagName === 'input' || tagName === 'textarea') {
        await handleNativeInput(
          element as HTMLInputElement | HTMLTextAreaElement,
          text,
        );
      } else if (element instanceof HTMLElement && element.isContentEditable) {
        await handleContentEditable(element, text);
      } else {
        return {
          success: false,
          result: `索引 ${index} 的元素 ${elementDescription(
            element,
          )} 不是输入框`,
        };
      }

      // 触发blur以激活表单验证
      await sleep(100);
      element.dispatchEvent(new FocusEvent('blur', { bubbles: true }));

      return {
        success: true,
        result: `已在索引 ${index} 的输入框中输入文本: "${text.substring(
          0,
          50,
        )}${text.length > 50 ? '...' : ''}"`,
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `输入失败: ${message}` };
    }
  },
};

/**
 * 处理原生 input/textarea 元素
 * 使用原生 value setter 绕过 React 受控组件拦截
 */
async function handleNativeInput(
  element: HTMLInputElement | HTMLTextAreaElement,
  text: string,
): Promise<void> {
  // 获取原型链上的原生 value setter
  const prototype =
    element.tagName.toLowerCase() === 'textarea'
      ? window.HTMLTextAreaElement.prototype
      : window.HTMLInputElement.prototype;
  const nativeSetter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set;

  if (!nativeSetter) {
    // 降级：直接设置value
    element.value = text;
  } else {
    // 先清空
    nativeSetter.call(element, '');
    element.dispatchEvent(new Event('input', { bubbles: true }));
    element.dispatchEvent(new Event('change', { bubbles: true }));

    // 设置新值
    nativeSetter.call(element, text);
  }

  // 触发事件确保框架感知值变化
  element.dispatchEvent(new Event('input', { bubbles: true }));
  element.dispatchEvent(new Event('change', { bubbles: true }));
}

/**
 * 处理 contenteditable 元素
 * Plan A: 合成事件; Plan B: execCommand 降级
 */
async function handleContentEditable(
  element: HTMLElement,
  text: string,
): Promise<void> {
  // Plan A: 合成 InputEvent
  const cleared =
    element.dispatchEvent(
      new InputEvent('beforeinput', {
        bubbles: true,
        cancelable: true,
        inputType: 'deleteContent',
      }),
    ) ?? true;

  if (cleared) {
    element.innerText = '';
    element.dispatchEvent(
      new InputEvent('input', { bubbles: true, inputType: 'deleteContent' }),
    );
  }

  const inserted =
    element.dispatchEvent(
      new InputEvent('beforeinput', {
        bubbles: true,
        cancelable: true,
        inputType: 'insertText',
        data: text,
      }),
    ) ?? true;

  if (inserted) {
    element.innerText = text;
    element.dispatchEvent(
      new InputEvent('input', {
        bubbles: true,
        inputType: 'insertText',
        data: text,
      }),
    );
  }

  // 验证 Plan A 是否成功
  if (element.innerText.trim() !== text.trim()) {
    // Plan B: execCommand 降级
    element.focus();
    const doc = element.ownerDocument;
    const selection = (doc.defaultView || window).getSelection();
    const range = doc.createRange();
    range.selectNodeContents(element);
    selection?.removeAllRanges();
    selection?.addRange(range);

    // eslint-disable-next-line @typescript-eslint/no-deprecated
    doc.execCommand('delete', false);
    // eslint-disable-next-line @typescript-eslint/no-deprecated
    doc.execCommand('insertText', false, text);
  }

  element.dispatchEvent(new Event('change', { bubbles: true }));
}

// 注册工具
registerWebTool('InputText', inputTextTool);

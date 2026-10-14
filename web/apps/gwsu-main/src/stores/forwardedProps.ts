/**
 * 传递到 AG-UI forwardedProps 的动态参数
 * 通过 Zustand 管理，路由/操作模式变化时自动同步到 CopilotKit
 */

import { create } from 'zustand';
import { aiRequestErrorPool } from '@gwsu/core';

/** 操作模式：人类操作 / AI操作 */
export type OperationMode = 'human' | 'ai';

interface ForwardedPropsState {
  /** 当前路由路径 */
  currentPath: string;
  /** 操作模式：human=人类操作, ai=AI操作，默认人类 */
  operationMode: OperationMode;
  /** 退出AI操作模式的原因（如用户手动终止），读取后由消费方清空 */
  exitReason: string;
  /**
   * 扩展属性
   */
  extras: Record<string, any>;
  /** 更新路由路径 */
  setCurrentPath: (path: string) => void;
  /** 设置操作模式 */
  setOperationMode: (mode: OperationMode) => void;
  /** 设置退出AI操作模式的原因 */
  setExitReason: (reason: string) => void;
  /** 清空退出原因 */
  clearExitReason: () => void;
  /** 更新扩展参数 */
  setExtras: (extras: Record<string, any>) => void;
}

export const useForwardedPropsStore = create<ForwardedPropsState>((set: any) => ({
  currentPath: '/',
  operationMode: 'human',
  exitReason: '',
  extras: {},
  setCurrentPath: (path: string) => set({ currentPath: path }),
  setOperationMode: (mode: OperationMode) => {
    aiRequestErrorPool.setEnabled(mode === 'ai');
    set({ operationMode: mode });
  },
  setExitReason: (reason: string) => set({ exitReason: reason }),
  clearExitReason: () => set({ exitReason: '' }),
  setExtras: (extras) => set({ extras }),
}));

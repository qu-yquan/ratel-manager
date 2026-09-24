import type { DeptTreeNode } from '../../types';

export interface DeptTreeSelectorProps {
  /** 部门树数据 */
  treeData: DeptTreeNode[];
  /** 加载状态 */
  loading?: boolean;
  /** 当前选中部门ID */
  selectedKey?: string | null;
  /** 各部门用户数量 */
  deptUserCount?: Record<string, number>;
  /** 是否显示用户数 */
  showUserCount?: boolean;
  /** 选中部门回调 */
  onSelect: (deptId: string) => void;
  /** 面板标题 */
  title?: string;
}

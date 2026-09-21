import type { MenuItem } from '../services/route';

/** 目录可能没有路由路径，使用 ID 生成稳定且唯一的展开 key。 */
export function getDirectoryMenuKey(menu: Pick<MenuItem, 'id'>): string {
  return `directory-${menu.id}`;
}

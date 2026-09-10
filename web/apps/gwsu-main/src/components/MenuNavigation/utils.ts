import { MenuPosition } from '@gwsu/core';
import type { MenuItem } from '@gwsu/core';

/** 搜索菜单，同时保留命中项的父级目录结构 */
export function filterMenus(menus: MenuItem[], keyword: string): MenuItem[] {
  const result: MenuItem[] = [];

  for (const menu of menus) {
    if (
      menu.menuType === 3 ||
      !menu.visible ||
      menu.position !== MenuPosition.SIDEBAR
    ) {
      continue;
    }

    const nameMatched = menu.menuName.toLowerCase().includes(keyword);
    const filteredChildren = menu.children
      ? filterMenus(menu.children, keyword)
      : [];

    if (nameMatched || filteredChildren.length > 0) {
      result.push({
        ...menu,
        children: nameMatched ? menu.children : filteredChildren,
      });
    }
  }

  return result;
}

/** 获取菜单树内的全部目录 key，供搜索结果一次性展开 */
export function getAllMenuKeys(menus: MenuItem[]): string[] {
  const keys: string[] = [];

  for (const menu of menus) {
    if (menu.menuType === 1 && menu.children?.length) {
      keys.push(menu.path);
      keys.push(...getAllMenuKeys(menu.children));
    }
  }

  return keys;
}

/** 获取目标目录同层级的其他目录 key，用于手风琴展开 */
export function getSiblingSubmenuKeys(
  menus: MenuItem[],
  targetKey: string,
): string[] {
  const submenuKeys = menus
    .filter((menu) => menu.menuType === 1 && menu.children?.length)
    .map((menu) => menu.path);

  if (submenuKeys.includes(targetKey)) {
    return submenuKeys.filter((key) => key !== targetKey);
  }

  for (const menu of menus) {
    if (menu.children?.length) {
      const result = getSiblingSubmenuKeys(menu.children, targetKey);
      if (result.length > 0) return result;
    }
  }

  return [];
}

import { SearchOutlined } from '@ant-design/icons';
import { Input, Menu, Spin } from 'antd';
import type { MenuProps } from 'antd';
import {
  findMenuByPath,
  findOpenKeys,
  transformToMenuItems,
  useMenuStore,
} from '@gwsu/core';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { history, useLocation } from 'umi';
import {
  filterMenus,
  getAllMenuKeys,
  getSiblingSubmenuKeys,
} from '@/components/MenuNavigation/utils';
import { useOperationTabStore } from '@/stores/operationTab';
import styles from './index.module.less';

/**
 * AI 助手拖拽或关闭时展示的常驻导航。
 * 组件位于 AI 读取区域外，避免菜单内容进入页面上下文。
 */
const SidebarNavigation: React.FC = () => {
  const location = useLocation();
  const menus = useMenuStore((state) => state.menus);
  const loading = useMenuStore((state) => state.loading);
  const switchToInterface = useOperationTabStore(
    (state) => state.switchToInterface,
  );
  const [searchValue, setSearchValue] = useState('');

  const currentMenu = useMemo(
    () => findMenuByPath(menus, location.pathname),
    [location.pathname, menus],
  );
  const selectedKeys = currentMenu ? [currentMenu.path] : [];
  const selectedOpenKeys = useMemo(
    () => findOpenKeys(menus, location.pathname),
    [location.pathname, menus],
  );
  const [openKeys, setOpenKeys] = useState<string[]>(selectedOpenKeys);

  const normalizedKeyword = searchValue.trim().toLowerCase();
  const filteredMenus = useMemo(
    () => (normalizedKeyword ? filterMenus(menus, normalizedKeyword) : menus),
    [menus, normalizedKeyword],
  );
  const menuItems = useMemo(
    () => transformToMenuItems(filteredMenus),
    [filteredMenus],
  );
  const displayedOpenKeys = normalizedKeyword
    ? getAllMenuKeys(filteredMenus)
    : openKeys;

  useEffect(() => {
    setOpenKeys(selectedOpenKeys);
  }, [selectedOpenKeys]);

  const handleOpenKeysChange: MenuProps['onOpenChange'] = useCallback(
    (keys) => {
      if (normalizedKeyword) {
        setOpenKeys(keys);
        return;
      }

      const lastOpenedKey = keys.find((key) => !openKeys.includes(key));
      if (!lastOpenedKey) {
        setOpenKeys(keys);
        return;
      }

      const siblingKeys = getSiblingSubmenuKeys(menus, lastOpenedKey);
      setOpenKeys(keys.filter((key) => !siblingKeys.includes(key)));
    },
    [menus, normalizedKeyword, openKeys],
  );

  const handleMenuClick: MenuProps['onClick'] = useCallback(
    ({ key }) => {
      switchToInterface();
      history.push(key);
    },
    [switchToInterface],
  );

  const handleSearchChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setSearchValue(event.target.value);
    },
    [],
  );

  return (
    <aside
      className={styles.sidebar}
      aria-label="主导航"
      data-ai-exclude="true"
    >
      <div className={styles.searchWrapper}>
        <Input
          aria-label="搜索菜单"
          prefix={<SearchOutlined aria-hidden="true" />}
          placeholder="搜索菜单"
          value={searchValue}
          onChange={handleSearchChange}
          allowClear
          className={styles.searchInput}
        />
      </div>

      <nav className={styles.menuWrapper} aria-label="功能菜单">
        {loading ? (
          <div className={styles.loading} role="status" aria-label="菜单加载中">
            <Spin size="small" />
          </div>
        ) : menuItems.length > 0 ? (
          <Menu
            mode="inline"
            selectedKeys={selectedKeys}
            openKeys={displayedOpenKeys}
            onOpenChange={handleOpenKeysChange}
            items={menuItems}
            onClick={handleMenuClick}
            className={styles.menu}
            inlineIndent={18}
          />
        ) : (
          <div className={styles.emptyText}>无匹配菜单</div>
        )}
      </nav>
    </aside>
  );
};

export default SidebarNavigation;

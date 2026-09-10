import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Menu, Popover } from 'antd';
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
import styles from './index.module.less';

interface RouteSelectorProps {
  /** Tab 是否激活，非激活时隐藏箭头且不可弹出菜单 */
  isActive?: boolean;
  /** 下拉导航用于 AI 固定模式，纯标签用于已有侧栏的模式 */
  navigationMode?: 'dropdown' | 'label';
}

/**
 * 路由选择器组件
 * 显示当前路由名称，点击展开菜单树供导航
 * 替代传统左侧菜单栏，节省空间
 */
const RouteSelector: React.FC<RouteSelectorProps> = ({
  isActive = true,
  navigationMode = 'dropdown',
}) => {
  const location = useLocation();
  const { menus } = useMenuStore();
  const [open, setOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');

  // 当前路由名称：从所有菜单中查找，找不到则不显示名称
  const currentMenu = useMemo(
    () => findMenuByPath(menus, location.pathname),
    [menus, location.pathname],
  );
  // 仅精确匹配时显示菜单名称，前缀匹配（非菜单路由）显示 '界面'
  const currentLabel =
    currentMenu?.path === location.pathname ? currentMenu.menuName : '';

  // 搜索过滤菜单
  const filteredMenus = useMemo(() => {
    if (!searchValue.trim()) return menus;
    return filterMenus(menus, searchValue.trim().toLowerCase());
  }, [menus, searchValue]);

  // 转换为 Ant Design Menu items
  const menuItems = useMemo(
    () => (filteredMenus.length > 0 ? transformToMenuItems(filteredMenus) : []),
    [filteredMenus],
  );

  // 选中项所在的目录 keys（用于初始展开和打开时自动展开）
  const selectedOpenKeys = useMemo(
    () => findOpenKeys(menus, location.pathname),
    [menus, location.pathname],
  );

  // 受控展开的目录 keys（手风琴模式：只展开一个）
  const [openKeys, setOpenKeys] = useState<string[]>(selectedOpenKeys);

  // 弹框打开时，路由变化自动同步展开目录
  useEffect(() => {
    if (open) {
      setOpenKeys(selectedOpenKeys);
    }
  }, [selectedOpenKeys, open]);

  // 切换到侧栏导航后关闭顶部弹层，避免恢复固定模式时意外重新打开
  useEffect(() => {
    if (navigationMode === 'label') {
      setOpen(false);
      setSearchValue('');
    }
  }, [navigationMode]);

  // 菜单弹出时，自动展开选中项所在目录
  const handleOpenChange = useCallback(
    (newOpen: boolean) => {
      if (!isActive) return;
      setOpen(newOpen);
      if (newOpen) {
        // 打开时展开选中项所在目录
        setOpenKeys(selectedOpenKeys);
      } else {
        setSearchValue('');
      }
    },
    [isActive, selectedOpenKeys],
  );

  // 目录展开/收起回调 - 手风琴模式：同层级互斥，只允许一个目录展开
  const handleOpenKeysChange: MenuProps['onOpenChange'] = useCallback(
    (keys: string[]) => {
      // 搜索模式下允许全部展开
      if (searchValue) {
        setOpenKeys(keys);
        return;
      }
      // 找到新展开的 key
      const lastKey = keys.find((k) => !openKeys.includes(k));
      if (!lastKey) {
        // 仅收起操作
        setOpenKeys(keys);
        return;
      }
      // 手风琴：关闭同层级其他目录
      const siblingKeys = getSiblingSubmenuKeys(menus, lastKey);
      setOpenKeys(keys.filter((k) => !siblingKeys.includes(k)));
    },
    [menus, openKeys, searchValue],
  );

  // 菜单点击
  const handleMenuClick: MenuProps['onClick'] = useCallback(({ key }) => {
    history.push(key);
    setOpen(false);
    setSearchValue('');
  }, []);

  // 搜索输入
  const handleSearchChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const value = e.target.value;
      setSearchValue(value);
      if (value) {
        const searchedMenus = filterMenus(menus, value.trim().toLowerCase());
        setOpenKeys(getAllMenuKeys(searchedMenus));
      } else {
        setOpenKeys(selectedOpenKeys);
      }
    },
    [menus, selectedOpenKeys],
  );

  // 弹出内容
  const dropdownContent = (
    <div className={styles.dropdownContent}>
      <div className={styles.searchWrapper}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索菜单..."
          value={searchValue}
          onChange={handleSearchChange}
          variant="borderless"
          allowClear
          className={styles.searchInput}
        />
      </div>
      <div className={styles.menuWrapper}>
        {menuItems.length > 0 ? (
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            openKeys={searchValue ? getAllMenuKeys(filteredMenus) : openKeys}
            onOpenChange={handleOpenKeysChange}
            items={menuItems}
            onClick={handleMenuClick}
            className={styles.menu}
            inlineIndent={16}
          />
        ) : (
          <div className={styles.emptyText}>无匹配结果</div>
        )}
      </div>
    </div>
  );

  // 非激活态：仅显示标签，不渲染 Popover（防止误触弹出菜单）
  if (!isActive || navigationMode === 'label') {
    return (
      <div className={styles.selector}>
        <span className={styles.selectorLabel}>
          {navigationMode === 'label' ? '界面' : currentLabel || '界面'}
        </span>
      </div>
    );
  }

  return (
    <Popover
      open={open}
      onOpenChange={handleOpenChange}
      content={dropdownContent}
      trigger="click"
      placement="bottomLeft"
      classNames={{
        root: styles.popover,
      }}
      destroyOnHidden
    >
      <div className={styles.selector}>
        <span className={styles.selectorLabel}>{currentLabel || '界面'}</span>
        <DownOutlined
          aria-hidden="true"
          className={`${styles.selectorArrow} ${
            open ? styles.selectorArrowOpen : ''
          }`}
        />
      </div>
    </Popover>
  );
};

export default RouteSelector;

/**
 * 菜单转换工具
 */

import React from 'react';
import {
  DashboardOutlined,
  SettingOutlined,
  SafetyOutlined,
  UserOutlined,
  MenuOutlined,
  AppstoreOutlined,
  HomeOutlined,
  FileOutlined,
  TeamOutlined,
  LockOutlined,
  ApartmentOutlined,
  KeyOutlined,
  ProfileOutlined,
  TableOutlined,
  BarChartOutlined,
  CloudOutlined,
  ShoppingCartOutlined,
  ShopOutlined,
  DatabaseOutlined,
  CodeOutlined,
  ToolOutlined,
  ApiOutlined,
  BellOutlined,
  FormOutlined,
  CalendarOutlined,
  ContactsOutlined,
  ClusterOutlined,
  DesktopOutlined,
  ExperimentOutlined,
  InsuranceOutlined,
  AuditOutlined,
  SolutionOutlined,
  FlagOutlined,
  GlobalOutlined,
  LaptopOutlined,
  MobileOutlined,
  NotificationOutlined,
  PictureOutlined,
  PrinterOutlined,
  RocketOutlined,
  SoundOutlined,
  StarOutlined,
  SwitcherOutlined,
  ThunderboltOutlined,
  TrophyOutlined,
  UngroupOutlined,
  WarningOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { MenuItem } from '../services/route';
import { MenuPosition, MenuRoute } from '../types/menu';
import { getDirectoryMenuKey } from './menuKey';

/**
 * 图标映射表（短名映射）
 * 同时支持短名（如 user）和完整名（如 UserOutlined、TeamOutlined），
 * getIconComponent 会自动去除 Outlined/Filled/TwoTone 后缀后再查找
 */
const iconMap: Record<string, React.FC> = {
  dashboard: DashboardOutlined,
  setting: SettingOutlined,
  security: SafetyOutlined,
  user: UserOutlined,
  menu: MenuOutlined,
  appstore: AppstoreOutlined,
  home: HomeOutlined,
  file: FileOutlined,
  team: TeamOutlined,
  lock: LockOutlined,
  apartment: ApartmentOutlined,
  key: KeyOutlined,
  profile: ProfileOutlined,
  table: TableOutlined,
  barchart: BarChartOutlined,
  cloud: CloudOutlined,
  shoppingcart: ShoppingCartOutlined,
  shop: ShopOutlined,
  database: DatabaseOutlined,
  code: CodeOutlined,
  tool: ToolOutlined,
  api: ApiOutlined,
  bell: BellOutlined,
  form: FormOutlined,
  calendar: CalendarOutlined,
  contacts: ContactsOutlined,
  cluster: ClusterOutlined,
  desktop: DesktopOutlined,
  experiment: ExperimentOutlined,
  insurance: InsuranceOutlined,
  audit: AuditOutlined,
  solution: SolutionOutlined,
  flag: FlagOutlined,
  global: GlobalOutlined,
  laptop: LaptopOutlined,
  mobile: MobileOutlined,
  notification: NotificationOutlined,
  picture: PictureOutlined,
  printer: PrinterOutlined,
  rocket: RocketOutlined,
  sound: SoundOutlined,
  star: StarOutlined,
  switcher: SwitcherOutlined,
  thunderbolt: ThunderboltOutlined,
  trophy: TrophyOutlined,
  ungroup: UngroupOutlined,
  warning: WarningOutlined,
  wallet: WalletOutlined,
};

/**
 * 获取图标组件
 * 支持短名（如 user、apartment）和完整名（如 UserOutlined、TeamOutlined）两种格式
 * 完整名会自动去除 Outlined/Filled/TwoTone 后缀后再查找映射表
 */
export function getIconComponent(iconName: string): React.ReactNode {
  if (!iconName) return undefined;
  // 去除首尾空格后转小写
  const normalized = iconName.trim().toLowerCase();
  // 先尝试直接查找
  const directMatch = iconMap[normalized];
  if (directMatch) return React.createElement(directMatch);
  // 去除 Outlined/Filled/TwoTone 后缀再查找
  const stripped = normalized.replace(/outlined$|filled$|twotone$/, '');
  const suffixMatch = iconMap[stripped];
  return suffixMatch ? React.createElement(suffixMatch) : undefined;
}

/**
 * 将后端菜单数据转换为 Ant Design Menu items
 * 目录类型（menuType=1）且有子菜单时渲染为 SubMenu
 * 菜单类型（menuType=2）始终渲染为 MenuItem，不受 children 影响
 */
export function transformToMenuItems(menus: MenuItem[]): MenuRoute[] {
  return menus
    .filter((m) => m.menuType !== 3 && m.visible && m.position === MenuPosition.SIDEBAR) // 过滤按钮类型、隐藏菜单和非侧边栏菜单
    .sort((a, b) => a.sort - b.sort)
    .map((menu) => {
      // 目录类型（menuType=1）且有子菜单时渲染为 SubMenu
      // 菜单类型（menuType=2）始终渲染为 MenuItem，不受 children 影响
      const shouldRenderAsSubMenu = menu.menuType === 1 && !!menu.children?.length;

      if (shouldRenderAsSubMenu) {
        return {
          key: getDirectoryMenuKey(menu),
          icon: getIconComponent(menu.icon),
          label: menu.menuName,
          'data-micro-app': menu.microApp,
          children: transformToMenuItems(menu.children || []),
        } as MenuRoute;
      }

      return {
        key: menu.path,
        icon: getIconComponent(menu.icon),
        label: menu.menuName,
        'data-micro-app': menu.microApp,
      } as MenuRoute;
    });
}

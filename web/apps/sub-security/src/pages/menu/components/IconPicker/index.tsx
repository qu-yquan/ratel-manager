import React, { useState, useMemo } from 'react';
import { Popover, Input } from 'antd';
import * as Icons from '@ant-design/icons';
import styles from './index.module.less';

type IconComponent = typeof Icons.MenuOutlined;

/** 常用图标列表（名称 + 组件） */
const ICON_LIST: Array<{ name: string; component: IconComponent }> = [
  { name: 'MenuOutlined', component: Icons.MenuOutlined },
  { name: 'MenuFoldOutlined', component: Icons.MenuFoldOutlined },
  { name: 'MenuUnfoldOutlined', component: Icons.MenuUnfoldOutlined },
  { name: 'DashboardOutlined', component: Icons.DashboardOutlined },
  { name: 'HomeOutlined', component: Icons.HomeOutlined },
  { name: 'SettingOutlined', component: Icons.SettingOutlined },
  { name: 'ToolOutlined', component: Icons.ToolOutlined },
  { name: 'UserOutlined', component: Icons.UserOutlined },
  { name: 'TeamOutlined', component: Icons.TeamOutlined },
  { name: 'SafetyOutlined', component: Icons.SafetyOutlined },
  { name: 'SecurityScanOutlined', component: Icons.SecurityScanOutlined },
  { name: 'LockOutlined', component: Icons.LockOutlined },
  { name: 'UnlockOutlined', component: Icons.UnlockOutlined },
  { name: 'KeyOutlined', component: Icons.KeyOutlined },
  { name: 'FileOutlined', component: Icons.FileOutlined },
  { name: 'FileTextOutlined', component: Icons.FileTextOutlined },
  { name: 'FileProtectOutlined', component: Icons.FileProtectOutlined },
  { name: 'FolderOutlined', component: Icons.FolderOutlined },
  { name: 'FolderOpenOutlined', component: Icons.FolderOpenOutlined },
  { name: 'DatabaseOutlined', component: Icons.DatabaseOutlined },
  { name: 'TableOutlined', component: Icons.TableOutlined },
  { name: 'BarChartOutlined', component: Icons.BarChartOutlined },
  { name: 'PieChartOutlined', component: Icons.PieChartOutlined },
  { name: 'LineChartOutlined', component: Icons.LineChartOutlined },
  { name: 'AreaChartOutlined', component: Icons.AreaChartOutlined },
  { name: 'AppstoreOutlined', component: Icons.AppstoreOutlined },
  { name: 'BuildOutlined', component: Icons.BuildOutlined },
  { name: 'CodeOutlined', component: Icons.CodeOutlined },
  { name: 'ApiOutlined', component: Icons.ApiOutlined },
  { name: 'CloudOutlined', component: Icons.CloudOutlined },
  { name: 'CloudServerOutlined', component: Icons.CloudServerOutlined },
  { name: 'GlobalOutlined', component: Icons.GlobalOutlined },
  { name: 'NotificationOutlined', component: Icons.NotificationOutlined },
  { name: 'BellOutlined', component: Icons.BellOutlined },
  { name: 'MailOutlined', component: Icons.MailOutlined },
  { name: 'MessageOutlined', component: Icons.MessageOutlined },
  { name: 'CalendarOutlined', component: Icons.CalendarOutlined },
  { name: 'ScheduleOutlined', component: Icons.ScheduleOutlined },
  { name: 'FlagOutlined', component: Icons.FlagOutlined },
  { name: 'TagOutlined', component: Icons.TagOutlined },
  { name: 'TagsOutlined', component: Icons.TagsOutlined },
  { name: 'ShopOutlined', component: Icons.ShopOutlined },
  { name: 'ShoppingOutlined', component: Icons.ShoppingOutlined },
  { name: 'ContainerOutlined', component: Icons.ContainerOutlined },
  { name: 'ControlOutlined', component: Icons.ControlOutlined },
  { name: 'MonitorOutlined', component: Icons.MonitorOutlined },
  { name: 'LaptopOutlined', component: Icons.LaptopOutlined },
  { name: 'MobileOutlined', component: Icons.MobileOutlined },
  { name: 'CameraOutlined', component: Icons.CameraOutlined },
];
const ICON_MAP = new Map<string, IconComponent>(
  ICON_LIST.map((item) => [item.name, item.component]),
);

interface IconPickerProps {
  value?: string;
  onChange?: (value: string) => void;
}

const IconPicker: React.FC<IconPickerProps> = ({ value, onChange }) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');

  const filteredIcons = useMemo(() => {
    if (!search) return ICON_LIST;
    const keyword = search.toLowerCase();
    return ICON_LIST.filter((item) => item.name.toLowerCase().includes(keyword));
  }, [search]);

  const SelectedIcon = value ? ICON_MAP.get(value) : undefined;

  const handleSelect = (name: string) => {
    onChange?.(name);
    setOpen(false);
    setSearch('');
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange?.('');
  };

  const content = (
    <div className={styles.pickerContent}>
      <Input
        placeholder="搜索图标"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        allowClear
        className={styles.searchInput}
      />
      <div className={styles.iconGrid}>
        {filteredIcons.map((item) => {
          const IconComp = item.component;
          return (
            <button
              type="button"
              key={item.name}
              className={`${styles.iconItem} ${value === item.name ? styles.iconItemActive : ''}`}
              title={item.name}
              aria-label={`选择图标 ${item.name}`}
              onClick={() => handleSelect(item.name)}
            >
              <IconComp className={styles.gridIcon} aria-hidden="true" />
            </button>
          );
        })}
        {filteredIcons.length === 0 && (
          <div className={styles.empty}>无匹配图标</div>
        )}
      </div>
    </div>
  );

  return (
    <Popover
      open={open}
      onOpenChange={setOpen}
      trigger="click"
      placement="bottomLeft"
      content={content}
      overlayStyle={{ width: 320 }}
    >
      <div className={styles.trigger}>
        {SelectedIcon ? (
          <>
            <SelectedIcon className={styles.selectedIcon} aria-hidden="true" />
            <span className={styles.iconName}>{value}</span>
          </>
        ) : (
          <span className={styles.placeholder}>请选择图标</span>
        )}
        {value && (
          <Icons.CloseCircleOutlined className={styles.clearIcon} onClick={handleClear} />
        )}
      </div>
    </Popover>
  );
};

export default IconPicker;

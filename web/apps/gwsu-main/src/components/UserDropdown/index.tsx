import { LogoutOutlined, SettingOutlined } from '@ant-design/icons';
import { App, Avatar, Dropdown, Tooltip } from 'antd';
import { useAgent } from '@copilotkit/react-core/v2';
import {
  useUserStore,
  useMenuStore,
  useHeadlessStore,
  MenuPosition,
  getIconComponent,
  EventType,
  emitEvent,
} from '@gwsu/core';
import type { MenuItem } from '@gwsu/core';
import { history } from 'umi';
import { logout } from '@/services/auth';
import { useOperationTabStore } from '@/stores/operationTab';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import { clearAgentOutput } from '@/services/agent-output';
import { clearAskUserQuestion } from '@/services/ask-user-question';
import { clearHumanApproval } from '@/services/human-approval';
import { AI_CHAT_PANEL_STATE_STORAGE_KEY } from '@/components/AIChat/AIChatContext';
import styles from './index.module.less';

/** 获取用户名首字母（支持中文取第一个字） */
function getInitial(name: string): string {
  if (!name) return 'U';
  return name.charAt(0).toUpperCase();
}

/**
 * 从菜单树中提取 position=HEADER 的顶级菜单项
 * 只取 menuType !== 3 且 visible 的菜单，按 sort 排序
 */
function extractHeaderMenus(menus: MenuItem[]): MenuItem[] {
  return menus
    .filter(
      (m) =>
        m.menuType !== 3 && m.visible && m.position === MenuPosition.HEADER,
    )
    .sort((a, b) => a.sort - b.sort);
}

export default function UserDropdown() {
  const { agent } = useAgent({ agentId: 'brain' });
  const userInfo = useUserStore((s) => s.userInfo);
  const menus = useMenuStore((s) => s.menus);
  const { message, modal } = App.useApp();
  const switchToSettings = useOperationTabStore((s) => s.switchToSettings);

  const displayName = userInfo?.nickname || userInfo?.username || '用户';
  const email = userInfo?.email;
  const avatarUrl = userInfo?.avatar;
  const initial = getInitial(displayName);

  // 提取 position=2 (HEADER) 的菜单项
  const headerMenus = extractHeaderMenus(menus);

  // 退出登录
  const handleLogout = () => {
    modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await logout();
          useUserStore.getState().logout();
          useMenuStore.getState().clearMenus();
          useHeadlessStore.getState().clearThreadId();
          useHeadlessStore.getState().setHeadless(false);
          useForwardedPropsStore.setState({
            operationMode: 'human',
            exitReason: '',
            extras: {},
          });
          useOperationTabStore.getState().setActiveTab('interface');
          clearHumanApproval();
          clearAskUserQuestion();
          clearAgentOutput();
          agent.abortRun();
          agent.setMessages([]);
          localStorage.removeItem(AI_CHAT_PANEL_STATE_STORAGE_KEY);
          document.body.removeAttribute('data-headless-chat-ready');
          emitEvent(EventType.LOGOUT);
          message.success('退出成功');
          const loginPath =
            process.env.UMI_APP_LOGIN_PATH || '/sub-system/login';
          history.push(loginPath);
        } catch {
          // 错误提示已在 request.ts 中统一处理
        }
      },
    });
  };

  // 菜单项点击
  const handleMenuClick = (menu: MenuItem) => {
    history.push(menu.path);
  };

  // 下拉面板内容
  const dropdownContent = (
    <div className={styles.dropdownPanel}>
      {/* 信息头 */}
      <div className={styles.infoHeader}>
        <Avatar
          className={styles.infoAvatar}
          size={40}
          src={avatarUrl || undefined}
          style={
            avatarUrl
              ? undefined
              : {
                  background:
                    'linear-gradient(135deg, var(--primary-color, #1a5fb4), #764ba2)',
                  color: '#fff',
                  fontWeight: 600,
                }
          }
        >
          {avatarUrl ? undefined : initial}
        </Avatar>
        <div className={styles.infoDetail}>
          <div className={styles.infoName}>{displayName}</div>
          {email && <div className={styles.infoEmail}>{email}</div>}
        </div>
        {userInfo?.admin && (
          <Tooltip title="设置">
            <div className={styles.settingsBtn} onClick={switchToSettings}>
              <SettingOutlined />
            </div>
          </Tooltip>
        )}
      </div>

      {/* 动态菜单项：position=2 的路由 */}
      {headerMenus.map((menu) => (
        <div
          key={menu.id}
          className={styles.menuItem}
          onClick={() => handleMenuClick(menu)}
        >
          {getIconComponent(menu.icon) ? (
            <span className={styles.menuItemIcon}>
              {getIconComponent(menu.icon)}
            </span>
          ) : (
            <span className={styles.menuItemDot} />
          )}
          <span>{menu.menuName}</span>
        </div>
      ))}

      {/* 退出登录（固定） */}
      <div
        className={`${styles.menuItem} ${styles.dangerItem}`}
        onClick={handleLogout}
      >
        <LogoutOutlined className={styles.menuItemIcon} />
        <span>退出登录</span>
      </div>
    </div>
  );

  return (
    <Dropdown
      popupRender={() => dropdownContent}
      placement="bottomRight"
      trigger={['click']}
    >
      <div className={styles.trigger}>
        <Avatar
          className={styles.triggerAvatar}
          size={32}
          src={avatarUrl || undefined}
          style={
            avatarUrl
              ? undefined
              : {
                  background:
                    'linear-gradient(135deg, var(--primary-color, #1a5fb4), #764ba2)',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: 14,
                }
          }
        >
          {avatarUrl ? undefined : initial}
        </Avatar>
        <span className={styles.triggerName}>{displayName}</span>
      </div>
    </Dropdown>
  );
}

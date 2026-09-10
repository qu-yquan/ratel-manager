import ThemeSwitcher from '@/components/ThemeSwitcher';
import UserDropdown from '@/components/UserDropdown';
import WorkspaceSwitcher from '@/components/WorkspaceSwitcher';
import { CopilotChatPanel } from '@/components/AIChat/CopilotChatPanel';
import AssistantOperationArea from '@/components/AssistantOperationArea';
import { RouteTracker } from '@/components/RouteTracker';
import RouteSelector from '@/components/RouteSelector';
import SidebarNavigation from '@/components/SidebarNavigation';
import {
  PanelProvider,
  usePanelContext,
} from '@/components/AIChat/AIChatContext';
import { GwsuCopilotKitProvider } from '@/providers/CopilotKitProvider';
import { ArrowDownOutlined, RobotOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import {
  EventType,
  onEvent,
  ThemeLayout,
  useThemeContext,
  useHeadlessStore,
  useProjectConfigStore,
  useUserStore,
} from '@gwsu/core';
import { useCallback, useEffect, useRef, useState } from 'react';
import { history, Outlet, useLocation } from 'umi';
import { useOperationTabStore } from '@/stores/operationTab';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import styles from './index.module.less';

export default function LayoutComponent() {
  return (
    <ThemeLayout>
      <LayoutRouter />
    </ThemeLayout>
  );
}

/** 路由层：根据是否登录页决定是否初始化 CopilotKit */
function LayoutRouter() {
  const location = useLocation();
  const { currentTheme } = useThemeContext();
  const isLoginPage = location.pathname.includes('/login');

  // 已登录时加载项目配置（登录页使用独立的免认证接口）
  useEffect(() => {
    if (!isLoginPage && useUserStore.getState().checkLogin()) {
      useProjectConfigStore.getState().loadConfig().catch(console.error);
    }
  }, [isLoginPage]);

  // 访问根路径时自动跳转首页 + 登录事件监听
  useEffect(() => {
    const homePath = process.env.UMI_APP_HOME_PATH as string;
    const loginPath = process.env.UMI_APP_LOGIN_PATH as string;
    if (location.pathname === '/') {
      history.replace(homePath);
    }

    const successEvent = onEvent(EventType.LOGIN_SUCCESS, (payload) => {
      console.log('登录成功, payload:', JSON.stringify(payload));
      // 如果 LOGIN_SUCCESS 事件携带了 threadId，存储到 headlessStore
      const { threadId, isHeadless } =
        (payload as { threadId?: string; isHeadless?: boolean }) || {};
      if (threadId) {
        useHeadlessStore.getState().setThreadId(threadId);
      }
      // 无头浏览器登录时，自动切换为 AI 操作模式
      if (isHeadless) {
        useHeadlessStore.getState().setHeadless(true);
        useForwardedPropsStore.getState().setOperationMode('ai');
      }

      // 登录成功后加载项目配置
      useProjectConfigStore.getState().loadConfig().catch(console.error);

      history.push(homePath);
    });

    const expireEvent = onEvent(EventType.TOKEN_EXPIRED, () => {
      history.push(loginPath);
    });

    return () => {
      successEvent();
      expireEvent();
    };
  }, [location.pathname]);

  // 登录页面：不初始化 CopilotKit，使用简单布局
  if (isLoginPage) {
    return (
      <div className={`${styles.mainLayout} ${styles.loginMode}`}>
        <div className={styles.loginContent}>
          <Outlet />
        </div>
      </div>
    );
  }

  // 非登录页面：初始化 CopilotKit
  return (
    <GwsuCopilotKitProvider>
      <RouteTracker />
      <PanelProvider>
        <MainLayoutContent currentTheme={currentTheme} />
      </PanelProvider>
    </GwsuCopilotKitProvider>
  );
}

/** 主布局内容（在 CopilotKit 和 PanelProvider 上下文内） */
function MainLayoutContent({
  currentTheme,
}: {
  currentTheme: ReturnType<typeof useThemeContext>['currentTheme'];
}) {
  const { panelState, setPanelMode, setPanelPosition, togglePanel } =
    usePanelContext();
  const { activeTab, setActiveTab } = useOperationTabStore();
  const projectName = useProjectConfigStore((s) => s.projectName);
  const location = useLocation();
  // 悬浮提示相关状态
  const [showGuide, setShowGuide] = useState(false);
  const guideTimerRef = useRef<NodeJS.Timeout | null>(null);
  const robotBtnRef = useRef<HTMLDivElement>(null);

  // 路由变化 → 自动切到界面 Tab
  useEffect(() => {
    setActiveTab('interface');
  }, [location.pathname, setActiveTab]);
  // 当面板收起时，显示引导提示
  useEffect(() => {
    if (panelState.mode === 'hidden') {
      guideTimerRef.current = setTimeout(() => {
        setShowGuide(true);
      }, 300);
    } else {
      setShowGuide(false);
    }

    return () => {
      if (guideTimerRef.current) {
        clearTimeout(guideTimerRef.current);
      }
    };
  }, [panelState.mode]);

  // 面板操作
  const handleFixed = useCallback(() => setPanelMode('fixed'), [setPanelMode]);
  const handleDraggable = useCallback(() => {
    const sidebarWidth = Number.parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue(
        '--sidebar-width',
      ),
    );
    const safeSidebarWidth = Number.isFinite(sidebarWidth) ? sidebarWidth : 240;
    const panelBaseLeft = 20;
    const viewportMargin = 20;
    const maximumX = Math.max(
      0,
      window.innerWidth - panelState.width - panelBaseLeft - viewportMargin,
    );
    const minimumX = maximumX >= safeSidebarWidth ? safeSidebarWidth : 0;
    const nextX = Math.min(Math.max(panelState.position.x, minimumX), maximumX);

    if (nextX !== panelState.position.x) {
      setPanelPosition({ ...panelState.position, x: nextX });
    }
    setPanelMode('draggable');
  }, [panelState.position, panelState.width, setPanelMode, setPanelPosition]);
  const handleHide = useCallback(() => setPanelMode('hidden'), [setPanelMode]);
  const handleRobotClick = useCallback(() => {
    setShowGuide(false);
    togglePanel();
  }, [togglePanel]);

  // 判断显示模式
  const isHidden = panelState.mode === 'hidden';
  const showSidebar = panelState.mode !== 'fixed';

  // 将 AI 面板模式同步到 body，供全局 CSS 约束弹框区域
  useEffect(() => {
    document.body.dataset.aiMode = panelState.mode;
    return () => {
      delete document.body.dataset.aiMode;
    };
  }, [panelState.mode]);

  return (
    <div className={styles.mainLayout}>
      {/* 顶部导航栏 - 固定不变 */}
      <header
        className={styles.mainHeader}
        style={{ background: currentTheme.colors.surface }}
      >
        <div className={styles.headerLeft}>
          <div
            className={styles.logo}
            onClick={() =>
              history.push(process.env.UMI_APP_HOME_PATH as string)
            }
          >
            <img src="/favicon.jpg" alt="logo" />
            <span style={{ color: currentTheme.colors.text }}>
              {projectName}
            </span>
          </div>
          {/* 操作区 Tab 切换 */}
          <div className={styles.headerTabs}>
            {/* Tab1: 界面 - 包含路由选择器 */}
            <button
              type="button"
              className={`${styles.headerTab} ${
                activeTab === 'interface' ? styles.headerTabActive : ''
              }`}
              onClick={() => setActiveTab('interface')}
            >
              <RouteSelector
                isActive={activeTab === 'interface'}
                navigationMode={showSidebar ? 'label' : 'dropdown'}
              />
            </button>
            {/* Tab2: AI 输出 */}
            <button
              type="button"
              className={`${styles.headerTab} ${
                activeTab === 'ai-output' ? styles.headerTabActive : ''
              }`}
              onClick={() => setActiveTab('ai-output')}
            >
              <RobotOutlined className={styles.headerTabIcon} />
              <span>AI 输出</span>
            </button>
          </div>
        </div>
        <div className={styles.headerRight}>
          {/* AI 助手图标 - 隐藏状态下显示 */}
          {isHidden && (
            <div ref={robotBtnRef} className={styles.robotBtnWrapper}>
              <Button
                type="text"
                className={styles.headerActionBtn}
                onClick={handleRobotClick}
                icon={<RobotOutlined />}
                aria-label="打开智能助手"
              />
              {/* 悬浮引导提示 */}
              {showGuide && (
                <div className={styles.guideBubble}>
                  <div className={styles.guideContent}>
                    <span>智能助手</span>
                    <ArrowDownOutlined className={styles.guideArrow} />
                  </div>
                </div>
              )}
            </div>
          )}
          <WorkspaceSwitcher />
          <ThemeSwitcher />
          <UserDropdown />
        </div>
      </header>

      {/* 下方内容区域 */}
      <div className={styles.contentLayout}>
        {/* AI 聊天区占位 - 固定模式下保留空间 */}
        {panelState.mode === 'fixed' && (
          <div className={styles.aiChatFixedPlaceholder} />
        )}

        {/* 常驻菜单位于 AI 界面读取范围之外，避免增加页面上下文 */}
        {showSidebar && <SidebarNavigation />}

        {/* 智能助手操作区 - 能力容器 */}
        <AssistantOperationArea />
      </div>

      {/* AI 聊天面板 - 始终渲染，通过 mode 属性控制显示模式，避免重新初始化 */}
      <CopilotChatPanel
        fixedWidth="100%"
        mode={panelState.mode}
        onFixed={handleFixed}
        onDraggable={handleDraggable}
        onHide={handleHide}
      />
    </div>
  );
}

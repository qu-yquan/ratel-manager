import { App, ConfigProvider, theme as antdTheme } from 'antd';
import React, { useState ,useCallback ,useEffect,createContext, useContext } from 'react';
import {
  defaultTheme,
  themeMap,
  EventType,
  onEvent,
} from '../constants';
import type { ThemeConfig } from '../types';
import {
  applyTheme,
  saveTheme,
  notifyThemeChange
} from '../utils';
import { setErrorToastHandler } from '../utils/request';

interface ThemeContextValue {
  currentTheme: ThemeConfig;
  changeTheme: (theme: ThemeConfig) => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export const useThemeContext = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useThemeContext must be used within ThemeLayout');
  }
  return context;
};

interface ThemeLayoutProps {
  children?: React.ReactNode;
}

const ThemeLayout: React.FC<ThemeLayoutProps> = ({ children }) => {


   const [currentTheme, setCurrentTheme] = useState<ThemeConfig>(defaultTheme);


    // 切换主题
    const changeTheme = useCallback((theme: ThemeConfig) => {
      setCurrentTheme(theme);
      saveTheme(theme.key);
      applyTheme(theme);
      notifyThemeChange(theme);
    }, []);

    useEffect(() => {
       const initTheme = () => {
         const savedThemeKey = localStorage.getItem('gwsu-theme');
         if (savedThemeKey && themeMap[savedThemeKey]) {
           const theme = themeMap[savedThemeKey] as ThemeConfig;
           setCurrentTheme(theme);
           applyTheme(theme);
         } else {
           applyTheme(defaultTheme);
         }
       };

       initTheme();

     }, [currentTheme]);

     useEffect(() => {
       // 监听主题变更事件
       const unsubscribe = onEvent(EventType.THEME_CHANGE, (payload) => {
         const theme = payload as ThemeConfig;
         setCurrentTheme(theme);
         applyTheme(theme);
       });
       return unsubscribe;
     }, []);

  const isDarkTheme = currentTheme.key === 'midnight';

  const antdThemeConfig = {
    token: {
      colorPrimary: currentTheme.colors.primary,
      colorSuccess: currentTheme.colors.success,
      colorWarning: currentTheme.colors.warning,
      colorError: currentTheme.colors.error,
      colorInfo: currentTheme.colors.info,
      colorBgBase: currentTheme.colors.background,
      colorTextBase: currentTheme.colors.text,
      colorBorder: currentTheme.colors.border,
    },
    algorithm: isDarkTheme ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
  };

  return (
    <ThemeContext.Provider value={{ currentTheme, changeTheme }}>
      <ConfigProvider theme={antdThemeConfig}>
        <App>
          <RequestErrorToastInjector />
          <div
            className="theme-layout-wrapper"
            style={{
              height: '100%',
              minHeight: 0,
              background: currentTheme.colors.background,
              color: currentTheme.colors.text,
            }}>
            {children}
          </div>
        </App>
      </ConfigProvider>
    </ThemeContext.Provider>
  );
};

/**
 * 内部组件：利用 App.useApp() 获取上下文化的 notification，
 * 注入到 request 模块，使全局错误提示能正确消费 ConfigProvider 上下文
 */
const RequestErrorToastInjector: React.FC = () => {
  const { notification, message } = App.useApp();

  useEffect(() => {
    setErrorToastHandler((msg: string, errCode?: string) => {
      notification.error({
        title: '请求错误',
        description: (
          <div>
            <div>{msg}</div>
            {errCode && (
              <div
                style={{
                  marginTop: 8,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'flex-end',
                  cursor: 'pointer',
                  color: '#1890ff',
                }}
                onClick={() => {
                  navigator.clipboard.writeText(errCode);
                  message.success('错误码已复制');
                }}
              >
                <span style={{ fontSize: 12, color: '#999' }}>📋 {errCode}</span>
              </div>
            )}
          </div>
        ),
        duration: 3,
      });
    });
  }, [notification, message]);

  return null;
};

export { ThemeLayout };

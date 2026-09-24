import React, { useEffect } from 'react';
// @ts-ignore
import { Outlet } from 'umi';
import { ThemeLayout, useProjectConfigStore, useUserStore } from '@gwsu/core';

const Layout: React.FC = () => {
  // 已登录时加载项目配置（登录页使用独立的免认证接口）
  useEffect(() => {
    if (useUserStore.getState().checkLogin()) {
      useProjectConfigStore.getState().loadConfig().catch(console.error);
    }
  }, []);

  return (
    <ThemeLayout>
      <Outlet />
    </ThemeLayout>
  );
};

export default Layout;

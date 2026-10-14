import { defineConfig } from 'umi';
import { dirname } from 'node:path';
import routes from './routes';

const isDev = process.env.NODE_ENV === 'development';

export default defineConfig({
  npmClient: 'pnpm',
  // 共享库与应用必须使用同一份 antd，避免 App/ConfigProvider 上下文断开。
  alias: {
    antd: dirname(require.resolve('antd/package.json')),
  },
  base: '/sub-security',
  publicPath: isDev ? '/' : '/sub-security/',
  plugins: ['@umijs/plugins/dist/qiankun'],
  esbuildMinifyIIFE: true,
  qiankun: {
    slave: {},
  },
  routes,
});

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
  base: '/sub-system',
  publicPath: isDev ? '/' : '/sub-system/',
  mountElementId: 'sub-system-root',
  mfsu: false,
  esbuildMinifyIIFE: true,
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    slave: {},
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
});

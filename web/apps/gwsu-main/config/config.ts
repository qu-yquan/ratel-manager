import { defineConfig } from 'umi';
import { dirname } from 'node:path';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
  // 共享库与应用必须使用同一份 antd，避免 App/ConfigProvider 上下文断开。
  alias: {
    antd: dirname(require.resolve('antd/package.json')),
  },
  mfsu: false,
  esbuildMinifyIIFE: true,
  favicons: ['/favicon.jpg'],
  title: 'Ratel Management',
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    master: {},
  },
  routes,
  // 代理配置
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
});

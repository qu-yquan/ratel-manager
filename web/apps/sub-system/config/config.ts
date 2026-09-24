import { defineConfig } from 'umi';
import routes from './routes';

const isDev = process.env.NODE_ENV === 'development';

export default defineConfig({
  npmClient: 'pnpm',
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

import { defineConfig } from 'umi';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
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

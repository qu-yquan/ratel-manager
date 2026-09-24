import { defineConfig } from 'umi';
import routes from './routes';

const isDev = process.env.NODE_ENV === 'development';

export default defineConfig({
  npmClient: 'pnpm',
  base: '/sub-security',
  publicPath: isDev ? '/' : '/sub-security/',
  plugins: ['@umijs/plugins/dist/qiankun'],
  esbuildMinifyIIFE: true,
  qiankun: {
    slave: {},
  },
  routes,
});
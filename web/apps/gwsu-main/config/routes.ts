const homePath = process.env.UMI_APP_HOME_PATH || '/sub-system/dashboard';

export default [{ path: '/', redirect: homePath }];

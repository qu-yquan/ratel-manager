/** 基础地址配置 */
export interface BaseUrlConfig {
  /** 项目名称 */
  projectName: string;
  /** 前端地址 */
  viewBaseUrl: string;
  /** 后端 API 地址 */
  apiBaseUrl: string;
}

/** 基础地址默认配置 */
export const DEFAULT_BASE_URL_CONFIG: BaseUrlConfig = {
  projectName: 'Ratel',
  viewBaseUrl: 'http://127.0.0.1:8000',
  apiBaseUrl: 'http://127.0.0.1:8888',
};

/** 创建默认的基础地址配置 */
export function createDefaultBaseUrlConfig(): BaseUrlConfig {
  return { ...DEFAULT_BASE_URL_CONFIG };
}

/** 验证码类型 */
export type CaptchaType = 'BLOCK_PUZZLE' | 'CLICK_WORD';

/** 图形验证码配置 */
export interface CaptchaConfig {
  /** 是否启用验证码能力 */
  enabled: boolean;
  /** 默认验证码类型 */
  type: CaptchaType;
  /** 水印文字 */
  waterMark: string;
  /** 验证码挑战有效时间，单位秒 */
  expireSeconds: number;
  /** 二次校验凭证有效时间，单位秒 */
  verificationExpireSeconds: number;
}

/** 验证码类型选项 */
export interface CaptchaTypeOption {
  key: CaptchaType;
  value: string;
}

/** 图形验证码默认配置 */
export const DEFAULT_CAPTCHA_CONFIG: CaptchaConfig = {
  enabled: true,
  type: 'BLOCK_PUZZLE',
  waterMark: 'Ratel-Manager',
  expireSeconds: 120,
  verificationExpireSeconds: 180,
};

/** 创建默认的图形验证码配置 */
export function createDefaultCaptchaConfig(): CaptchaConfig {
  return { ...DEFAULT_CAPTCHA_CONFIG };
}

/** 规范化图形验证码配置，只保留公共 CaptchaProperties 字段 */
export function normalizeCaptchaConfig(
  config?: Partial<CaptchaConfig>,
): CaptchaConfig {
  return {
    enabled: config?.enabled ?? DEFAULT_CAPTCHA_CONFIG.enabled,
    type: config?.type ?? DEFAULT_CAPTCHA_CONFIG.type,
    waterMark: config?.waterMark ?? DEFAULT_CAPTCHA_CONFIG.waterMark,
    expireSeconds:
      config?.expireSeconds ?? DEFAULT_CAPTCHA_CONFIG.expireSeconds,
    verificationExpireSeconds:
      config?.verificationExpireSeconds ??
      DEFAULT_CAPTCHA_CONFIG.verificationExpireSeconds,
  };
}

/** 基础地址配置 key */
export const BASE_URL_CONFIG_KEY = 'basic_url_config';

/** 图形验证码配置 key */
export const CAPTCHA_CONFIG_KEY = 'captcha_config';

/** 日志存储媒介 */
export type LogStorageMedium = 'DATABASE' | 'ES';

/** 日志生命周期 */
export interface LogLifeCycleConfig {
  coldMinAge: number;
  deleteMinAge: number;
}

/** 单类日志存储配置 */
export interface LogStorageItemConfig {
  medium: LogStorageMedium;
  dataLifeCycle: LogLifeCycleConfig;
}

/** 日志存储配置 */
export interface LogStorageConfig {
  operationLog: LogStorageItemConfig;
  tableLog: LogStorageItemConfig;
  loginLog: LogStorageItemConfig;
}

/** 日志存储媒介选项 */
export interface LogStorageMediumOption {
  key: LogStorageMedium;
  value: string;
}

export const DEFAULT_LOG_STORAGE_CONFIG: LogStorageConfig = {
  operationLog: {
    medium: 'DATABASE',
    dataLifeCycle: { coldMinAge: 60, deleteMinAge: 180 },
  },
  tableLog: {
    medium: 'DATABASE',
    dataLifeCycle: { coldMinAge: 60, deleteMinAge: 180 },
  },
  loginLog: {
    medium: 'DATABASE',
    dataLifeCycle: { coldMinAge: 360, deleteMinAge: 720 },
  },
};

export function createDefaultLogStorageConfig(): LogStorageConfig {
  const cloneItem = (item: LogStorageItemConfig): LogStorageItemConfig => ({
    medium: item.medium,
    dataLifeCycle: { ...item.dataLifeCycle },
  });
  return {
    operationLog: cloneItem(DEFAULT_LOG_STORAGE_CONFIG.operationLog),
    tableLog: cloneItem(DEFAULT_LOG_STORAGE_CONFIG.tableLog),
    loginLog: cloneItem(DEFAULT_LOG_STORAGE_CONFIG.loginLog),
  };
}

export function normalizeLogStorageConfig(
  config?: Partial<LogStorageConfig>,
): LogStorageConfig {
  const defaults = createDefaultLogStorageConfig();
  const normalizeItem = (
    value: LogStorageItemConfig | undefined,
    fallback: LogStorageItemConfig,
  ): LogStorageItemConfig => ({
    medium: value?.medium ?? fallback.medium,
    dataLifeCycle: {
      coldMinAge:
        value?.dataLifeCycle?.coldMinAge ?? fallback.dataLifeCycle.coldMinAge,
      deleteMinAge:
        value?.dataLifeCycle?.deleteMinAge ??
        fallback.dataLifeCycle.deleteMinAge,
    },
  });
  return {
    operationLog: normalizeItem(config?.operationLog, defaults.operationLog),
    tableLog: normalizeItem(config?.tableLog, defaults.tableLog),
    loginLog: normalizeItem(config?.loginLog, defaults.loginLog),
  };
}

/** 日志存储配置 key */
export const LOG_STORAGE_CONFIG_KEY = 'log_storage_config';

/** 通用配置 Tab 标识 */
export type GeneralTabKey = 'projectUrl' | 'captcha' | 'log';

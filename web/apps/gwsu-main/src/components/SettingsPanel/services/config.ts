import { get, post, del } from '@gwsu/core';
import type { ConfigVO } from '@gwsu/core';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type {
  CaptchaTypeOption,
  LogStorageMediumOption,
} from '../GeneralConfigTab/types';
import type { RemoteControlConfig } from '../AssistantConfigTab/types';

const BASE = '/security/config';

/** 配置信息（兼容旧引用） */
export type ConfigInfo = ConfigVO;

export interface ConfigQuery {
  configKey?: string;
  configName?: string;
  valueType?: ConfigValueType;
  configType?: ConfigType;
  modulePrefix?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface ConfigPageResult {
  records: ConfigInfo[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 分页查询配置 */
export async function getConfigPage(query: ConfigQuery) {
  const res = await post<ConfigPageResult>(`${BASE}/page`, query);
  return res.data;
}

/** 新增或更新配置 */
export async function saveOrUpdateConfig(data: Partial<ConfigInfo>) {
  const res = await post<boolean>(BASE, data);
  return res.data;
}

/** 保存 LLM 模型配置并刷新智能助手模型 */
export async function saveLlmModelConfig(data: Partial<ConfigInfo>) {
  const res = await post<boolean>('/security/brain/model/llm-config', data);
  return res.data;
}

/** 批量删除配置 */
export async function deleteConfigs(ids: string[]) {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}

/** 保存远程操作配置 */
export async function saveRemoteControlConfig(config: RemoteControlConfig) {
  const res = await post<boolean>(
    '/security/connect/config/remote-control',
    config,
  );
  return res.data;
}

/** 获取图形验证码类型 */
export async function getCaptchaTypeOptions() {
  const res = await get<CaptchaTypeOption[]>(`${BASE}/captchaType`);
  return res.data ?? [];
}

/** 获取日志存储媒介 */
export async function getLogStorageMediumOptions() {
  const res = await get<LogStorageMediumOption[]>(
    '/log/log/config/storage-medium-options',
  );
  return res.data ?? [];
}

/** 下载钉钉 AI 输出卡片模板导入文件接口路径 */
export const AI_CARD_TEMPLATE_DOWNLOAD_URL =
  '/security/connect/dingtalk/ai-card-template/download';

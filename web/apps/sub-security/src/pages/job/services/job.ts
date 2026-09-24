import { get, post } from '@gwsu/core';
import type {
  JobInfo,
  JobQuery,
  JobInfoCreateDTO,
  JobLog,
  JobLogQuery,
  LogData,
  GlueVersion,
  ModuleInfo,
} from '../types';

const BASE = '/kit/job/info';
const LOG_BASE = '/kit/job/log';

/** 分页查询任务列表 */
export async function getJobPage(query: JobQuery) {
  const res = await post<{
    records: JobInfo[];
    total: number;
    size: number;
    current: number;
    pages: number;
  }>(`${BASE}/page`, query);
  return res.data;
}

/** DTO适配新增任务 */
export async function addJob(data: JobInfoCreateDTO): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/addByDTO`, data);
  return res.data;
}

/** DTO适配更新任务 */
export async function updateJob(data: JobInfoCreateDTO): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/updateByDTO`, data);
  return res.data;
}

/** 删除任务 */
export async function removeJob(id: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/remove`, null, { params: { id } });
  return res.data;
}

/** 启动任务 */
export async function startJob(id: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/start`, null, { params: { id } });
  return res.data;
}

/** 停止任务 */
export async function stopJob(id: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/stop`, null, { params: { id } });
  return res.data;
}

/** 手动触发一次 */
export async function triggerJob(id: string, executorParam?: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/trigger`, null, { params: { id, executorParam } });
  return res.data;
}

/** 预估下次触发时间 */
export async function getNextTriggerTime(
  scheduleType: string,
  scheduleConf: string,
): Promise<string[]> {
  const res = await get<string[]>(`${BASE}/nextTriggerTime`, {
    scheduleType,
    scheduleConf,
  });
  return res.data ?? [];
}

/** 终止运行中的任务 */
export async function killJob(logId: string): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/kill`, null, { params: { logId } });
  return res.data;
}

/** 读取执行器端完整日志 */
export async function getLogContent(
  logId: string,
  fromLineNum: number = 1,
): Promise<LogData | null> {
  const res = await get<LogData>(`${BASE}/logContent`, { logId, fromLineNum });
  return res.data;
}

/** 查询在线Handler列表(过滤urlJobHandler) */
export async function getHandlerList(): Promise<string[]> {
  const res = await get<string[]>(`${BASE}/handlerList`);
  return res.data ?? [];
}

/** 查询GLUE版本历史 */
export async function getGlueVersionList(jobId: string): Promise<GlueVersion[]> {
  const res = await get<GlueVersion[]>(`${BASE}/glueVersionList`, { jobId });
  return res.data ?? [];
}

/** 查询GLUE版本详情 */
export async function getGlueVersionDetail(id: string): Promise<GlueVersion | null> {
  const res = await get<GlueVersion>(`${BASE}/glueVersionDetail`, { id });
  return res.data;
}

/** 获取模块列表 */
export async function getModuleList(): Promise<ModuleInfo[]> {
  const res = await post<ModuleInfo[]>('/modules/list');
  return res.data ?? [];
}

// ===== 日志相关 =====

/** 分页查询日志列表 */
export async function getLogPage(query: JobLogQuery) {
  const res = await post<{
    records: JobLog[];
    total: number;
    size: number;
    current: number;
    pages: number;
  }>(`${LOG_BASE}/page`, query);
  return res.data;
}

/** 查询日志详情 */
export async function getLogDetail(id: string): Promise<JobLog> {
  const res = await get<JobLog>(`${LOG_BASE}/load`, { id });
  return res.data;
}

/** 清理日志 */
export async function clearLog(jobId: string, type: number): Promise<boolean> {
  const res = await post<boolean>(`${LOG_BASE}/clearLog`, null, {
    params: { jobId, type },
  });
  return res.data;
}

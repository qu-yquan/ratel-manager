/** 任务信息 */
export interface JobInfo {
  id?: string;
  name: string;
  author: string;
  alarmEmail?: string;
  scheduleType: string;
  scheduleConf: string;
  misfireStrategy: string;
  executorRouteStrategy: string;
  executorHandler?: string;
  executorParam?: string;
  executorBlockStrategy: string;
  executorTimeout: number;
  executorFailRetryCount: number;
  glueType: string;
  glueSource?: string;
  glueRemark?: string;
  glueUpdatetime?: string;
  childJobId?: string;
  triggerStatus: number;
  triggerLastTime: number;
  triggerNextTime: number;
  createTime?: string;
}

/** 任务查询条件 */
export interface JobQuery {
  name?: string;
  executorHandler?: string;
  author?: string;
  triggerStatus?: number;
  pageNum?: number;
  pageSize?: number;
}

/** 任务模式 */
export type JobMode = 'URL' | 'BEAN' | 'GLUE';

/** 任务新增/编辑DTO */
export interface JobInfoCreateDTO {
  id?: string;
  name: string;
  author?: string;
  alarmEmail?: string;
  misfireStrategy: string;
  executorRouteStrategy: string;
  executorBlockStrategy: string;
  executorTimeout: number;
  executorFailRetryCount: number;
  jobMode: JobMode;
  prefix?: string;
  url?: string;
  bodyJson?: string;
  executorHandler?: string;
  executorParam?: string;
  glueType?: string;
  glueSource?: string;
  glueRemark?: string;
  scheduleType: string;
  scheduleConf: string;
  childJobId?: string;
}

/** 调度日志 */
export interface JobLog {
  id: string;
  jobId: string;
  executorAddress?: string;
  executorHandler?: string;
  executorParam?: string;
  executorShardingParam?: string;
  executorFailRetryCount: number;
  triggerTime?: string;
  triggerCode: number;
  triggerMsg?: string;
  handleTime?: string;
  handleCode: number;
  handleMsg?: string;
  alarmStatus: number;
}

/** 日志查询条件 */
export interface JobLogQuery {
  jobId?: string;
  logStatus?: number;
  triggerTimeStart?: string;
  triggerTimeEnd?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 执行器端日志数据 */
export interface LogData {
  fromLineNum: number;
  toLineNum: number;
  logContent: string;
  isEnd: boolean;
}

/** GLUE版本记录 */
export interface GlueVersion {
  id: string;
  jobId: string;
  glueType: string;
  glueSource: string;
  glueRemark: string;
  createTime?: string;
}

/** 模块信息 */
export interface ModuleInfo {
  prefix: string;
  name: string;
}

/** 选项常量 */
export const JOB_MODE_OPTIONS = [
  { label: '平台URL', value: 'URL' },
  { label: 'GLUE', value: 'GLUE' },
  { label: 'BEAN', value: 'BEAN' },
] as const;

export const SCHEDULE_TYPE_OPTIONS = [
  { label: '无', value: 'NONE' },
  { label: 'CRON', value: 'CRON' },
  { label: '固定速率', value: 'FIX_RATE' },
] as const;

export const MISFIRE_STRATEGY_OPTIONS = [
  { label: '忽略', value: 'DO_NOTHING' },
  { label: '立即执行一次', value: 'FIRE_ONCE_NOW' },
] as const;

export const BLOCK_STRATEGY_OPTIONS = [
  { label: '单机串行', value: 'SERIAL_EXECUTION' },
  { label: '丢弃后续调度', value: 'DISCARD_LATER' },
  { label: '覆盖之前调度', value: 'COVER_EARLY' },
] as const;

export const ROUTE_STRATEGY_OPTIONS = [
  { label: '第一个', value: 'FIRST' },
  { label: '最后一个', value: 'LAST' },
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性HASH', value: 'CONSISTENT_HASH' },
  { label: '最不经常使用', value: 'LEAST_FREQUENTLY_USED' },
  { label: '最近最久未使用', value: 'LEAST_RECENTLY_USED' },
  { label: '故障转移', value: 'FAILOVER' },
  { label: '忙碌转移', value: 'BUSYOVER' },
  { label: '分片广播', value: 'SHARDING_BROADCAST' },
] as const;

export const GLUE_TYPE_OPTIONS = [
  { label: 'GLUE(Java)', value: 'GLUE_GROOVY' },
  { label: 'GLUE(Shell)', value: 'GLUE_SHELL' },
  { label: 'GLUE(Python3)', value: 'GLUE_PYTHON' },
  { label: 'GLUE(Nodejs)', value: 'GLUE_NODEJS' },
  { label: 'GLUE(PHP)', value: 'GLUE_PHP' },
  { label: 'GLUE(PowerShell)', value: 'GLUE_POWERSHELL' },
] as const;

export const LOG_STATUS_OPTIONS = [
  { label: '全部', value: -1 },
  { label: '成功', value: 1 },
  { label: '失败', value: 2 },
  { label: '运行中', value: 3 },
] as const;

export const TRIGGER_STATUS_OPTIONS = [
  { label: '全部', value: -1 },
  { label: '运行中', value: 1 },
  { label: '已停止', value: 0 },
] as const;

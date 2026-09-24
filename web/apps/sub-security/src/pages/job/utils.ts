import type { JobInfo, JobInfoCreateDTO, JobMode } from './types';

export function deriveJobMode(record: JobInfo): JobMode {
  if (record.glueType !== 'BEAN') return 'GLUE';
  if (record.executorHandler === 'urlJobHandler') return 'URL';
  return 'BEAN';
}

export function parseUrlParams(executorParam?: string): { prefix?: string; url?: string; bodyJson?: string } {
  if (!executorParam) return {};
  try {
    const obj = JSON.parse(executorParam);
    return { prefix: obj.prefix, url: obj.url, bodyJson: obj.bodyJson };
  } catch {
    return {};
  }
}

export function parseChildJobIds(childJobId?: string): string[] {
  if (!childJobId) return [];
  return childJobId
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function stringifyChildJobIds(childJobIds?: string[]): string | undefined {
  if (!childJobIds || childJobIds.length === 0) {
    return undefined;
  }
  return childJobIds.join(',');
}

type JobFormMode = 'create' | 'edit';

type JobFormValues = Omit<JobInfoCreateDTO, 'childJobId'> & {
  childJobId?: string[];
};

export function buildJobFormPayload(
  mode: JobFormMode,
  values: JobFormValues,
  data?: JobInfo | null,
): JobInfoCreateDTO {
  return {
    ...values,
    id: mode === 'edit' ? data?.id : values.id,
    childJobId: stringifyChildJobIds(values.childJobId),
  };
}

export function buildGlueUpdatePayload(
  record: JobInfo,
  glueType: string,
  glueSource: string,
  glueRemark: string,
): JobInfoCreateDTO {
  return {
    id: record.id,
    name: record.name,
    alarmEmail: record.alarmEmail,
    misfireStrategy: record.misfireStrategy,
    executorRouteStrategy: record.executorRouteStrategy,
    executorBlockStrategy: record.executorBlockStrategy,
    executorTimeout: record.executorTimeout,
    executorFailRetryCount: record.executorFailRetryCount,
    scheduleType: record.scheduleType,
    scheduleConf: record.scheduleConf,
    childJobId: record.childJobId,
    jobMode: 'GLUE',
    glueType,
    glueSource,
    glueRemark,
  };
}

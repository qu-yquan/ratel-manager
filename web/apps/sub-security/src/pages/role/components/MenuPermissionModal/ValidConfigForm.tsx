import React from 'react';
import { Form, Radio, DatePicker, TimePicker, Checkbox, Select } from 'antd';
import dayjs from 'dayjs';
import styles from './index.module.less';
import {
  VALID_TYPE_OPTIONS,
  CYCLE_TYPE_OPTIONS,
  WEEK_DAY_OPTIONS,
} from '../../types';
import type { ValidGroup } from '../../types';

interface ValidConfigFormProps {
  form: ReturnType<typeof Form.useForm>[0];
  /** 初始数据（编辑时传入已有的时效组） */
  initialData?: ValidGroup | null;
}

/** 时效配置表单组件 - 支持永久、绝对时间范围、周期性三种模式 */
const ValidConfigForm: React.FC<ValidConfigFormProps> = ({ form, initialData }) => {
  const validType = Form.useWatch('validType', form) ?? initialData?.validType ?? 1;
  const cycleType = Form.useWatch('cycleType', form) ?? initialData?.cycleType ?? 1;

  return (
    <div className={styles.validForm}>
      <Form form={form} layout="vertical" size="small">
        {/* 时效类型选择 */}
        <Form.Item name="validType" label="时效类型">
          <Radio.Group optionType="button" buttonStyle="solid">
            {VALID_TYPE_OPTIONS.map((opt) => (
              <Radio.Button key={opt.value} value={opt.value}>
                {opt.label}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>

        {/* 绝对时间范围 */}
        {validType === 2 && (
          <div className={styles.formSection}>
            <div className={styles.formSectionTitle}>时间范围</div>
            <div className={styles.formRow}>
              <Form.Item name="validStart" className={styles.formRowItem} label="开始时间">
                <DatePicker
                  showTime
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择开始时间"
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item name="validEnd" className={styles.formRowItem} label="结束时间">
                <DatePicker
                  showTime
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择结束时间"
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </div>
            <div className={styles.formHelpText}>
              配置角色菜单权限的生效时间范围，到达结束时间后权限自动失效
            </div>
          </div>
        )}

        {/* 周期性配置 */}
        {validType === 3 && (
          <>
            {/* 周期类型 */}
            <Form.Item name="cycleType" label="周期类型">
              <Radio.Group optionType="button" buttonStyle="solid">
                {CYCLE_TYPE_OPTIONS.map((opt) => (
                  <Radio.Button key={opt.value} value={opt.value}>
                    {opt.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </Form.Item>

            {/* 周期值选择 */}
            {cycleType === 1 && (
              <Form.Item name="cycleValue" label="选择星期">
                <Checkbox.Group className={styles.cycleValueGroup}>
                  {WEEK_DAY_OPTIONS.map((opt) => (
                    <Checkbox key={opt.value} value={opt.value}>
                      {opt.label}
                    </Checkbox>
                  ))}
                </Checkbox.Group>
              </Form.Item>
            )}

            {cycleType === 2 && (
              <Form.Item name="cycleValue" label="选择日期">
                <Select
                  mode="multiple"
                  placeholder="请选择日期"
                  style={{ width: '100%' }}
                  options={Array.from({ length: 31 }, (_, i) => ({
                    label: `${i + 1}号`,
                    value: String(i + 1),
                  }))}
                />
              </Form.Item>
            )}

            {/* 时间范围 */}
            <div className={styles.formSection}>
              <div className={styles.formSectionTitle}>每日时间范围</div>
              <div className={styles.formRow}>
                <Form.Item name="cycleStartTime" className={styles.formRowItem} label="开始时间">
                  <TimePicker
                    format="HH:mm"
                    placeholder="开始时间"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
                <Form.Item name="cycleEndTime" className={styles.formRowItem} label="结束时间">
                  <TimePicker
                    format="HH:mm"
                    placeholder="结束时间"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </div>
              <div className={styles.formHelpText}>
                开始时间有值代表从配置时间到 23:59:59，结束时间有值代表从 0 点到结束时间点，都为空表示全天
              </div>
            </div>
          </>
        )}
      </Form>
    </div>
  );
};

/** 将 ValidGroup 数据转换为表单初始值 */
export function validGroupToFormValues(data: ValidGroup) {
  return {
    validType: data.validType,
    validStart: data.validStart ? dayjs(data.validStart) : undefined,
    validEnd: data.validEnd ? dayjs(data.validEnd) : undefined,
    cycleType: data.cycleType ?? 1,
    cycleValue: data.cycleValue ? data.cycleValue.split(',') : [],
    cycleStartTime: data.cycleStartTime ? dayjs(data.cycleStartTime, 'HH:mm') : undefined,
    cycleEndTime: data.cycleEndTime ? dayjs(data.cycleEndTime, 'HH:mm') : undefined,
  };
}

/** 将表单值转换为保存请求所需的时效字段 */
export function formValuesToValidFields(values: {
  validType: number;
  validStart?: dayjs.Dayjs;
  validEnd?: dayjs.Dayjs;
  cycleType?: number;
  cycleValue?: string[];
  cycleStartTime?: dayjs.Dayjs;
  cycleEndTime?: dayjs.Dayjs;
}) {
  return {
    validType: values.validType,
    validStart: values.validStart ? values.validStart.format('YYYY-MM-DD HH:mm:ss') : undefined,
    validEnd: values.validEnd ? values.validEnd.format('YYYY-MM-DD HH:mm:ss') : undefined,
    cycleType: values.validType === 3 ? (values.cycleType ?? 1) : undefined,
    cycleValue: values.validType === 3 && values.cycleValue?.length
      ? values.cycleValue.join(',')
      : undefined,
    cycleStartTime: values.validType === 3 && values.cycleStartTime
      ? values.cycleStartTime.format('HH:mm')
      : undefined,
    cycleEndTime: values.validType === 3 && values.cycleEndTime
      ? values.cycleEndTime.format('HH:mm')
      : undefined,
  };
}

/** 根据 ValidGroup 生成可读的时效描述文字 */
export function getValidGroupLabel(group: ValidGroup): string {
  if (group.validType === 1) {
    return '永久';
  }
  if (group.validType === 2) {
    const start = group.validStart ?? '?';
    const end = group.validEnd ?? '?';
    return `${start} ~ ${end}`;
  }
  if (group.validType === 3) {
    const cycleTypeLabel =
      CYCLE_TYPE_OPTIONS.find((o) => o.value === group.cycleType)?.label ?? '';
    const dayLabels = group.cycleValue
      ? group.cycleValue
          .split(',')
          .map((v) => WEEK_DAY_OPTIONS.find((o) => o.value === v)?.label ?? v)
          .join('、')
      : '';
    const timeRange =
      group.cycleStartTime || group.cycleEndTime
        ? ` ${group.cycleStartTime ?? '00:00'}~${group.cycleEndTime ?? '23:59'}`
        : ' 全天';
    return `${cycleTypeLabel} ${dayLabels}${timeRange}`;
  }
  return '未知';
}

export default ValidConfigForm;

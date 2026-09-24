import React, { useEffect, useState } from "react";
import { Form, Select, Input, InputNumber } from "antd";
import Cron, { HEADER, cronstrue } from "react-cron-generator";
import "react-cron-generator/build/cron-builder.css";
import { SCHEDULE_TYPE_OPTIONS } from "../../types";
import styles from "./index.module.less";

interface StepScheduleProps {
  jobOptions: Array<{ label: string; value: string }>;
  mode: "create" | "edit";
}

const DEFAULT_CRON_EXPRESSION = "0 0/5 * * * ?";
const CRON_TRANSLATIONS: Record<string, string> = {
  Every: "每隔",
  "minute(s)": "分钟",
  hour: "小时",
  "day(s)": "天",
  "Every week day": "每个工作日",
  "Start time": "开始时间",
  At: "在",
  Monday: "星期一",
  Tuesday: "星期二",
  Wednesday: "星期三",
  Thursday: "星期四",
  Friday: "星期五",
  Saturday: "星期六",
  Sunday: "星期日",
  Day: "第",
  "of every month(s)": "天，每月执行",
  "Last day of every month": "每月最后一天",
  "On the last weekday of every month": "每月最后一个工作日",
  "day(s) before the end of the month": "天，于月底前执行",
  "Days of every month": "每月的这些天",
  Expression: "表达式",
  Minutes: "分钟",
  Hourly: "每小时",
  Daily: "每天",
  Weekly: "每周",
  Monthly: "每月",
  Custom: "自定义",
};
const CRON_HEADERS = [
  HEADER.MINUTES,
  HEADER.HOURLY,
  HEADER.DAILY,
  HEADER.WEEKLY,
  HEADER.MONTHLY,
  HEADER.CUSTOM,
] as const;
const CRON_OPTIONS = { headers: [...CRON_HEADERS] };
const translateCronText = (key: string) => CRON_TRANSLATIONS[key] ?? key;

const StepSchedule: React.FC<StepScheduleProps> = ({ jobOptions, mode }) => {
  const form = Form.useFormInstance();
  const scheduleType = Form.useWatch("scheduleType", form);
  const scheduleConf = Form.useWatch("scheduleConf", form);
  const [cronText, setCronText] = useState("");
  const hasCronValue = Boolean(scheduleConf?.trim());
  const shouldRenderCronBuilder = mode === "create" || hasCronValue;
  const cronValue =
    scheduleType === "CRON" && hasCronValue
      ? scheduleConf
      : DEFAULT_CRON_EXPRESSION;

  useEffect(() => {
    if (mode === "create" && scheduleType === "CRON" && !scheduleConf?.trim()) {
      form.setFieldValue("scheduleConf", DEFAULT_CRON_EXPRESSION);
    }
  }, [form, mode, scheduleConf, scheduleType]);

  useEffect(() => {
    if (scheduleType !== "CRON" || !cronValue.trim()) {
      setCronText("");
      return;
    }
    try {
      setCronText(
        cronstrue.toString(cronValue, {
          throwExceptionOnParseError: false,
          locale: "zh_CN",
        })
      );
    } catch {
      setCronText("");
    }
  }, [cronValue, scheduleType]);

  const handleCronBuilderChange = (value: string, _text: string) => {
    if (form.getFieldValue("scheduleConf") !== value) {
      form.setFieldValue("scheduleConf", value);
    }
  };

  return (
    <>
      <Form.Item
        name="scheduleType"
        label="调度类型"
        rules={[{ required: true, message: "请选择调度类型" }]}
        initialValue="CRON"
      >
        <Select
          options={[...SCHEDULE_TYPE_OPTIONS]}
          placeholder="请选择调度类型"
        />
      </Form.Item>
      {scheduleType === "CRON" && (
        <>
          <Form.Item
            name="scheduleConf"
            label="Cron表达式"
            rules={[{ required: true, message: "请输入Cron表达式" }]}
          >
            <Input
              placeholder="如: 0 0/5 * * * ?"
              onChange={() => setCronText("")}
            />
          </Form.Item>
          {shouldRenderCronBuilder && (
            <div className={styles.cronBuilderWrapper}>
              <Cron
                value={cronValue}
                onChange={handleCronBuilderChange}
                options={CRON_OPTIONS}
                locale="zh_CN"
                translateFn={translateCronText}
                showResultText={false}
                showResultCron={false}
                use6FieldQuartz
              />
              {cronText ? (
                <div className={styles.cronHelperText}>{cronText}</div>
              ) : null}
            </div>
          )}
        </>
      )}
      {scheduleType === "FIX_RATE" && (
        <Form.Item
          name="scheduleConf"
          label="间隔(秒)"
          rules={[{ required: true, message: "请输入间隔秒数" }]}
        >
          <InputNumber
            min={1}
            className={styles.fullWidth}
            placeholder="请输入间隔秒数"
          />
        </Form.Item>
      )}
      <Form.Item name="childJobId" label="子任务ID">
        <Select
          mode="multiple"
          placeholder="请选择子任务"
          options={jobOptions}
          showSearch
          optionFilterProp="label"
        />
      </Form.Item>
    </>
  );
};

export default StepSchedule;

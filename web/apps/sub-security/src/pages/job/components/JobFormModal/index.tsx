import React, { useState, useEffect, useCallback } from "react";
import { Modal, Form, Steps, Button, message } from "antd";
import type { JobInfo, JobInfoCreateDTO, JobMode } from "../../types";
import StepCommon from "../StepCommon";
import StepJobType from "../StepJobType";
import StepSchedule from "../StepSchedule";
import { getJobPage } from "../../services/job";
import {
  buildJobFormPayload,
  deriveJobMode,
  parseChildJobIds,
  parseUrlParams,
} from "../../utils";
import {
  DEFAULT_GLUE_INIT_REMARK,
  DEFAULT_GLUE_UPDATE_REMARK,
  normalizeGlueRemark,
} from "../../glue";
import styles from "./index.module.less";

interface JobFormModalProps {
  visible: boolean;
  mode: "create" | "edit";
  data: JobInfo | null;
  onSave: (data: JobInfoCreateDTO) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

interface JobFormValues extends Omit<JobInfoCreateDTO, "childJobId"> {
  childJobId?: string[];
}

const JobFormModal: React.FC<JobFormModalProps> = ({
  visible,
  mode,
  data,
  onSave,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm<JobFormValues>();
  const [current, setCurrent] = useState(0);
  const [jobMode, setJobMode] = useState<JobMode>("URL");
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [jobOptions, setJobOptions] = useState<
    Array<{ label: string; value: string }>
  >([]);

  useEffect(() => {
    if (!visible) {
      return;
    }
    getJobPage({ pageNum: 1, pageSize: 500 }).then((page) => {
      const currentId = data?.id;
      setJobOptions(
        (page?.records ?? [])
          .filter((item) => item.id && item.id !== currentId)
          .map((item) => ({
            label: `${item.name}${
              item.executorHandler ? ` (${item.executorHandler})` : ""
            }`,
            value: item.id!,
          }))
      );
    });
  }, [visible, data?.id]);

  useEffect(() => {
    if (visible) {
      setCurrent(0);
      if (mode === "edit" && data) {
        const derivedMode = deriveJobMode(data);
        setJobMode(derivedMode);
        const urlParams =
          derivedMode === "URL" ? parseUrlParams(data.executorParam) : {};
        form.setFieldsValue({
          ...data,
          jobMode: derivedMode,
          ...urlParams,
          executorParam: derivedMode === "URL" ? undefined : data.executorParam,
          childJobId: parseChildJobIds(data.childJobId),
        });
      } else {
        form.resetFields();
        setJobMode("URL");
        form.setFieldsValue({
          jobMode: "URL",
          misfireStrategy: "DO_NOTHING",
          executorRouteStrategy: "ROUND",
          executorBlockStrategy: "SERIAL_EXECUTION",
          executorTimeout: 0,
          executorFailRetryCount: 0,
          scheduleType: "CRON",
        });
      }
    }
  }, [visible, mode, data, form]);

  const steps = [
    { title: "公共配置" },
    { title: "任务类型" },
    { title: "调度配置" },
  ];

  const handleNext = useCallback(async () => {
    try {
      if (current === 1) {
        const m = form.getFieldValue("jobMode");
        if (!m) {
          message.warning("请选择任务模式");
          return;
        }
        if (m === "URL") {
          await form.validateFields(["prefix", "url"]);
        } else if (m === "BEAN") {
          await form.validateFields(["executorHandler"]);
        } else if (m === "GLUE") {
          await form.validateFields(["glueType", "glueSource"]);
        }
      } else {
        const commonFields = [
          "name",
          "misfireStrategy",
          "executorRouteStrategy",
          "executorBlockStrategy",
        ];
        await form.validateFields(commonFields);
      }
      setCurrent(current + 1);
    } catch {
      // validation failed
    }
  }, [current, form]);

  const handlePrev = useCallback(() => {
    setCurrent(current - 1);
  }, [current]);

  const handleFinish = useCallback(async () => {
    try {
      const values = await form.validateFields();
      setConfirmLoading(true);
      const payload = buildJobFormPayload(mode, values, data);
      if (payload.jobMode === "GLUE") {
        payload.glueRemark = normalizeGlueRemark(
          payload.glueRemark,
          mode === "create"
            ? DEFAULT_GLUE_INIT_REMARK
            : DEFAULT_GLUE_UPDATE_REMARK
        );
      }
      const success = await onSave(payload);
      if (success) {
        onSuccess();
      }
    } catch {
      // validation failed
    } finally {
      setConfirmLoading(false);
    }
  }, [data, form, mode, onSave, onSuccess]);

  const handleClose = useCallback(() => {
    form.resetFields();
    onClose();
  }, [form, onClose]);

  return (
    <Modal
      title={mode === "create" ? "新增任务" : "编辑任务"}
      open={visible}
      width={720}
      onCancel={handleClose}
      footer={null}
      destroyOnHidden
    >
      <Steps current={current} items={steps} className={styles.steps} />

      <Form form={form} layout="vertical" className={styles.formBody}>
        <div
          className={current === 0 ? styles.stepPanelActive : styles.stepPanel}
        >
          <StepCommon />
        </div>
        <div
          className={current === 1 ? styles.stepPanelActive : styles.stepPanel}
        >
          <StepJobType jobMode={jobMode} onJobModeChange={setJobMode} />
        </div>
        <div
          className={current === 2 ? styles.stepPanelActive : styles.stepPanel}
        >
          <StepSchedule jobOptions={jobOptions} mode={mode} />
        </div>
      </Form>

      <div className={styles.footer}>
        {current > 0 && (
          <Button className={styles.prevButton} onClick={handlePrev}>
            上一步
          </Button>
        )}
        {current < steps.length - 1 && (
          <Button type="primary" onClick={handleNext}>
            下一步
          </Button>
        )}
        {current === steps.length - 1 && (
          <Button
            type="primary"
            loading={confirmLoading}
            onClick={handleFinish}
            data-ai-approval
          >
            {mode === "create" ? "提交" : "保存"}
          </Button>
        )}
      </div>
    </Modal>
  );
};

export default JobFormModal;

import React, { useEffect, useState } from "react";
import { Empty, Modal, Spin, Tag, Typography } from "antd";
import { getNextTriggerTime } from "../../services/job";
import type { JobInfo } from "../../types";
import styles from "./index.module.less";

interface NextTriggerTimeModalProps {
  visible: boolean;
  job: JobInfo | null;
  onClose: () => void;
}

const NextTriggerTimeModal: React.FC<NextTriggerTimeModalProps> = ({
  visible,
  job,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [nextTimes, setNextTimes] = useState<string[]>([]);

  useEffect(() => {
    if (!visible || !job?.scheduleType || !job.scheduleConf) {
      setNextTimes([]);
      return;
    }
    let active = true;
    setLoading(true);
    getNextTriggerTime(job.scheduleType, job.scheduleConf)
      .then((times) => {
        if (active) {
          setNextTimes(times);
        }
      })
      .catch(() => {
        if (active) {
          setNextTimes([]);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [job, visible]);

  return (
    <Modal
      title="预估下次触发时间"
      open={visible}
      footer={null}
      onCancel={onClose}
      destroyOnHidden
    >
      <div className={styles.content}>
        {job ? (
          <Typography.Paragraph className={styles.description}>
            {job.name} / {job.scheduleType} / {job.scheduleConf}
          </Typography.Paragraph>
        ) : null}
        <Spin spinning={loading}>
          {nextTimes.length > 0 ? (
            <div className={styles.tagList}>
              {nextTimes.map((time) => (
                <Tag key={time} color="blue" className={styles.tag}>
                  {time}
                </Tag>
              ))}
            </div>
          ) : (
            <Empty description="暂无可预估的触发时间" />
          )}
        </Spin>
      </div>
    </Modal>
  );
};

export default NextTriggerTimeModal;

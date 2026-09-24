import React, { useState, useEffect } from 'react';
import { useProjectConfigStore } from '@gwsu/core';
import styles from './dashboard.module.less';

function useCurrentTime() {
  const [time, setTime] = useState(new Date());
  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);
  return time;
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
}

function formatDate(date: Date): string {
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' });
}

export default function Dashboard() {
  const now = useCurrentTime();
  const projectName = useProjectConfigStore((s) => s.projectName);

  return (
    <div className={styles.dashboard}>
      <div className={styles.heroGlow} />
      <div className={styles.heroGlowSecondary} />

      <div className={styles.heroClock}>
        <span className={styles.clockTime}>{formatTime(now)}</span>
        <span className={styles.clockDate}>{formatDate(now)}</span>
      </div>

      <div className={styles.heroCenter}>
        <h1 className={styles.brandName}>
          {projectName}
          <span className={styles.brandDots}>
            <span className={styles.dot}>.</span>
            <span className={styles.dot}>.</span>
            <span className={styles.dot}>.</span>
          </span>
        </h1>
        <div className={styles.brandDivider} />
        <p className={styles.brandTagline}>以 LLM 为中心的管理系统智能体</p>
      </div>
    </div>
  );
}

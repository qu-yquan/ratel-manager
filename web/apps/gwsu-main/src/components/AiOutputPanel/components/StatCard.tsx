import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  MinusOutlined,
} from '@ant-design/icons';
import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './StatCard.module.less';

interface StatCardProps {
  title: string;
  value: string;
  trend?: 'up' | 'down' | 'flat' | null;
  changeRate?: string | null;
  icon?: string | null;
}

const trendConfig = {
  up: { className: styles.trendUp, icon: <ArrowUpOutlined /> },
  down: { className: styles.trendDown, icon: <ArrowDownOutlined /> },
  flat: { className: styles.trendFlat, icon: <MinusOutlined /> },
};

const StatCard: React.FC<BaseComponentProps<StatCardProps>> = ({ props }) => {
  const trend = props.trend || 'flat';
  const config = trendConfig[trend];

  return (
    <div className={styles.statCard}>
      <div className={styles.title}>
        <KnowledgeCitationText text={props.title} />
      </div>
      <div className={styles.valueRow}>
        <span className={styles.value}>
          <KnowledgeCitationText text={props.value} />
        </span>
        {props.changeRate && (
          <span className={`${styles.trend} ${config.className}`}>
            {config.icon}
            <KnowledgeCitationText text={props.changeRate} />
          </span>
        )}
      </div>
    </div>
  );
};

export default StatCard;

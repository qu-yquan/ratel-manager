import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './Dashboard.module.less';

interface DashboardProps {
  title: string;
  description?: string | null;
}

const Dashboard: React.FC<BaseComponentProps<DashboardProps>> = ({
  props,
  children,
}) => {
  return (
    <div className={styles.dashboard}>
      {props.title && (
        <h1 className={styles.title}>
          <KnowledgeCitationText text={props.title} />
        </h1>
      )}
      {props.description && (
        <p className={styles.description}>
          <KnowledgeCitationText text={props.description} />
        </p>
      )}
      {children}
    </div>
  );
};

export default Dashboard;

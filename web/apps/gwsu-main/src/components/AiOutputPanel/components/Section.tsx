import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './Dashboard.module.less';

interface SectionProps {
  title?: string | null;
  description?: string | null;
  layout?: 'row' | 'column' | null;
}

const Section: React.FC<BaseComponentProps<SectionProps>> = ({
  props,
  children,
}) => {
  const isRow = props.layout === 'row';

  return (
    <div className={styles.section}>
      {props.title && (
        <h2 className={styles.sectionTitle}>
          <KnowledgeCitationText text={props.title} />
        </h2>
      )}
      {props.description && (
        <p className={styles.sectionDesc}>
          <KnowledgeCitationText text={props.description} />
        </p>
      )}
      <div className={isRow ? styles.sectionRow : styles.sectionColumn}>
        {children}
      </div>
    </div>
  );
};

export default Section;

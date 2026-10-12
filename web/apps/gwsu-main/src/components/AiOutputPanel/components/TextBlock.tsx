import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './TextBlock.module.less';

interface TextBlockProps {
  content: string;
  variant?: 'plain' | 'heading' | 'info' | 'warning' | 'error' | null;
}

const variantClass: Record<string, string> = {
  heading: styles.heading,
  info: styles.info,
  warning: styles.warning,
  error: styles.error,
};

const TextBlock: React.FC<BaseComponentProps<TextBlockProps>> = ({ props }) => {
  const variant = props.variant || 'plain';
  const className = variantClass[variant] || styles.textBlock;

  if (variant === 'heading') {
    return (
      <h3 className={className}>
        <KnowledgeCitationText text={props.content} />
      </h3>
    );
  }

  return (
    <div className={`${styles.textBlock} ${className}`}>
      <KnowledgeCitationText text={props.content} />
    </div>
  );
};

export default TextBlock;

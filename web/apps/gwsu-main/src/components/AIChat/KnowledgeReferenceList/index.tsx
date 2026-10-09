import { FileTextOutlined } from '@ant-design/icons';
import { useKnowledgeReferences } from '@/services/knowledge-reference';
import styles from './index.module.less';

interface KnowledgeReferenceListProps {
  messageId: string;
}

export function KnowledgeReferenceList({
  messageId,
}: KnowledgeReferenceListProps) {
  const references = useKnowledgeReferences(messageId);
  if (references.length === 0) return null;

  return (
    <section className={styles.container} aria-label="参考文件">
      <div className={styles.title}>参考文件（{references.length}）</div>
      <ul className={styles.list}>
        {references.map((reference) => (
          <li className={styles.item} key={reference.documentId}>
            <FileTextOutlined className={styles.icon} aria-hidden />
            <div className={styles.content}>
              <div className={styles.fileName} title={reference.fileName}>
                {reference.fileName}
              </div>
              <div className={styles.meta}>
                {reference.fileFormat?.toUpperCase() || '文档'}
                <span className={styles.separator}>·</span>
                {reference.chunkCount} 处引用
              </div>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

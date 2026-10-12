import { FileTextOutlined } from '@ant-design/icons';
import type { KnowledgeReference } from '@/services/knowledge-reference';
import styles from './index.module.less';

interface KnowledgeReferenceListProps {
  references: KnowledgeReference[];
  onOpenReference: (reference: KnowledgeReference) => void;
}

export function KnowledgeReferenceList({
  references,
  onOpenReference,
}: KnowledgeReferenceListProps) {
  if (references.length === 0) return null;

  return (
    <section className={styles.container} aria-label="参考文件">
      <div className={styles.title}>参考文件（{references.length}）</div>
      <ul className={styles.list}>
        {references.map((reference) => (
          <li className={styles.item} key={reference.documentId}>
            <button
              type="button"
              className={styles.referenceButton}
              aria-label={`预览引用文档：${reference.fileName}`}
              onClick={() => onOpenReference(reference)}
            >
              <FileTextOutlined className={styles.icon} aria-hidden="true" />
              <div className={styles.content}>
                <div className={styles.fileName} title={reference.fileName}>
                  {reference.fileName}
                </div>
                <div className={styles.meta}>
                  {reference.fileFormat?.toUpperCase() || '文档'}
                  <span className={styles.separator}>·</span>
                  {reference.chunkCount} 处
                </div>
              </div>
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

import React, { useState, useCallback } from 'react';
import { Segmented } from 'antd';
import MarkdownPreview from '../MarkdownPreview';
import styles from './index.module.less';

interface MarkdownEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  minHeight?: number;
}

const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value = '',
  onChange,
  placeholder,
  minHeight = 300,
}) => {
  const [mdMode, setMdMode] = useState<'edit' | 'preview' | 'split'>('edit');

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange?.(e.target.value);
    },
    [onChange],
  );

  return (
    <div className={styles.mdEditor}>
      <div className={styles.mdToolbar}>
        <Segmented
          size="small"
          options={[
            { label: '编辑', value: 'edit' },
            { label: '预览', value: 'preview' },
            { label: '分屏', value: 'split' },
          ]}
          value={mdMode}
          onChange={(val) => setMdMode(val as 'edit' | 'preview' | 'split')}
        />
      </div>
      <div className={styles.mdContent} style={{ minHeight }}>
        {(mdMode === 'edit' || mdMode === 'split') && (
          <textarea
            className={styles.mdEditArea}
            value={value}
            onChange={handleChange}
            placeholder={placeholder}
            style={{ width: mdMode === 'split' ? '50%' : '100%' }}
          />
        )}
        {(mdMode === 'preview' || mdMode === 'split') && (
          <MarkdownPreview
            className={styles.mdPreview}
            content={value}
            emptyText={placeholder ?? '请输入 Markdown 内容'}
            style={{ width: mdMode === 'split' ? '50%' : '100%' }}
          />
        )}
      </div>
    </div>
  );
};

export default MarkdownEditor;

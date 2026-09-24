import React, { useMemo, useState } from 'react';
import { Button, Modal } from 'antd';
import { FullscreenOutlined, FullscreenExitOutlined } from '@ant-design/icons';
import CodeMirror from '@uiw/react-codemirror';
import { java } from '@codemirror/lang-java';
import { javascript } from '@codemirror/lang-javascript';
import { php } from '@codemirror/lang-php';
import { python } from '@codemirror/lang-python';
import { StreamLanguage } from '@codemirror/language';
import { shell } from '@codemirror/legacy-modes/mode/shell';
import { powerShell } from '@codemirror/legacy-modes/mode/powershell';
import type { Extension } from '@codemirror/state';
import { getGlueEditorLanguage } from '../../glue';
import styles from './index.module.less';

interface GlueCodeEditorProps {
  glueType?: string;
  value?: string;
  onChange?: (value: string) => void;
  height?: string;
  fullscreenTitle?: string;
  readOnly?: boolean;
}

function getExtensions(glueType?: string): Extension[] {
  switch (getGlueEditorLanguage(glueType)) {
    case 'shell':
      return [StreamLanguage.define(shell)];
    case 'python':
      return [python()];
    case 'javascript':
      return [javascript()];
    case 'php':
      return [php()];
    case 'powershell':
      return [StreamLanguage.define(powerShell)];
    case 'java':
    default:
      return [java()];
  }
}

const GlueCodeEditor: React.FC<GlueCodeEditorProps> = ({
  glueType,
  value = '',
  onChange,
  height = '320px',
  fullscreenTitle = '脚本代码',
  readOnly = false,
}) => {
  const [fullscreenOpen, setFullscreenOpen] = useState(false);
  const extensions = useMemo(() => getExtensions(glueType), [glueType]);
  const isAiFixedMode = typeof document !== 'undefined' && document.body.dataset.aiMode === 'fixed';
  const fullscreenWidth = isAiFixedMode ? 'calc(100vw - 468px)' : '92vw';
  const languageLabel = useMemo(() => {
    switch (getGlueEditorLanguage(glueType)) {
      case 'shell':
        return 'Shell';
      case 'python':
        return 'Python';
      case 'javascript':
        return 'JavaScript';
      case 'php':
        return 'PHP';
      case 'powershell':
        return 'PowerShell';
      case 'java':
      default:
        return 'Java';
    }
  }, [glueType]);

  const editor = (
    <CodeMirror
      value={value}
      height={height}
      extensions={extensions}
      basicSetup={{
        foldGutter: true,
        lineNumbers: true,
        highlightActiveLine: true,
        autocompletion: true,
      }}
      editable={!readOnly}
      onChange={(nextValue) => onChange?.(nextValue)}
    />
  );

  return (
    <>
      <div className={styles.editor}>
        <div className={styles.toolbar}>
          <span className={styles.language}>语言模式：{languageLabel}</span>
          <div className={styles.toolbarRight}>
            <span className={styles.hint}>{readOnly ? '当前为只读预览模式' : '切换 GLUE 类型时会自动填充示例模板'}</span>
            <Button
              type="text"
              size="small"
              icon={<FullscreenOutlined aria-hidden="true" />}
              onClick={() => setFullscreenOpen(true)}
              aria-label={readOnly ? "全屏查看脚本代码" : "全屏编辑脚本代码"}
            >
              全屏
            </Button>
          </div>
        </div>
        <div className={styles.instance}>
          {editor}
        </div>
      </div>
      <Modal
        title={fullscreenTitle}
        open={fullscreenOpen}
        footer={null}
        width={fullscreenWidth}
        style={{ top: 24 }}
        onCancel={() => setFullscreenOpen(false)}
        destroyOnHidden
      >
        <div className={styles.fullscreenBody}>
          <div className={styles.editor}>
            <div className={styles.toolbar}>
              <span className={styles.language}>语言模式：{languageLabel}</span>
              <div className={styles.toolbarRight}>
                <span className={styles.hint}>{readOnly ? '全屏模式下仅支持查看历史代码' : '全屏模式下可直接编辑并实时回写'}</span>
                <Button
                  type="text"
                  size="small"
                  icon={<FullscreenExitOutlined aria-hidden="true" />}
                  onClick={() => setFullscreenOpen(false)}
                  aria-label={readOnly ? "退出全屏查看" : "退出全屏编辑"}
                >
                  退出全屏
                </Button>
              </div>
            </div>
            <div className={styles.instance}>
              <CodeMirror
                value={value}
                height="68vh"
                extensions={extensions}
                basicSetup={{
                  foldGutter: true,
                  lineNumbers: true,
                  highlightActiveLine: true,
                  autocompletion: true,
                }}
                editable={!readOnly}
                onChange={(nextValue) => onChange?.(nextValue)}
              />
            </div>
          </div>
        </div>
      </Modal>
    </>
  );
};

export default GlueCodeEditor;

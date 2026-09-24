import React, { useMemo } from 'react';
import styles from './index.module.less';

interface MarkdownPreviewProps {
  content?: string;
  className?: string;
  emptyText?: string;
  style?: React.CSSProperties;
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderInlineMarkdown(content: string): string {
  return content
    .replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+"([^"]+)")?\)/g, (_, alt, src, title) => {
      const safeAlt = escapeHtml(alt);
      const safeSrc = escapeHtml(src);
      const titleAttr = title ? ` title="${escapeHtml(title)}"` : '';
      return `<img src="${safeSrc}" alt="${safeAlt}"${titleAttr} />`;
    })
    .replace(/\[([^\]]+)\]\(([^)\s]+)(?:\s+"([^"]+)")?\)/g, (_, text, href, title) => {
      const safeText = escapeHtml(text);
      const safeHref = escapeHtml(href);
      const titleAttr = title ? ` title="${escapeHtml(title)}"` : '';
      return `<a href="${safeHref}" target="_blank" rel="noreferrer"${titleAttr}>${safeText}</a>`;
    })
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>');
}

function renderMarkdown(content: string): string {
  const lines = escapeHtml(content).split('\n');
  const html: string[] = [];
  let inList = false;

  const closeListIfNeeded = () => {
    if (inList) {
      html.push('</ul>');
      inList = false;
    }
  };

  lines.forEach((line) => {
    if (!line.trim()) {
      closeListIfNeeded();
      return;
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      closeListIfNeeded();
      const level = heading[1].length;
      html.push(`<h${level}>${renderInlineMarkdown(heading[2])}</h${level}>`);
      return;
    }

    const blockquote = line.match(/^&gt;\s+(.+)$/);
    if (blockquote) {
      closeListIfNeeded();
      html.push(`<blockquote>${renderInlineMarkdown(blockquote[1])}</blockquote>`);
      return;
    }

    const listItem = line.match(/^\s*[-*]\s+(.+)$/);
    if (listItem) {
      if (!inList) {
        html.push('<ul>');
        inList = true;
      }
      html.push(`<li>${renderInlineMarkdown(listItem[1])}</li>`);
      return;
    }

    closeListIfNeeded();
    html.push(`<p>${renderInlineMarkdown(line)}</p>`);
  });

  closeListIfNeeded();
  return html.join('');
}

const MarkdownPreview: React.FC<MarkdownPreviewProps> = ({
  content = '',
  className,
  emptyText = '暂无内容',
  style,
}) => {
  const html = useMemo(() => renderMarkdown(content), [content]);
  const classes = [styles.markdownPreview, className].filter(Boolean).join(' ');

  if (!content.trim()) {
    return (
      <div className={classes} style={style}>
        {emptyText}
      </div>
    );
  }

  return (
    <div
      className={classes}
      style={style}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
};

export default MarkdownPreview;

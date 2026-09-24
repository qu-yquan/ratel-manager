import React, { useEffect, useMemo, useState } from 'react';
import { Button, Descriptions, Empty, Form, Input, List, Modal, Radio, Segmented, Select, Space, Tag, TreeSelect, Typography, message } from 'antd';
import type { TreeSelectProps } from 'antd';
import { useUserStore } from '@gwsu/core';
import MarkdownPreview from '../../../../components/MarkdownPreview';
import { getRoleList } from '../../../role/services/role';
import type { RoleInfo } from '../../../role/types';
import { debugSearch, getAdjacentKnowledgeChunks, getDirectoryTree } from '../../services/knowledge';
import type { KnowledgeNode, KnowledgeSearchResult } from '../../types';
import styles from './index.module.less';

interface Props { open: boolean; onClose: () => void }
type Mode = 'DIRECTORY' | 'ROLE';
type Direction = 'PREVIOUS' | 'NEXT';

function stripHighlight(content: string): string {
  return content.replace(/<\/?mark>/g, '');
}

function stripDuplicateTitle(content: string, title?: string): string {
  if (!title) return content;
  const lines = content.split('\n');
  const first = lines.findIndex((line) => line.trim());
  if (first < 0 || lines[first].trim().replace(/^#\s+/, '').trim().toLowerCase() !== title.trim().toLowerCase()
      || !/^#\s+/.test(lines[first].trim())) return content;
  return lines.slice(first + 1).join('\n').trimStart();
}

function directoryTree(nodes: KnowledgeNode[], parentId?: string): NonNullable<TreeSelectProps['treeData']> {
  return nodes.filter((node) => (node.parentId ?? undefined) === parentId).map((node) => ({
    title: node.name,
    value: node.id,
    selectable: node.canSearch === true,
    children: directoryTree(nodes, node.id),
  }));
}

const SearchDebugModal: React.FC<Props> = ({ open, onClose }) => {
  const isAdmin = useUserStore((state) => state.userInfo?.admin === true || state.userInfo?.roles?.includes('super_admin') === true);
  const [mode, setMode] = useState<Mode>('DIRECTORY');
  const [directoryId, setDirectoryId] = useState<string>();
  const [roleCode, setRoleCode] = useState<string>();
  const [keyword, setKeyword] = useState('');
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [directories, setDirectories] = useState<KnowledgeNode[]>([]);
  const [results, setResults] = useState<KnowledgeSearchResult[]>([]);
  const [selectedResult, setSelectedResult] = useState<KnowledgeSearchResult>();
  const [detailBlocks, setDetailBlocks] = useState<KnowledgeSearchResult[]>([]);
  const [detailMode, setDetailMode] = useState<'preview' | 'markdown'>('preview');
  const [boundaryReached, setBoundaryReached] = useState<Record<Direction, boolean>>({ PREVIOUS: false, NEXT: false });
  const [adjacentLoading, setAdjacentLoading] = useState<Direction>();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    void Promise.all([isAdmin ? getRoleList(1) : Promise.resolve([]), getDirectoryTree()]).then(([roleList, directoryTree]) => {
      setRoles(roleList);
      setDirectories(directoryTree);
    });
  }, [open, isAdmin]);
  const directoryTreeData = useMemo<NonNullable<TreeSelectProps['treeData']>>(() => [{
    title: '全部有权限的文档', value: 'ROOT', children: directoryTree(directories),
  }], [directories]);
  const resetDetail = () => {
    setSelectedResult(undefined);
    setDetailBlocks([]);
    setBoundaryReached({ PREVIOUS: false, NEXT: false });
  };
  const detailMarkdown = detailBlocks.map((block) => stripHighlight(block.content)).join('\n\n');

  const search = async () => {
    if (!keyword.trim() || (mode === 'DIRECTORY' ? !directoryId : !roleCode)) {
      message.warning('请输入关键词并选择目录或角色');
      return;
    }
    setLoading(true);
    try {
      resetDetail();
      setResults(await debugSearch({ keyword: keyword.trim(), size: 20, mode, directoryId, roleCode }));
    } finally { setLoading(false); }
  };

  const inspectAdjacent = async (direction: Direction) => {
    const boundary = direction === 'PREVIOUS' ? detailBlocks[0] : detailBlocks[detailBlocks.length - 1];
    if (!boundary?.pageBlockId) return;
    setAdjacentLoading(direction);
    try {
      const neighbors = await getAdjacentKnowledgeChunks({
        pageBlockId: boundary.pageBlockId, direction, mode, directoryId, roleCode,
      });
      if (!neighbors.length) {
        setBoundaryReached((current) => ({ ...current, [direction]: true }));
        message.info(direction === 'PREVIOUS' ? '已经是第一段内容' : '已经是最后一段内容');
        return;
      }
      setDetailBlocks((current) => {
        const ids = new Set(current.map((item) => item.pageBlockId));
        const unique = neighbors.filter((item) => !item.pageBlockId || !ids.has(item.pageBlockId));
        return direction === 'PREVIOUS' ? [...unique.reverse(), ...current] : [...current, ...unique];
      });
    } finally { setAdjacentLoading(undefined); }
  };

  return <Modal title="检索调试" open={open} onCancel={onClose} footer={null} width={900} destroyOnHidden>
    <Form layout="vertical">
      <Form.Item label="测试范围"><Radio.Group value={mode} onChange={(event) => { setMode(event.target.value); setResults([]); resetDetail(); }}
        options={[{ label: '按目录', value: 'DIRECTORY' }, ...(isAdmin ? [{ label: '按角色', value: 'ROLE' }] : [])]} /></Form.Item>
      <Form.Item label={mode === 'DIRECTORY' ? '目录及其子目录' : '角色授权范围'}>
        {mode === 'DIRECTORY' ? <TreeSelect className={styles.directorySelect} value={directoryId}
          onChange={(value) => { setDirectoryId(value); setResults([]); resetDetail(); }}
          treeData={directoryTreeData} treeDefaultExpandAll showSearch treeNodeFilterProp="title" placeholder="选择目录" />
          : <Select value={roleCode} onChange={(value) => { setRoleCode(value); setResults([]); resetDetail(); }} showSearch optionFilterProp="label"
          options={roles.map((role) => ({ label: `${role.roleName} (${role.roleCode})`, value: role.roleCode }))} placeholder="选择角色" />}
      </Form.Item>
      <Form.Item label="检索词"><Space.Compact className={styles.searchLine}>
        <Input value={keyword} onChange={(event) => setKeyword(event.target.value)} onPressEnter={() => void search()} placeholder="请输入检索词" />
        <Button type="primary" loading={loading} onClick={() => void search()}>检索</Button>
      </Space.Compact></Form.Item>
    </Form>
    {selectedResult ? <div className={styles.detail}>
      <div className={styles.detailHeader}>
        <div><Typography.Text strong>{selectedResult.title ?? selectedResult.sourceDocumentId}</Typography.Text>
          {selectedResult.headingPath && <div><Typography.Text type="secondary">{selectedResult.headingPath}</Typography.Text></div>}</div>
        <Space wrap><Button onClick={resetDetail}>返回列表</Button>
          <Button loading={adjacentLoading === 'PREVIOUS'} disabled={boundaryReached.PREVIOUS || !detailBlocks[0]?.pageBlockId}
            onClick={() => void inspectAdjacent('PREVIOUS')}>加载上一段</Button>
          <Button loading={adjacentLoading === 'NEXT'} disabled={boundaryReached.NEXT || !detailBlocks[detailBlocks.length - 1]?.pageBlockId}
            onClick={() => void inspectAdjacent('NEXT')}>加载下一段</Button></Space>
      </div>
      <Descriptions bordered size="small" column={2}>
        <Descriptions.Item label="Page ID">{selectedResult.pageId ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="命中 Block">{selectedResult.pageBlockId ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="源文档ID">{selectedResult.sourceDocumentId}</Descriptions.Item>
        <Descriptions.Item label="Block 序号">{selectedResult.blockOrder ?? '-'}</Descriptions.Item>
      </Descriptions>
      <div className={styles.detailToolbar}><Segmented size="small" value={detailMode}
        options={[{ label: '预览', value: 'preview' }, { label: 'MD 原文', value: 'markdown' }]}
        onChange={(value) => setDetailMode(value as 'preview' | 'markdown')} /></div>
      {detailMode === 'preview' ? <MarkdownPreview className={styles.preview}
        content={stripDuplicateTitle(detailMarkdown, selectedResult.title)} />
        : <Input.TextArea className={styles.source} value={detailMarkdown || '暂无内容'} readOnly autoSize={{ minRows: 18, maxRows: 30 }} />}
    </div> : results.length ? <List className={styles.results} dataSource={results} renderItem={(item) => <List.Item>
      <button type="button" className={styles.resultButton} onClick={() => {
        setSelectedResult(item); setDetailBlocks([{ ...item, content: stripHighlight(item.content) }]);
        setDetailMode('preview'); setBoundaryReached({ PREVIOUS: false, NEXT: false });
      }}><div><Typography.Text strong>{item.title ?? item.sourceDocumentId}</Typography.Text>
        {typeof item.score === 'number' && <Tag color="blue">score {item.score.toFixed(4)}</Tag>}
        {typeof item.blockOrder === 'number' && <Tag>Block #{item.blockOrder}</Tag>}
        {typeof item.chunkOrder === 'number' && <Tag>Chunk #{item.chunkOrder}</Tag>}</div>
        {item.headingPath && <Typography.Text type="secondary">{item.headingPath}</Typography.Text>}
        <div className={styles.content}>{stripHighlight(item.content)}</div>
        <Typography.Text type="secondary">pageBlockId：{item.pageBlockId ?? '-'} · sourceDocumentId：{item.sourceDocumentId}</Typography.Text>
      </button></List.Item>} /> : <Empty description="暂无检索结果" />}
  </Modal>;
};

export default SearchDebugModal;

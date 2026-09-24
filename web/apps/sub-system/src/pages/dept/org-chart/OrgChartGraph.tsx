import React, { useEffect, useRef } from 'react';
import { Graph, NodeEvent } from '@antv/g6';
import type { DeptTreeNode } from '../types';

interface OrgChartGraphProps {
  data: DeptTreeNode[];
  onNodeDoubleClick: (deptId: number) => void;
}

const OrgChartGraph: React.FC<OrgChartGraphProps> = ({ data, onNodeDoubleClick }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);

  useEffect(() => {
    if (!containerRef.current || data.length === 0) return;

    // 将树形数据展开为扁平的 nodes + edges
    const nodes: { id: string; data: Record<string, unknown> }[] = [];
    const edges: { id: string; source: string; target: string }[] = [];

    const walk = (items: DeptTreeNode[], parentId?: string) => {
      for (const item of items) {
        nodes.push({
          id: String(item.id),
          data: { label: item.name, type: item.type },
        });
        if (parentId) {
          edges.push({
            id: `edge-${parentId}-${item.id}`,
            source: parentId,
            target: String(item.id),
          });
        }
        if (item.children?.length) {
          walk(item.children, String(item.id));
        }
      }
    };

    // 添加虚拟根节点
    nodes.push({ id: 'root', data: { label: '组织架构', type: 0 } });
    walk(data, 'root');

    if (graphRef.current) {
      graphRef.current.destroy();
    }

    const width = containerRef.current.offsetWidth;
    const height = containerRef.current.offsetHeight;

    const graph = new Graph({
      container: containerRef.current,
      width,
      height,
      autoResize: true,
      padding: 20,
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
      node: {
        type: 'rect',
        style: {
          size: [140, 36],
          fill: '#fff',
          stroke: '#1677ff',
          lineWidth: 1,
          radius: 4,
          labelText: (d: Record<string, any>) => (d.data?.label as string) || '',
          labelPlacement: 'center',
          labelFontSize: 12,
          labelFill: '#333',
        },
      },
      edge: {
        type: 'cubic-vertical',
        style: {
          stroke: '#999',
          lineWidth: 1,
        },
      },
      layout: {
        type: 'dagre',
        rankdir: 'TB',
        nodesep: 30,
        ranksep: 60,
      },
      data: { nodes, edges },
    });

    graph.render().then(() => {
      void graph.fitView();
    });

    graphRef.current = graph;

    graph.on(NodeEvent.DBLCLICK, (e: { target: { id: string } }) => {
      const nodeId = e.target?.id;
      if (nodeId && nodeId !== 'root') {
        onNodeDoubleClick(Number(nodeId));
      }
    });

    const handleResize = () => {
      if (graphRef.current && containerRef.current) {
        graphRef.current.resize(
          containerRef.current.offsetWidth,
          containerRef.current.offsetHeight,
        );
        void graphRef.current.fitView();
      }
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (graphRef.current) {
        graphRef.current.destroy();
        graphRef.current = null;
      }
    };
  }, [data, onNodeDoubleClick]);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
};

export default OrgChartGraph;

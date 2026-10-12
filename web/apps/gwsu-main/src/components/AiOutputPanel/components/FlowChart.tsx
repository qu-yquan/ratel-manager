import { useRef, useEffect, useMemo, useCallback } from 'react';
import * as echarts from 'echarts/core';
import { GraphChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import type { EChartsOption } from 'echarts';
import styles from './FlowChart.module.less';

echarts.use([GraphChart, TitleComponent, TooltipComponent, CanvasRenderer]);

interface FlowNode {
  id: string;
  label: string;
  type: 'start' | 'process' | 'decision' | 'end';
}

interface FlowEdge {
  from: string;
  to: string;
  label?: string | null;
}

interface FlowChartProps {
  title?: string | null;
  direction?: 'vertical' | 'horizontal' | null;
  nodes: FlowNode[];
  edges: FlowEdge[];
}

const nodeStyles: Record<
  string,
  { symbol: string; color: string; borderColor: string }
> = {
  start: { symbol: 'circle', color: '#16a34a', borderColor: '#16a34a' },
  process: { symbol: 'roundRect', color: '#ffffff', borderColor: '#1a5fb4' },
  decision: { symbol: 'diamond', color: '#fffbeb', borderColor: '#f59e0b' },
  end: { symbol: 'circle', color: '#dc2626', borderColor: '#dc2626' },
};

/** 主轴层间距 */
const LAYER_GAP = 140;
/** 同层节点间距 */
const SIBLING_GAP = 240;

/**
 * 基于图拓扑的分层布局（Sugiyama 简化版）
 * 1. 按最长路径计算每个节点的层级（rank），保证所有边从低层指向高层
 * 2. 同层节点水平展开，居中对齐
 */
function calculateNodePositions(
  nodes: FlowNode[],
  edges: FlowEdge[],
  direction: 'vertical' | 'horizontal',
): {
  positions: Record<string, [number, number]>;
  maxMain: number;
  maxCross: number;
} {
  if (nodes.length === 0) {
    return { positions: {}, maxMain: 0, maxCross: 0 };
  }

  const isVertical = direction === 'vertical';

  // 构建邻接表：记录每个节点的直接前驱
  const nodeIds = new Set(nodes.map((n) => n.id));
  const predecessors = new Map<string, string[]>();
  nodeIds.forEach((id) => predecessors.set(id, []));
  edges.forEach((edge) => {
    if (nodeIds.has(edge.from) && nodeIds.has(edge.to)) {
      predecessors.get(edge.to)!.push(edge.from);
    }
  });

  // 按最长路径计算 rank：rank[node] = max(rank[predecessor]) + 1
  // 根节点（无前驱）rank = 0
  const rank = new Map<string, number>();

  // 拓扑排序（Kahn 算法）
  const inDegree = new Map<string, number>();
  nodeIds.forEach((id) => inDegree.set(id, 0));
  edges.forEach((edge) => {
    if (nodeIds.has(edge.from) && nodeIds.has(edge.to)) {
      inDegree.set(edge.to, (inDegree.get(edge.to) || 0) + 1);
    }
  });

  const queue: string[] = [];
  inDegree.forEach((deg, id) => {
    if (deg === 0) queue.push(id);
  });

  // 按拓扑序处理，逐层推进
  const sorted: string[] = [];
  while (queue.length > 0) {
    const id = queue.shift()!;
    sorted.push(id);

    // 计算 rank：所有前驱的最大 rank + 1
    const preds = predecessors.get(id) || [];
    const maxPredRank =
      preds.length > 0 ? Math.max(...preds.map((p) => rank.get(p) ?? 0)) : -1;
    rank.set(id, maxPredRank + 1);

    // 更新后继的入度
    edges.forEach((edge) => {
      if (edge.from === id && nodeIds.has(edge.to)) {
        const newDeg = (inDegree.get(edge.to) || 1) - 1;
        inDegree.set(edge.to, newDeg);
        if (newDeg === 0) queue.push(edge.to);
      }
    });
  }

  // 处理环中的节点（拓扑排序未覆盖的节点）
  nodeIds.forEach((id) => {
    if (!rank.has(id)) {
      const preds = predecessors.get(id) || [];
      const maxPredRank =
        preds.length > 0 ? Math.max(...preds.map((p) => rank.get(p) ?? 0)) : -1;
      rank.set(id, maxPredRank + 1);
    }
  });

  // 按层分组
  const layers = new Map<number, string[]>();
  rank.forEach((r, id) => {
    if (!layers.has(r)) layers.set(r, []);
    layers.get(r)!.push(id);
  });

  // 计算最大层宽，用于居中对齐
  let maxLayerWidth = 0;
  layers.forEach((layerNodes) => {
    maxLayerWidth = Math.max(maxLayerWidth, layerNodes.length);
  });

  // 为每个节点分配坐标
  const positions: Record<string, [number, number]> = {};

  layers.forEach((layerNodes, r) => {
    const layerWidth = layerNodes.length;
    // 居中对齐：第一个节点的 x 偏移量
    const startX = -((layerWidth - 1) * SIBLING_GAP) / 2;

    layerNodes.forEach((id, i) => {
      const crossPos = startX + i * SIBLING_GAP;
      const mainPos = r * LAYER_GAP;
      positions[id] = isVertical ? [crossPos, mainPos] : [mainPos, crossPos];
    });
  });

  // 计算边界
  let maxMain = 0;
  let maxCross = 0;
  Object.values(positions).forEach(([x, y]) => {
    if (isVertical) {
      maxMain = Math.max(maxMain, y);
      maxCross = Math.max(maxCross, Math.abs(x));
    } else {
      maxMain = Math.max(maxMain, x);
      maxCross = Math.max(maxCross, Math.abs(y));
    }
  });

  return { positions, maxMain, maxCross };
}

function buildFlowOption(props: FlowChartProps): EChartsOption {
  const direction =
    props.direction === 'horizontal' ? 'horizontal' : 'vertical';
  const { positions } = calculateNodePositions(
    props.nodes,
    props.edges,
    direction,
  );

  return {
    tooltip: {},
    animation: true,
    series: [
      {
        type: 'graph',
        layout: 'none',
        roam: false,
        label: {
          show: true,
          fontSize: 12,
          color: '#374151',
        },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 8],
        edgeLabel: {
          show: true,
          fontSize: 11,
          color: '#6b7280',
        },
        lineStyle: {
          color: '#9ca3af',
          width: 1.5,
          curveness: 0.2,
        },
        data: props.nodes.map((node) => {
          const style = nodeStyles[node.type] || nodeStyles.process;
          return {
            name: node.id,
            x: positions[node.id][0],
            y: positions[node.id][1],
            label: { formatter: node.label },
            symbol: style.symbol,
            symbolSize:
              node.type === 'decision'
                ? [60, 40]
                : node.type === 'start' || node.type === 'end'
                ? 50
                : [80, 36],
            itemStyle: {
              color: style.color,
              borderColor: style.borderColor,
              borderWidth: 2,
            },
          };
        }),
        links: props.edges.map((edge) => {
          const nodeIndexMap = new Map(props.nodes.map((n, i) => [n.id, i]));
          const sourceIdx = nodeIndexMap.get(edge.from);
          const targetIdx = nodeIndexMap.get(edge.to);
          const isBackEdge =
            sourceIdx !== undefined &&
            targetIdx !== undefined &&
            targetIdx < sourceIdx;

          return {
            source: edge.from,
            target: edge.to,
            value: edge.label ? 1 : undefined,
            edgeLabel: edge.label ? { formatter: edge.label } : undefined,
            lineStyle: {
              curveness: isBackEdge ? 0.3 : 0.1,
            },
          };
        }),
      },
    ],
  };
}

const FlowChart: React.FC<BaseComponentProps<FlowChartProps>> = ({ props }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);
  const resizeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const direction =
    props.direction === 'horizontal' ? 'horizontal' : 'vertical';

  const { maxMain, maxCross } = useMemo(
    () =>
      calculateNodePositions(props.nodes || [], props.edges || [], direction),
    [props.nodes, props.edges, direction],
  );

  const isVertical = direction === 'vertical';
  const wrapperStyle = isVertical
    ? { height: Math.max(300, maxMain + 100) }
    : { height: Math.max(300, maxCross + 160), minHeight: 300 };

  const handleResize = useCallback(() => {
    if (resizeTimerRef.current) clearTimeout(resizeTimerRef.current);
    resizeTimerRef.current = setTimeout(() => {
      instanceRef.current?.resize();
    }, 100);
  }, []);

  useEffect(() => {
    if (!chartRef.current) return;

    const instance = instanceRef.current || echarts.init(chartRef.current);
    instanceRef.current = instance;
    instance.setOption(buildFlowOption(props));

    const observer = new ResizeObserver(() => {
      handleResize();
    });
    observer.observe(chartRef.current);

    return () => {
      observer.disconnect();
      if (resizeTimerRef.current) clearTimeout(resizeTimerRef.current);
    };
  }, [props, handleResize]);

  useEffect(() => {
    return () => {
      instanceRef.current?.dispose();
      instanceRef.current = null;
    };
  }, []);

  return (
    <div className={styles.flowContainer}>
      {props.title && (
        <div className={styles.flowTitle}>
          <KnowledgeCitationText text={props.title} />
        </div>
      )}
      <div ref={chartRef} className={styles.flowWrapper} style={wrapperStyle} />
    </div>
  );
};

export default FlowChart;

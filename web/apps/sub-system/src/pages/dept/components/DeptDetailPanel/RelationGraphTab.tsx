import React, {useEffect, useRef} from 'react';
import {Graph, NodeEvent} from '@antv/g6';
import {getDeptChildren, getDeptDetail} from '@/services/dept';
import type {DeptDetail} from '../../types';

interface RelationGraphTabProps {
    dept: DeptDetail;
    onNodeClick: (deptId: number) => void;
}

const NODE_COLORS: Record<string, { fill: string; stroke: string }> = {
    current: {fill: '#e6f4ff', stroke: '#1677ff'},
    parent: {fill: '#f6ffed', stroke: '#52c41a'},
    extraParent: {fill: '#fff7e6', stroke: '#faad14'},
    child: {fill: '#fff', stroke: '#13c2c2'},
    default: {fill: '#fff', stroke: '#1677ff'},
};

const RelationGraphTab: React.FC<RelationGraphTabProps> = ({dept, onNodeClick}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const graphRef = useRef<Graph | null>(null);

    useEffect(() => {
        if (!containerRef.current) return;

        const buildGraphData = async () => {
            const nodes: { id: string; label: string; nodeType: string }[] = [];
            const edges: { source: string; target: string; style?: Record<string, unknown> }[] = [];

            nodes.push({
                id: String(dept.id),
                label: dept.name,
                nodeType: 'current',
            });

            if (dept.parentId) {
                try {
                    const parent = await getDeptDetail(dept.parentId);
                    nodes.push({
                        id: String(parent.id),
                        label: parent.name,
                        nodeType: 'parent',
                    });
                    edges.push({
                        source: String(parent.id),
                        target: String(dept.id),
                    });
                } catch {
                    // ignore
                }
            }

            if (dept.extraParents && dept.extraParents.length > 0) {
                dept.extraParents.forEach((p) => {
                    nodes.push({
                        id: String(p.id),
                        label: p.name,
                        nodeType: 'extraParent',
                    });
                    edges.push({
                        source: String(p.id),
                        target: String(dept.id),
                        style: {stroke: '#faad14', lineDash: [4, 4]},
                    });
                });
            }

            try {
                const children = await getDeptChildren(dept.id);
                children.forEach((child) => {
                    nodes.push({
                        id: String(child.id),
                        label: child.name,
                        nodeType: 'child',
                    });
                    edges.push({
                        source: String(dept.id),
                        target: String(child.id),
                    });
                });
            } catch {
                // ignore
            }

            return {nodes, edges};
        };

        buildGraphData().then((data) => {
            if (!containerRef.current || !data) return;

            if (graphRef.current) {
                graphRef.current.destroy();
            }

            const width = containerRef.current.offsetWidth;
            const graph = new Graph({
                container: containerRef.current,
                width,
                height: 400,
                autoResize: true,
                padding: 20,
                behaviors: ['drag-canvas', 'zoom-canvas'],
                layout: {
                    type: 'dagre',
                    rankdir: 'TB',
                    nodesep: 30,
                    ranksep: 60,
                },
                node: {
                    type: 'rect',
                    style: (d) => {
                        const nodeType = (d.data?.nodeType as string) || 'default';
                        const colors = NODE_COLORS[nodeType] || NODE_COLORS.default;
                        return {
                            size: [120, 40],
                            fill: colors.fill,
                            stroke: colors.stroke,
                            lineWidth: nodeType === 'current' ? 2 : 1,
                            radius: 4,
                            labelText: d.data?.label as string || d.id,
                            labelPlacement: 'center',
                            labelFontSize: 12,
                            labelFill: '#333',
                        };
                    },
                },
                edge: {
                    type: 'line',
                    style: {
                        stroke: '#999',
                        lineWidth: 1,
                        endArrow: true,
                    },
                },
                data: {
                    nodes: data.nodes.map((n) => ({
                        id: n.id,
                        data: {label: n.label, nodeType: n.nodeType},
                    })),
                    edges: data.edges.map((e, i) => ({
                        id: `edge-${i}`,
                        source: e.source,
                        target: e.target,
                        style: e.style,
                    })),
                },
            });

            void graph.render();
            graphRef.current = graph;

            graph.on(NodeEvent.CLICK, (e: { target: { id: string } }) => {
                const nodeId = e.target?.id;
                if (nodeId && nodeId !== String(dept.id)) {
                    onNodeClick(Number(nodeId));
                }
            });
        });

        return () => {
            if (graphRef.current) {
                graphRef.current.destroy();
                graphRef.current = null;
            }
        };
    }, [dept, onNodeClick]);

    return (
        <div
            ref={containerRef}
            style={{height: 400, border: '1px solid #e8e8e8', borderRadius: 4}}
        />
    );
};

export default RelationGraphTab;

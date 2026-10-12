import { defineCatalog } from '@json-render/core';
import { schema } from '@json-render/react/schema';
import { z } from 'zod';

export const catalog = defineCatalog(schema, {
  components: {
    Dashboard: {
      props: z.object({
        title: z.string().describe('仪表盘标题'),
        description: z.string().nullable().describe('仪表盘描述'),
      }),
      slots: ['default'],
      description: '根容器组件，定义仪表盘标题和整体布局，必须作为最外层元素',
    },
    Section: {
      props: z.object({
        title: z.string().nullable().describe('分组标题'),
        description: z.string().nullable().describe('分组描述'),
        layout: z
          .enum(['row', 'column'])
          .nullable()
          .describe('子元素排列方式：row 水平排列，column 垂直排列（默认）'),
      }),
      slots: ['default'],
      description:
        '分组容器组件，支持行列布局。用 Section 将内容按主题分组，layout="row" 时子元素水平排列（适合并排展示 StatCard），layout="column" 时垂直排列',
    },
    StatCard: {
      props: z.object({
        title: z.string().describe('指标名称，如"总事件数"'),
        value: z.string().describe('指标值，如"1,284"'),
        trend: z
          .enum(['up', 'down', 'flat'])
          .nullable()
          .describe('趋势方向：up 上升、down 下降、flat 持平'),
        changeRate: z.string().nullable().describe('变化率，如"+12.5%"'),
        icon: z.string().nullable().describe('图标名称（可选）'),
      }),
      description:
        '统计指标卡片组件，展示单个核心指标及其趋势。通常在 Section(layout="row") 中并排展示多个',
    },
    Chart: {
      props: z.object({
        chartType: z
          .enum(['bar', 'line', 'pie', 'area'])
          .describe('图表类型：bar 柱状图、line 折线图、pie 饼图、area 面积图'),
        title: z.string().nullable().describe('图表标题'),
        data: z
          .object({
            categories: z.array(z.string()).describe('X 轴分类标签'),
            series: z
              .array(
                z.object({
                  name: z.string().describe('系列名称'),
                  values: z.array(z.number()).describe('系列数据值'),
                }),
              )
              .describe('数据系列'),
          })
          .describe('图表数据'),
      }),
      description: '图表组件，支持柱状图、折线图、饼图、面积图',
    },
    DataTable: {
      props: z.object({
        title: z.string().nullable().describe('表格标题'),
        columns: z
          .array(
            z.object({
              key: z.string().describe('列字段名'),
              label: z.string().describe('列标题'),
              width: z.string().nullable().describe('列宽度'),
            }),
          )
          .describe('列定义'),
        data: z
          .array(
            z.record(
              z.string(),
              z.union([z.string(), z.number(), z.boolean(), z.null()]),
            ),
          )
          .describe('行数据数组'),
        bordered: z.boolean().nullable().describe('是否显示边框'),
        striped: z.boolean().nullable().describe('是否显示斑马纹'),
      }),
      description: '数据列表组件，展示结构化表格数据',
    },
    TextBlock: {
      props: z.object({
        content: z.string().describe('文本内容'),
        variant: z
          .enum(['plain', 'heading', 'info', 'warning', 'error'])
          .nullable()
          .describe('文本变体'),
      }),
      description: '文本/提示/说明组件，支持5种变体',
    },
    ImageGallery: {
      props: z.object({
        title: z.string().nullable().describe('图片区块标题'),
        description: z.string().nullable().describe('图片区块说明'),
        layout: z
          .enum(['grid', 'carousel'])
          .nullable()
          .describe('布局模式：grid 网格，carousel 轮播（当前按网格渲染）'),
        images: z
          .array(
            z.object({
              url: z.string().describe('图片地址'),
              title: z.string().nullable().describe('图片标题'),
              description: z.string().nullable().describe('图片说明'),
              alt: z.string().nullable().describe('图片替代文本'),
            }),
          )
          .describe('图片列表，单图和多图都通过该数组传递'),
      }),
      description: '图片展示组件，支持单图和多图网格展示，并支持点击预览',
    },
    FlowChart: {
      props: z.object({
        title: z.string().nullable().describe('流程图标题'),
        direction: z
          .enum(['vertical', 'horizontal'])
          .nullable()
          .describe('布局方向'),
        nodes: z
          .array(
            z.object({
              id: z.string().describe('节点唯一标识'),
              label: z.string().describe('节点显示文本'),
              type: z
                .enum(['start', 'process', 'decision', 'end'])
                .describe('节点类型'),
            }),
          )
          .describe('流程节点列表'),
        edges: z
          .array(
            z.object({
              from: z.string().describe('起始节点 id'),
              to: z.string().describe('目标节点 id'),
              label: z.string().nullable().describe('边标签'),
            }),
          )
          .describe('节点连线列表'),
      }),
      description: '流程图组件，展示流程和决策路径',
    },
  },
  actions: {},
});

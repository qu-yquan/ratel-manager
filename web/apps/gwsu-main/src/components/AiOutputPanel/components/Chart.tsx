import { useRef, useEffect, useCallback } from 'react';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import type { EChartsOption } from 'echarts';
import styles from './Chart.module.less';

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer,
]);

interface ChartData {
  categories: string[];
  series: { name: string; values: number[] }[];
}

interface ChartProps {
  chartType: 'bar' | 'line' | 'pie' | 'area';
  title?: string | null;
  data: ChartData;
}

function buildOption(props: ChartProps): EChartsOption {
  const { chartType, data } = props;
  const primaryColor =
    getComputedStyle(document.documentElement)
      .getPropertyValue('--primary-color')
      .trim() || '#1a5fb4';

  const colorPalette = [
    primaryColor,
    '#16a34a',
    '#d97706',
    '#6b4c9a',
    '#dc2626',
    '#0891b2',
  ];

  if (chartType === 'pie') {
    return {
      color: colorPalette,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#6b7280' } },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, color: '#374151' },
          data: data.categories.map((cat, i) => ({
            name: cat,
            value: data.series[0]?.values[i] ?? 0,
          })),
        },
      ],
    };
  }

  const isArea = chartType === 'area';
  return {
    color: colorPalette,
    tooltip: { trigger: 'axis' },
    legend: {
      data: data.series.map((s) => s.name),
      bottom: 0,
      textStyle: { color: '#6b7280' },
    },
    grid: { left: '3%', right: '4%', top: 16, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.categories,
      axisLabel: { color: '#6b7280' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#6b7280' },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
    },
    series: data.series.map((s) => ({
      name: s.name,
      type: chartType === 'area' ? 'line' : chartType,
      data: s.values,
      smooth: true,
      ...(isArea ? { areaStyle: { opacity: 0.15 } } : {}),
      ...(chartType === 'bar'
        ? { itemStyle: { borderRadius: [4, 4, 0, 0] } }
        : {}),
    })),
  };
}

const Chart: React.FC<BaseComponentProps<ChartProps>> = ({ props }) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);
  const resizeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
    instance.setOption(buildOption(props));

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
    <div className={styles.chartContainer}>
      {props.title && (
        <div className={styles.chartTitle}>
          <KnowledgeCitationText text={props.title} />
        </div>
      )}
      <div ref={chartRef} className={styles.chartWrapper} />
    </div>
  );
};

export default Chart;

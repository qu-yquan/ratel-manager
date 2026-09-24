import React, { useEffect, useRef, useState } from 'react';
import { Button, Spin, Empty } from 'antd';
import { ArrowLeftOutlined, ZoomInOutlined, ZoomOutOutlined, ReloadOutlined } from '@ant-design/icons';
// @ts-ignore
import { history } from 'umi';
import styles from './index.module.less';
import { getDeptTree } from '@/services/dept';
import type { DeptTreeNode } from '../types';
import OrgChartGraph from './OrgChartGraph';

const OrgChartPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [treeData, setTreeData] = useState<DeptTreeNode[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const loadData = async () => {
      try {
        const data = await getDeptTree();
        setTreeData(data);
      } catch {
        // error handled by request util
      } finally {
        setLoading(false);
      }
    };
    void loadData();
  }, []);

  const handleBack = () => {
    history.push('/dept');
  };

  return (
    <div className={styles.orgChartPage}>
      <div className={styles.header}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          className={styles.backBtn}
          onClick={handleBack}
        >
          返回
        </Button>
        <span className={styles.title}>组织架构图</span>
        <Button icon={<ZoomInOutlined />} style={{ marginRight: 8 }}>
          放大
        </Button>
        <Button icon={<ZoomOutOutlined />} style={{ marginRight: 8 }}>
          缩小
        </Button>
        <Button icon={<ReloadOutlined />}>
          重置
        </Button>
      </div>
      <div className={styles.content}>
        {loading ? (
          <div className={styles.loading}>
            <Spin size="large" />
          </div>
        ) : treeData.length === 0 ? (
          <div className={styles.loading}>
            <Empty description="暂无部门数据" />
          </div>
        ) : (
          <div ref={containerRef} className={styles.graphContainer}>
            <OrgChartGraph
              data={treeData}
              onNodeDoubleClick={_ => {
                history.push('/dept');
                // 选中的部门将在主页面处理
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default OrgChartPage;

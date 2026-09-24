import React, { useState, useCallback, useEffect } from 'react';
import { Modal, Steps, Select, Button, Tag, List, Spin, Progress, Empty } from 'antd';
import { CheckCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import type { ModuleInfo, TableModelInfo, CollectItem } from '../../types';
import { listUncollected, collectTableModels, getUncollectedCount } from '../../services/tableModel';
import styles from './index.module.less';

interface CollectModalProps {
  visible: boolean;
  modules: ModuleInfo[];
  onClose: () => void;
  onSuccess: () => void;
}

const CollectModal: React.FC<CollectModalProps> = ({ visible, modules, onClose, onSuccess }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedModule, setSelectedModule] = useState<string | undefined>();
  const [uncollectedList, setUncollectedList] = useState<TableModelInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [collecting, setCollecting] = useState(false);
  const [collectProgress, setCollectProgress] = useState(0);
  const [uncollectedCounts, setUncollectedCounts] = useState<Record<string, number>>({});
  const [countsLoading, setCountsLoading] = useState(false);

  /** 加载未采集数量 */
  const loadUncollectedCounts = useCallback(async () => {
    setCountsLoading(true);
    try {
      const counts = await getUncollectedCount(
        modules.map((m) => ({ modulePrefix: m.prefix, applicationName: m.applicationName }))
      );
      setUncollectedCounts(counts);
    } catch {
      // request 层已自动提示
    } finally {
      setCountsLoading(false);
    }
  }, [modules]);

  /** 重置状态 */
  const resetState = useCallback(() => {
    setCurrentStep(0);
    setSelectedModule(undefined);
    setUncollectedList([]);
    setLoading(false);
    setCollecting(false);
    setCollectProgress(0);
    setUncollectedCounts({});
  }, []);

  /** 关闭弹窗 */
  const handleClose = useCallback(() => {
    resetState();
    onClose();
  }, [resetState, onClose]);

  /** 弹窗打开时加载未采集数量 */
  useEffect(() => {
    if (visible) {
      loadUncollectedCounts();
    }
  }, [visible, loadUncollectedCounts]);

  /** 解析未采集的表模型 */
  const handleParse = useCallback(async () => {
    if (!selectedModule) return;
    setLoading(true);
    try {
      const list = await listUncollected(selectedModule);
      setUncollectedList(list);
      setCurrentStep(1);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [selectedModule]);

  /** 执行采集 */
  const handleCollect = useCallback(async () => {
    if (uncollectedList.length === 0) return;
    setCollecting(true);
    setCurrentStep(2);

    const items: CollectItem[] = uncollectedList.map((item) => {
      const mod = modules.find((m) => m.prefix === item.modulePrefix);
      return {
        applicationName: mod?.applicationName ?? '',
        modulePrefix: item.modulePrefix,
        datasource: item.dataSource,
        tableName: item.tableName,
        moduleFieldConfig: item.moduleFieldConfig,
      };
    });

    try {
      // 模拟进度
      const timer = setInterval(() => {
        setCollectProgress((prev) => {
          if (prev >= 90) {
            clearInterval(timer);
            return 90;
          }
          return prev + Math.random() * 20;
        });
      }, 300);

      await collectTableModels(items);

      clearInterval(timer);
      setCollectProgress(100);
      setCurrentStep(3);
      onSuccess();
    } catch {
      setCollecting(false);
      setCurrentStep(1);
    }
  }, [uncollectedList, onSuccess]);

  /** 步骤内容 */
  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <div className={styles.stepContent}>
            <div style={{ marginBottom: 16 }}>请选择需要采集的模块，系统将解析出该模块下未采集的表模型列表。</div>
            <Select
              placeholder="请选择模块"
              style={{ width: '100%' }}
              value={selectedModule}
              onChange={setSelectedModule}
              options={modules.map((m) => {
                const count = uncollectedCounts[m.prefix] ?? 0;
                return {
                  label: `${m.note || m.prefix}${count > 0 ? `（${count} 个未采集）` : ''}`,
                  value: m.prefix,
                };
              })}
            />
            {countsLoading ? (
              <div className={styles.countsLoading}>
                <Spin size="small" /> <span>加载未采集数量...</span>
              </div>
            ) : (
              Object.keys(uncollectedCounts).length > 0 && (
                <div className={styles.moduleCountList}>
                  {modules.map((m) => {
                    const count = uncollectedCounts[m.prefix] ?? 0;
                    return (
                      <div
                        key={m.prefix}
                        className={`${styles.moduleCountItem} ${selectedModule === m.prefix ? styles.moduleCountItemActive : ''}`}
                        onClick={() => setSelectedModule(m.prefix)}
                      >
                        <span className={styles.moduleCountName}>{m.note || m.prefix}</span>
                        <Tag color={count > 0 ? 'blue' : 'default'}>{count} 个未采集</Tag>
                      </div>
                    );
                  })}
                </div>
              )
            )}
          </div>
        );
      case 1:
        return (
          <div className={styles.stepContent}>
            {uncollectedList.length === 0 ? (
              <div className={styles.emptyHint}>
                <Empty description="没有未采集的表模型" />
              </div>
            ) : (
              <>
                <div style={{ marginBottom: 12 }}>
                  共解析出 <strong>{uncollectedList.length}</strong> 个未采集的表模型，点击下一步将开始采集。
                </div>
                <div className={styles.uncollectedList}>
                  <List
                    size="small"
                    dataSource={uncollectedList}
                    renderItem={(item) => (
                      <List.Item>
                        <div className={styles.tableItem}>
                          <div className={styles.tableItemInfo}>
                            <span className={styles.tableName}>{item.tableName}</span>
                            <Tag className={styles.moduleTag}>{item.modulePrefix}</Tag>
                            <Tag className={styles.dsTag} color="orange">{item.dataSource}</Tag>
                          </div>
                        </div>
                      </List.Item>
                    )}
                  />
                </div>
              </>
            )}
          </div>
        );
      case 2:
        return (
          <div className={styles.collectingOverlay}>
            <Spin indicator={<LoadingOutlined style={{ fontSize: 32 }} spin />} />
            <div>正在采集表模型数据，请稍候...</div>
            <Progress
              percent={Math.round(collectProgress)}
              className={styles.collectProgress}
              status="active"
            />
          </div>
        );
      case 3:
        return (
          <div className={styles.collectingOverlay}>
            <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
            <div>采集完成！共采集 {uncollectedList.length} 个表模型。</div>
          </div>
        );
      default:
        return null;
    }
  };

  /** 底部按钮 */
  const renderFooter = () => {
    switch (currentStep) {
      case 0:
        return [
          <Button key="cancel" onClick={handleClose}>取消</Button>,
          <Button key="next" type="primary" loading={loading} disabled={!selectedModule} onClick={handleParse}>
            下一步
          </Button>,
        ];
      case 1:
        return [
          <Button key="prev" onClick={() => setCurrentStep(0)}>上一步</Button>,
          <Button key="cancel" onClick={handleClose}>取消</Button>,
          <Button key="collect" type="primary" disabled={uncollectedList.length === 0} onClick={handleCollect}>
            下一步
          </Button>,
        ];
      case 2:
        return [
          <Button key="cancel" disabled={collecting}>取消</Button>,
        ];
      case 3:
        return [
          <Button key="close" type="primary" onClick={handleClose}>完成</Button>,
        ];
      default:
        return [];
    }
  };

  return (
    <Modal
      title="采集表模型"
      open={visible}
      width={680}
      footer={renderFooter()}
      onCancel={handleClose}
      mask={
      {closable: false}
      }
      className={styles.collectModal}
    >
      <Steps
        current={currentStep}
        items={[
          { title: '选择模块' },
          { title: '确认采集' },
          { title: '采集中' },
          { title: '完成' },
        ]}
      />
      {renderStepContent()}
    </Modal>
  );
};

export default CollectModal;

import { useState, useEffect, useCallback } from 'react';
import { Button, App, Spin } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  EyeOutlined,
  ControlOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch } from '@gwsu/core';
import {
  saveOrUpdateConfig,
  saveRemoteControlConfig,
} from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { ViewConfig, RemoteControlConfig, AssistantTabKey } from './types';
import {
  createDefaultViewConfig,
  createDefaultRemoteControlConfig,
  DEFAULT_DINGTALK_REMOTE_CONFIG,
} from './types';
import ViewConfigForm from './ViewConfigForm';
import RemoteControlForm from './RemoteControlForm';
import styles from './index.module.less';

const VIEW_CONFIG_KEY = 'assistant_view_config';
const REMOTE_CONTROL_CONFIG_KEY = 'assistant_remote_control_config';

const AssistantConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<AssistantTabKey>('view');

  // 展示配置
  const [viewConfig, setViewConfig] = useState<ViewConfig>(
    createDefaultViewConfig(),
  );
  const [viewConfigId, setViewConfigId] = useState<string | undefined>();

  // 远程操作配置
  const [remoteControlConfig, setRemoteControlConfig] =
    useState<RemoteControlConfig>(createDefaultRemoteControlConfig());
  const [, setRemoteControlConfigId] = useState<string | undefined>();

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([
        VIEW_CONFIG_KEY,
        REMOTE_CONTROL_CONFIG_KEY,
      ]);

      // 解析展示配置
      const viewInfo = configMap[VIEW_CONFIG_KEY] as ConfigInfo | undefined;
      if (viewInfo?.configValue) {
        try {
          const parsed = JSON.parse(viewInfo.configValue) as ViewConfig;
          setViewConfig({ ...createDefaultViewConfig(), ...parsed });
          setViewConfigId(viewInfo.id);
        } catch {
          message.warning('展示配置解析失败，已恢复默认值');
          setViewConfig(createDefaultViewConfig());
          setViewConfigId(viewInfo.id);
        }
      } else {
        setViewConfig(createDefaultViewConfig());
        setViewConfigId(undefined);
      }

      // 解析远程操作配置
      const remoteInfo = configMap[REMOTE_CONTROL_CONFIG_KEY] as
        | ConfigInfo
        | undefined;
      if (remoteInfo?.configValue) {
        try {
          const parsed = JSON.parse(
            remoteInfo.configValue,
          ) as RemoteControlConfig;
          setRemoteControlConfig({
            type: parsed.type || 'NONE',
            dingTalk: { ...DEFAULT_DINGTALK_REMOTE_CONFIG, ...parsed.dingTalk },
          });
          setRemoteControlConfigId(remoteInfo.id);
        } catch {
          message.warning('远程操作配置解析失败，已恢复默认值');
          setRemoteControlConfig(createDefaultRemoteControlConfig());
          setRemoteControlConfigId(remoteInfo.id);
        }
      } else {
        setRemoteControlConfig(createDefaultRemoteControlConfig());
        setRemoteControlConfigId(undefined);
      }
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  const handleViewConfigChange = (updated: ViewConfig) => {
    setViewConfig(updated);
  };

  const handleRemoteControlConfigChange = (updated: RemoteControlConfig) => {
    setRemoteControlConfig(updated);
  };

  const handleSave = async () => {
    // 远程操作配置校验
    if (activeTab === 'remote' && remoteControlConfig.type === 'DING_TALK') {
      const dt = remoteControlConfig.dingTalk;
      if (!dt.protocol || !dt.regionId || !dt.endpoint) {
        message.warning('请填写完整的钉钉连接配置');
        return;
      }
      if (!dt.clientId) {
        message.warning('请填写 Client ID');
        return;
      }
      if (!dt.clientSecret) {
        message.warning('请填写 Client Secret');
        return;
      }
      if (!dt.aiCardTemplateId) {
        message.warning('请填写 AI 输出卡片模板 ID');
        return;
      }
    }

    setSaving(true);
    try {
      // 保存当前激活的 Tab 配置
      if (activeTab === 'view') {
        const success = await saveOrUpdateConfig({
          id: viewConfigId,
          configKey: VIEW_CONFIG_KEY,
          configName: '助手展示配置',
          configValue: JSON.stringify(viewConfig),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: 'AI 助手界面展示配置',
        });
        if (success) {
          message.success('展示配置保存成功');
          fetchConfig();
        }
      } else if (activeTab === 'remote') {
        const success = await saveRemoteControlConfig(remoteControlConfig);
        if (success) {
          message.success('远程操作配置保存成功');
          fetchConfig();
        }
      }
    } catch {
      // error handled by request util
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const tabs: { key: AssistantTabKey; label: string; icon: React.ReactNode }[] =
    [
      { key: 'view', label: '展示配置', icon: <EyeOutlined /> },
      { key: 'remote', label: '远程操作', icon: <ControlOutlined /> },
    ];

  return (
    <div className={styles.assistantConfig}>
      <div className={styles.layout}>
        {/* 左侧 Tab 导航 */}
        <div className={styles.sideTabNav}>
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`${styles.sideTab} ${
                activeTab === tab.key ? styles.sideTabActive : ''
              }`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span className={styles.sideTabIcon}>{tab.icon}</span>
              <span className={styles.sideTabLabel}>{tab.label}</span>
            </button>
          ))}
        </div>

        {/* 右侧内容区 */}
        <div className={styles.sideTabContent}>
          {activeTab === 'view' && (
            <ViewConfigForm
              value={viewConfig}
              onChange={handleViewConfigChange}
            />
          )}

          {activeTab === 'remote' && (
            <RemoteControlForm
              value={remoteControlConfig}
              onChange={handleRemoteControlConfigChange}
            />
          )}

          {/* 操作栏 */}
          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={fetchConfig}>
              重置
            </Button>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saving}
              onClick={handleSave}
              data-ai-approval
            >
              保存
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssistantConfigTab;

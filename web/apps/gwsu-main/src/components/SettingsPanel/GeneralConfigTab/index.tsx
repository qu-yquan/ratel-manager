import { useState, useEffect, useCallback } from 'react';
import { Button, App, Spin } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  GlobalOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch, useProjectConfigStore } from '@gwsu/core';
import {
  getCaptchaTypeOptions,
  getLogStorageMediumOptions,
  saveOrUpdateConfig,
} from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type {
  BaseUrlConfig,
  CaptchaConfig,
  CaptchaTypeOption,
  GeneralTabKey,
  LogStorageConfig,
  LogStorageMediumOption,
} from './types';
import {
  BASE_URL_CONFIG_KEY,
  CAPTCHA_CONFIG_KEY,
  createDefaultBaseUrlConfig,
  createDefaultCaptchaConfig,
  createDefaultLogStorageConfig,
  DEFAULT_BASE_URL_CONFIG,
  LOG_STORAGE_CONFIG_KEY,
  normalizeCaptchaConfig,
  normalizeLogStorageConfig,
} from './types';
import ProjectUrlForm from './ProjectUrlForm';
import CaptchaConfigForm from './CaptchaConfigForm';
import LogConfigForm from './LogConfigForm';
import styles from './index.module.less';

const GeneralConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<GeneralTabKey>('projectUrl');

  // 基础地址配置
  const [baseUrlConfig, setBaseUrlConfig] = useState<BaseUrlConfig>(
    createDefaultBaseUrlConfig(),
  );
  const [baseUrlConfigId, setBaseUrlConfigId] = useState<string | undefined>();
  const [captchaConfig, setCaptchaConfig] = useState<CaptchaConfig>(
    createDefaultCaptchaConfig(),
  );
  const [captchaConfigId, setCaptchaConfigId] = useState<string | undefined>();
  const [captchaTypeOptions, setCaptchaTypeOptions] = useState<
    CaptchaTypeOption[]
  >([]);
  const [captchaTypeLoading, setCaptchaTypeLoading] = useState(false);
  const [logStorageConfig, setLogStorageConfig] = useState<LogStorageConfig>(
    createDefaultLogStorageConfig(),
  );
  const [logStorageConfigId, setLogStorageConfigId] = useState<
    string | undefined
  >();
  const [logStorageMediumOptions, setLogStorageMediumOptions] = useState<
    LogStorageMediumOption[]
  >([]);
  const [logStorageMediumLoading, setLogStorageMediumLoading] = useState(false);

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([
        BASE_URL_CONFIG_KEY,
        CAPTCHA_CONFIG_KEY,
        LOG_STORAGE_CONFIG_KEY,
      ]);

      // 解析基础地址配置
      const urlInfo = configMap[BASE_URL_CONFIG_KEY] as ConfigInfo | undefined;
      if (urlInfo?.configValue) {
        try {
          const parsed = JSON.parse(urlInfo.configValue) as BaseUrlConfig;
          setBaseUrlConfig({ ...DEFAULT_BASE_URL_CONFIG, ...parsed });
          setBaseUrlConfigId(urlInfo.id);
        } catch {
          message.warning('项目地址配置解析失败，已恢复默认值');
          setBaseUrlConfig(createDefaultBaseUrlConfig());
          setBaseUrlConfigId(urlInfo.id);
        }
      } else {
        setBaseUrlConfig(createDefaultBaseUrlConfig());
        setBaseUrlConfigId(undefined);
      }

      // 解析图形验证码配置
      const captchaInfo = configMap[CAPTCHA_CONFIG_KEY] as
        | ConfigInfo
        | undefined;
      if (captchaInfo?.configValue) {
        try {
          const parsed = JSON.parse(
            captchaInfo.configValue,
          ) as Partial<CaptchaConfig>;
          setCaptchaConfig(normalizeCaptchaConfig(parsed));
          setCaptchaConfigId(captchaInfo.id);
        } catch {
          message.warning('图形验证码配置解析失败，已恢复默认值');
          setCaptchaConfig(createDefaultCaptchaConfig());
          setCaptchaConfigId(captchaInfo.id);
        }
      } else {
        setCaptchaConfig(createDefaultCaptchaConfig());
        setCaptchaConfigId(undefined);
      }

      const logStorageInfo = configMap[LOG_STORAGE_CONFIG_KEY] as
        | ConfigInfo
        | undefined;
      if (logStorageInfo?.configValue) {
        try {
          const parsed = JSON.parse(
            logStorageInfo.configValue,
          ) as Partial<LogStorageConfig>;
          setLogStorageConfig(normalizeLogStorageConfig(parsed));
          setLogStorageConfigId(logStorageInfo.id);
        } catch {
          message.warning('日志配置解析失败，已恢复默认值');
          setLogStorageConfig(createDefaultLogStorageConfig());
          setLogStorageConfigId(logStorageInfo.id);
        }
      } else {
        setLogStorageConfig(createDefaultLogStorageConfig());
        setLogStorageConfigId(undefined);
      }
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCaptchaTypes = useCallback(async () => {
    setCaptchaTypeLoading(true);
    try {
      const options = await getCaptchaTypeOptions();
      setCaptchaTypeOptions(options);
    } catch {
      // error handled by request util
    } finally {
      setCaptchaTypeLoading(false);
    }
  }, []);

  const fetchLogStorageMediums = useCallback(async () => {
    setLogStorageMediumLoading(true);
    try {
      setLogStorageMediumOptions(await getLogStorageMediumOptions());
    } catch {
      // error handled by request util
    } finally {
      setLogStorageMediumLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
    fetchCaptchaTypes();
    fetchLogStorageMediums();
  }, [fetchConfig, fetchCaptchaTypes, fetchLogStorageMediums]);

  const handleBaseUrlConfigChange = (updated: BaseUrlConfig) => {
    setBaseUrlConfig(updated);
  };

  const handleCaptchaConfigChange = (updated: CaptchaConfig) => {
    setCaptchaConfig(updated);
  };

  const handleLogStorageConfigChange = (updated: LogStorageConfig) => {
    setLogStorageConfig(updated);
  };

  const handleReload = () => {
    fetchConfig();
    fetchCaptchaTypes();
    fetchLogStorageMediums();
  };

  const handleSave = async () => {
    // 基础地址配置校验
    if (activeTab === 'projectUrl') {
      if (!baseUrlConfig.projectName) {
        message.warning('请填写项目名称');
        return;
      }
      if (!baseUrlConfig.viewBaseUrl) {
        message.warning('请填写前端地址');
        return;
      }
      if (!baseUrlConfig.apiBaseUrl) {
        message.warning('请填写后端 API 地址');
        return;
      }
    }
    if (activeTab === 'captcha') {
      if (!captchaConfig.type) {
        message.warning('请选择验证码类型');
        return;
      }
      if (!captchaConfig.waterMark) {
        message.warning('请填写水印文字');
        return;
      }
      if (!captchaConfig.expireSeconds || captchaConfig.expireSeconds <= 0) {
        message.warning('请填写有效的验证码有效时间');
        return;
      }
      if (
        !captchaConfig.verificationExpireSeconds ||
        captchaConfig.verificationExpireSeconds <= 0
      ) {
        message.warning('请填写有效的二次校验凭证有效时间');
        return;
      }
    }
    if (activeTab === 'log') {
      const configs = [
        { name: '操作日志', value: logStorageConfig.operationLog },
        { name: '表操作日志', value: logStorageConfig.tableLog },
        { name: '登录日志', value: logStorageConfig.loginLog },
      ];
      for (const config of configs) {
        const { coldMinAge, deleteMinAge } = config.value.dataLifeCycle;
        if (!deleteMinAge || deleteMinAge <= 0) {
          message.warning(`请填写有效的${config.name}自动删除天数`);
          return;
        }
        if (
          config.value.medium === 'ES' &&
          (coldMinAge < 0 || coldMinAge >= deleteMinAge)
        ) {
          message.warning(`${config.name}进入冷数据天数必须小于自动删除天数`);
          return;
        }
      }
    }

    setSaving(true);
    try {
      if (activeTab === 'projectUrl') {
        const success = await saveOrUpdateConfig({
          id: baseUrlConfigId,
          configKey: BASE_URL_CONFIG_KEY,
          configName: '基础地址配置',
          configValue: JSON.stringify(baseUrlConfig),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: '项目前后端基础地址配置',
        });
        if (success) {
          message.success('项目地址配置保存成功');
          // 同步更新全局项目配置 store
          useProjectConfigStore.getState().setBaseUrlConfig(baseUrlConfig);
          fetchConfig();
        }
      }
      if (activeTab === 'captcha') {
        const success = await saveOrUpdateConfig({
          id: captchaConfigId,
          configKey: CAPTCHA_CONFIG_KEY,
          configName: '验证码配置',
          configValue: JSON.stringify(normalizeCaptchaConfig(captchaConfig)),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: '登录验证码默认配置',
        });
        if (success) {
          message.success('图形验证码配置保存成功');
          fetchConfig();
        }
      }
      if (activeTab === 'log') {
        const success = await saveOrUpdateConfig({
          id: logStorageConfigId,
          configKey: LOG_STORAGE_CONFIG_KEY,
          configName: '日志配置',
          configValue: JSON.stringify(
            normalizeLogStorageConfig(logStorageConfig),
          ),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: '操作日志、表操作日志和登录日志的存储媒介及生命周期配置',
        });
        if (success) {
          message.success('日志配置保存成功');
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

  const tabs: { key: GeneralTabKey; label: string; icon: React.ReactNode }[] = [
    { key: 'projectUrl', label: '项目信息', icon: <GlobalOutlined /> },
    {
      key: 'captcha',
      label: '图形验证码',
      icon: <SafetyCertificateOutlined />,
    },
    { key: 'log', label: '日志配置', icon: <FileTextOutlined /> },
  ];

  return (
    <div className={styles.generalConfig}>
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
          {activeTab === 'projectUrl' && (
            <ProjectUrlForm
              value={baseUrlConfig}
              onChange={handleBaseUrlConfigChange}
            />
          )}
          {activeTab === 'captcha' && (
            <CaptchaConfigForm
              value={captchaConfig}
              typeOptions={captchaTypeOptions}
              typeLoading={captchaTypeLoading}
              onChange={handleCaptchaConfigChange}
            />
          )}
          {activeTab === 'log' && (
            <LogConfigForm
              value={logStorageConfig}
              mediumOptions={logStorageMediumOptions}
              mediumLoading={logStorageMediumLoading}
              onChange={handleLogStorageConfigChange}
            />
          )}

          {/* 操作栏 */}
          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={handleReload}>
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

export default GeneralConfigTab;

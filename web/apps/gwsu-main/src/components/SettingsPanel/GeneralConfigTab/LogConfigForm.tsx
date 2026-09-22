import { Alert, InputNumber, Select } from 'antd';
import { ClockCircleOutlined, DatabaseOutlined } from '@ant-design/icons';
import type {
  LogStorageConfig,
  LogStorageItemConfig,
  LogStorageMediumOption,
} from './types';
import styles from './LogConfigForm.module.less';

interface LogConfigFormProps {
  value: LogStorageConfig;
  mediumOptions: LogStorageMediumOption[];
  mediumLoading?: boolean;
  onChange: (value: LogStorageConfig) => void;
}

const LOG_ITEMS: Array<{
  key: keyof LogStorageConfig;
  label: string;
  description: string;
}> = [
  {
    key: 'operationLog',
    label: '操作日志',
    description: '用户请求及服务调用链日志',
  },
  { key: 'tableLog', label: '表操作日志', description: '业务数据变更记录' },
  {
    key: 'loginLog',
    label: '登录日志',
    description: '用户认证及会话生命周期记录',
  },
];

const LogConfigForm: React.FC<LogConfigFormProps> = ({
  value,
  mediumOptions,
  mediumLoading = false,
  onChange,
}) => {
  const updateItem = (
    key: keyof LogStorageConfig,
    item: LogStorageItemConfig,
  ) => {
    onChange({ ...value, [key]: item });
  };

  return (
    <div className={styles.logConfigForm}>
      {LOG_ITEMS.map((definition) => {
        const item = value[definition.key];
        const isEs = item.medium === 'ES';
        return (
          <section className={styles.logCard} key={definition.key}>
            <div className={styles.cardHeader}>
              <div className={styles.cardIcon}>
                <DatabaseOutlined aria-hidden="true" />
              </div>
              <div>
                <h3 className={styles.cardTitle}>{definition.label}</h3>
                <p className={styles.cardDescription}>
                  {definition.description}
                </p>
              </div>
            </div>

            <div className={styles.fieldGrid}>
              <label className={styles.fieldItem}>
                <span className={styles.fieldLabel}>存储媒介</span>
                <Select
                  aria-label={`${definition.label}存储媒介`}
                  loading={mediumLoading}
                  options={mediumOptions.map((option) => ({
                    label: option.value,
                    value: option.key,
                  }))}
                  value={item.medium}
                  onChange={(medium) =>
                    updateItem(definition.key, { ...item, medium })
                  }
                />
              </label>

              {isEs && (
                <label className={styles.fieldItem}>
                  <span className={styles.fieldLabel}>进入冷数据</span>
                  <InputNumber
                    addonAfter="天"
                    aria-label={`${definition.label}进入冷数据天数`}
                    min={0}
                    precision={0}
                    value={item.dataLifeCycle.coldMinAge}
                    onChange={(coldMinAge) =>
                      updateItem(definition.key, {
                        ...item,
                        dataLifeCycle: {
                          ...item.dataLifeCycle,
                          coldMinAge: coldMinAge ?? 0,
                        },
                      })
                    }
                  />
                </label>
              )}

              <label className={styles.fieldItem}>
                <span className={styles.fieldLabel}>自动删除</span>
                <InputNumber
                  addonAfter="天"
                  aria-label={`${definition.label}自动删除天数`}
                  min={1}
                  precision={0}
                  prefix={<ClockCircleOutlined aria-hidden="true" />}
                  value={item.dataLifeCycle.deleteMinAge}
                  onChange={(deleteMinAge) =>
                    updateItem(definition.key, {
                      ...item,
                      dataLifeCycle: {
                        ...item.dataLifeCycle,
                        deleteMinAge: deleteMinAge ?? 1,
                      },
                    })
                  }
                />
              </label>
            </div>

            {isEs && (
              <Alert
                message="Elasticsearch 存储能力尚未接入；选择后数据库生命周期清理任务不会处理该类日志。"
                showIcon
                type="warning"
              />
            )}
          </section>
        );
      })}
    </div>
  );
};

export default LogConfigForm;

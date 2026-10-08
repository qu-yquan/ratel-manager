import { Card, Form, Input, InputNumber, Select, Switch } from 'antd';
import type {
  ModelRerankConfig,
  RerankProvider,
  RerankProviderConfig,
} from './types';
import { RERANK_PROVIDER_LIST } from './types';
import styles from './index.module.less';

interface RerankConfigFormProps {
  value: ModelRerankConfig;
  onChange: (value: ModelRerankConfig) => void;
}

const RerankConfigForm: React.FC<RerankConfigFormProps> = ({
  value,
  onChange,
}) => {
  const handleProviderChange = (provider: RerankProvider) => {
    onChange({ ...value, provider });
  };

  const handleConfigChange = (
    field: keyof RerankProviderConfig | 'instruct',
    fieldValue: unknown,
  ) => {
    const provider = value.provider;
    onChange({
      ...value,
      [provider]: { ...value[provider], [field]: fieldValue },
    });
  };

  const currentConfig = value[value.provider];
  const providerLabel =
    RERANK_PROVIDER_LIST.find((item) => item.key === value.provider)?.label ??
    value.provider;

  return (
    <>
      <Card
        title="模型提供商"
        className={`${styles.sectionCard} ${styles.providerSection}`}
        size="small"
      >
        <Form layout="vertical">
          <Form.Item label="启用">
            <Switch
              checked={value.enabled}
              onChange={(enabled) => onChange({ ...value, enabled })}
            />
          </Form.Item>
          <Form.Item label="提供商">
            <Select
              value={value.provider}
              onChange={handleProviderChange}
              options={RERANK_PROVIDER_LIST.map((item) => ({
                label: `${item.label} - ${item.description}`,
                value: item.key,
              }))}
              className={styles.fullWidthControl}
              aria-label="重排模型提供商"
            />
          </Form.Item>
          <Form.Item label="返回数量 Top N" required>
            <InputNumber
              min={1}
              max={100}
              precision={0}
              value={value.topN}
              onChange={(topN) => onChange({ ...value, topN: topN ?? 10 })}
              className={styles.fullWidthControl}
            />
          </Form.Item>
        </Form>
      </Card>
      <Card title="连接配置" className={styles.sectionCard} size="small">
        <Form layout="vertical">
          <Form.Item label="API Key" required={value.provider !== 'xinference'}>
            <Input.Password
              value={currentConfig.apiKey}
              onChange={(event) =>
                handleConfigChange('apiKey', event.target.value)
              }
              placeholder={
                value.provider === 'xinference'
                  ? '可选，按服务端鉴权配置填写'
                  : `请输入 ${providerLabel} API Key`
              }
            />
          </Form.Item>
          <Form.Item label="模型名称" required>
            <Input
              value={currentConfig.modelName}
              onChange={(event) =>
                handleConfigChange('modelName', event.target.value)
              }
              placeholder={
                value.provider === 'xinference'
                  ? '请输入已启动的 Reranker Model UID'
                  : '请输入重排模型名称'
              }
            />
          </Form.Item>
          <Form.Item
            label="Base URL"
            required={value.provider === 'xinference'}
          >
            <Input
              value={currentConfig.baseUrl}
              onChange={(event) =>
                handleConfigChange('baseUrl', event.target.value)
              }
              placeholder={
                value.provider === 'xinference'
                  ? '例如 http://localhost:9997'
                  : '可选，留空使用默认地址'
              }
            />
          </Form.Item>
          {value.provider === 'dashscope' && (
            <Form.Item label="指令">
              <Input
                value={value.dashscope.instruct}
                onChange={(event) =>
                  handleConfigChange('instruct', event.target.value)
                }
                placeholder="可选，向重排模型补充任务指令"
              />
            </Form.Item>
          )}
        </Form>
      </Card>
    </>
  );
};

export default RerankConfigForm;

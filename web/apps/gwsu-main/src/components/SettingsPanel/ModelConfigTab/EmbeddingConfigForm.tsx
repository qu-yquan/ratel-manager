import { Form, Input, InputNumber, Select, Card, Switch } from 'antd';
import type {
  EmbeddingProvider,
  ModelEmbeddingConfig,
  DashscopeEmbeddingConfig,
  EmbeddingProviderConfig,
  OllamaEmbeddingConfig,
} from './types';
import { EMBEDDING_PROVIDER_LIST } from './types';
import styles from './index.module.less';

interface EmbeddingConfigFormProps {
  value: ModelEmbeddingConfig;
  onChange: (value: ModelEmbeddingConfig) => void;
}

const EmbeddingConfigForm: React.FC<EmbeddingConfigFormProps> = ({
  value,
  onChange,
}) => {
  const handleProviderChange = (provider: EmbeddingProvider) => {
    onChange({ ...value, provider });
  };

  const handleConfigChange = (
    provider: EmbeddingProvider,
    providerConfig:
      | DashscopeEmbeddingConfig
      | EmbeddingProviderConfig
      | OllamaEmbeddingConfig,
  ) => {
    onChange({ ...value, [provider]: providerConfig });
  };

  const renderRemoteForm = (
    provider: 'dashscope' | 'openai',
    config: DashscopeEmbeddingConfig | EmbeddingProviderConfig,
  ) => (
    <>
      <Form.Item label="API Key" required>
        <Input.Password
          value={config.apiKey}
          onChange={(event) =>
            handleConfigChange(provider, {
              ...config,
              apiKey: event.target.value,
            })
          }
          placeholder="请输入 API Key"
        />
      </Form.Item>
      <Form.Item label="模型名称" required>
        <Input
          value={config.modelName}
          onChange={(event) =>
            handleConfigChange(provider, {
              ...config,
              modelName: event.target.value,
            })
          }
          placeholder="请输入向量化模型名称"
        />
      </Form.Item>
      <Form.Item label="Base URL">
        <Input
          value={config.baseUrl}
          onChange={(event) =>
            handleConfigChange(provider, {
              ...config,
              baseUrl: event.target.value,
            })
          }
          placeholder="可选，留空使用默认地址"
        />
      </Form.Item>
      <Form.Item label="向量维度">
        <InputNumber
          min={1}
          value={config.dimensions}
          onChange={(dimensions) =>
            handleConfigChange(provider, {
              ...config,
              dimensions: dimensions ?? undefined,
            })
          }
          placeholder="留空使用模型默认维度"
          className={styles.fullWidthControl}
        />
      </Form.Item>
      <Form.Item label="批量大小">
        <InputNumber
          min={1}
          max={2048}
          value={config.batchSize}
          onChange={(batchSize) =>
            handleConfigChange(provider, {
              ...config,
              batchSize: batchSize ?? undefined,
            })
          }
          className={styles.fullWidthControl}
        />
      </Form.Item>
    </>
  );

  const renderOllamaForm = (config: OllamaEmbeddingConfig) => (
    <>
      <Form.Item label="模型名称" required>
        <Input
          value={config.modelName}
          onChange={(event) =>
            handleConfigChange('ollama', {
              ...config,
              modelName: event.target.value,
            })
          }
          placeholder="例如 nomic-embed-text"
        />
      </Form.Item>
      <Form.Item label="Base URL" required>
        <Input
          value={config.baseUrl}
          onChange={(event) =>
            handleConfigChange('ollama', {
              ...config,
              baseUrl: event.target.value,
            })
          }
          placeholder="http://localhost:11434"
        />
      </Form.Item>
      <Form.Item label="向量维度">
        <InputNumber
          min={1}
          value={config.dimensions}
          onChange={(dimensions) =>
            handleConfigChange('ollama', {
              ...config,
              dimensions: dimensions ?? undefined,
            })
          }
          placeholder="留空使用模型默认维度"
          className={styles.fullWidthControl}
        />
      </Form.Item>
      <Form.Item label="批量大小">
        <InputNumber
          min={1}
          max={2048}
          value={config.batchSize}
          onChange={(batchSize) =>
            handleConfigChange('ollama', {
              ...config,
              batchSize: batchSize ?? undefined,
            })
          }
          className={styles.fullWidthControl}
        />
      </Form.Item>
    </>
  );

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
              options={EMBEDDING_PROVIDER_LIST.map((item) => ({
                label: `${item.label} - ${item.description}`,
                value: item.key,
              }))}
              className={styles.fullWidthControl}
              aria-label="向量化模型提供商"
            />
          </Form.Item>
        </Form>
      </Card>
      <Card title="连接配置" className={styles.sectionCard} size="small">
        <Form layout="vertical">
          {value.provider === 'dashscope' &&
            renderRemoteForm('dashscope', value.dashscope)}
          {value.provider === 'openai' &&
            renderRemoteForm('openai', value.openai)}
          {value.provider === 'ollama' && renderOllamaForm(value.ollama)}
        </Form>
      </Card>
    </>
  );
};

export default EmbeddingConfigForm;

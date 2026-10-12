import { Segmented } from 'antd';
import type { ModelProvider, ProviderInfo } from './types';
import { PROVIDER_LIST } from './types';

interface ProviderSelectorProps {
  value?: ModelProvider;
  onChange?: (value: ModelProvider) => void;
}

const ProviderSelector: React.FC<ProviderSelectorProps> = ({
  value,
  onChange,
}) => {
  const options = PROVIDER_LIST.map((item: ProviderInfo<ModelProvider>) => ({
    label: (
      <div style={{ textAlign: 'center', padding: '4px 0' }}>
        <div style={{ fontWeight: 500, fontSize: 14 }}>{item.label}</div>
        <div style={{ fontSize: 11, color: 'var(--ant-color-text-secondary)' }}>
          {item.description}
        </div>
      </div>
    ),
    value: item.key,
  }));

  return (
    <Segmented
      block
      value={value}
      onChange={(val) => onChange?.(val as ModelProvider)}
      options={options}
    />
  );
};

export default ProviderSelector;

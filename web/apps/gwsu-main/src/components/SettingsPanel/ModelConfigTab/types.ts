/** 模型提供商标识 */
export type ModelProvider = 'dashscope' | 'openai' | 'gemini' | 'anthropic';

/** 向量化模型提供商标识 */
export type EmbeddingProvider = 'dashscope' | 'openai' | 'ollama';

/** 重排模型提供商标识 */
export type RerankProvider = 'dashscope' | 'jina' | 'xinference';

/** DashScope LLM 提供商配置 */
export interface DashscopeConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  enableThinking: boolean;
  enableSearch: boolean;
  baseUrl: string;
}

/** OpenAI LLM 提供商配置 */
export interface OpenaiConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  baseUrl: string;
  endpointPath: string;
}

/** Gemini LLM 提供商配置 */
export interface GeminiConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  project: string;
  location: string;
}

/** Anthropic LLM 提供商配置 */
export interface AnthropicConfig {
  apiKey: string;
  modelName: string;
  stream: boolean;
  baseUrl: string;
}

/** 通用生成参数 */
export interface GenerateOptionsConfig {
  temperature?: number;
  topP?: number;
  maxTokens?: number;
  frequencyPenalty?: number;
  presencePenalty?: number;
  topK?: number;
  seed?: number;
  additionalBodyParams?: Record<string, unknown>;
}

/** 多模态配置 */
export interface MultimodalOptionsConfig {
  resourceUrlToBase64: boolean;
  maxUploadSizeMb: number;
  maxUploadCount: number;
  allowedUploadFormats?: string[];
}

/** LLM 模型配置结构（与后端 model_llm_config JSON 结构一一对应） */
export interface ModelLlmConfig {
  provider: ModelProvider;
  supportMultimodal: boolean;
  multimodalOptions: MultimodalOptionsConfig;
  dashscope: DashscopeConfig;
  openai: OpenaiConfig;
  gemini: GeminiConfig;
  anthropic: AnthropicConfig;
  generateOptions: GenerateOptionsConfig;
}

export interface ProviderInfo<T extends string> {
  key: T;
  label: string;
  description: string;
}

export interface EmbeddingProviderConfig {
  apiKey: string;
  modelName: string;
  baseUrl: string;
  dimensions?: number;
  batchSize?: number;
}

export interface DashscopeEmbeddingConfig extends EmbeddingProviderConfig {}

export interface OllamaEmbeddingConfig {
  modelName: string;
  baseUrl: string;
  dimensions?: number;
  batchSize?: number;
}

export interface ModelEmbeddingConfig {
  enabled: boolean;
  provider: EmbeddingProvider;
  dashscope: DashscopeEmbeddingConfig;
  openai: EmbeddingProviderConfig;
  ollama: OllamaEmbeddingConfig;
}

export interface RerankProviderConfig {
  apiKey: string;
  modelName: string;
  baseUrl: string;
}

export interface DashscopeRerankConfig extends RerankProviderConfig {
  instruct?: string;
}

export interface ModelRerankConfig {
  enabled: boolean;
  provider: RerankProvider;
  topN: number;
  dashscope: DashscopeRerankConfig;
  jina: RerankProviderConfig;
  xinference: RerankProviderConfig;
}

export type ModelTabKey = 'llm' | 'embedding' | 'rerank';

export const DEFAULT_DASHSCOPE_CONFIG: DashscopeConfig = {
  apiKey: '',
  modelName: 'qwen-plus',
  stream: true,
  enableThinking: false,
  enableSearch: false,
  baseUrl: '',
};

export const DEFAULT_OPENAI_CONFIG: OpenaiConfig = {
  apiKey: '',
  modelName: 'gpt-4.1-mini',
  stream: true,
  baseUrl: '',
  endpointPath: '',
};

export const DEFAULT_GEMINI_CONFIG: GeminiConfig = {
  apiKey: '',
  modelName: 'gemini-2.0-flash',
  stream: true,
  project: '',
  location: 'us-central1',
};

export const DEFAULT_ANTHROPIC_CONFIG: AnthropicConfig = {
  apiKey: '',
  modelName: 'claude-sonnet-4-5-20250929',
  stream: true,
  baseUrl: '',
};

export const DEFAULT_GENERATE_OPTIONS: GenerateOptionsConfig = {
  temperature: 0.2,
  topP: 0.75,
  frequencyPenalty: 0.5,
  presencePenalty: 0.5,
  additionalBodyParams: {},
};

export const DEFAULT_MULTIMODAL_OPTIONS: MultimodalOptionsConfig = {
  resourceUrlToBase64: false,
  maxUploadSizeMb: 20,
  maxUploadCount: 3,
  allowedUploadFormats: undefined,
};

export const PROVIDER_LIST: ProviderInfo<ModelProvider>[] = [
  { key: 'dashscope', label: 'DashScope', description: '阿里云通义千问' },
  { key: 'openai', label: 'OpenAI', description: 'GPT 系列模型' },
  { key: 'gemini', label: 'Gemini', description: 'Google Gemini 模型' },
  { key: 'anthropic', label: 'Anthropic', description: 'Claude 系列模型' },
];

export const EMBEDDING_PROVIDER_LIST: ProviderInfo<EmbeddingProvider>[] = [
  { key: 'dashscope', label: 'DashScope', description: '通义文本向量' },
  { key: 'openai', label: 'OpenAI', description: 'OpenAI Embeddings' },
  { key: 'ollama', label: 'Ollama', description: '本地向量模型' },
];

export const RERANK_PROVIDER_LIST: ProviderInfo<RerankProvider>[] = [
  { key: 'dashscope', label: 'DashScope', description: '通义重排模型' },
  { key: 'jina', label: 'Jina', description: 'Jina AI 重排模型' },
  { key: 'xinference', label: 'Xinference', description: '自托管重排模型' },
];

export function createDefaultModelLlmConfig(): ModelLlmConfig {
  return {
    provider: 'openai',
    supportMultimodal: false,
    multimodalOptions: { ...DEFAULT_MULTIMODAL_OPTIONS },
    dashscope: { ...DEFAULT_DASHSCOPE_CONFIG },
    openai: { ...DEFAULT_OPENAI_CONFIG },
    gemini: { ...DEFAULT_GEMINI_CONFIG },
    anthropic: { ...DEFAULT_ANTHROPIC_CONFIG },
    generateOptions: { ...DEFAULT_GENERATE_OPTIONS },
  };
}

export function createDefaultModelEmbeddingConfig(): ModelEmbeddingConfig {
  return {
    enabled: true,
    provider: 'dashscope',
    dashscope: {
      apiKey: '',
      modelName: 'text-embedding-v4',
      baseUrl: '',
      dimensions: 1024,
      batchSize: 16,
    },
    openai: {
      apiKey: '',
      modelName: 'text-embedding-3-small',
      baseUrl: '',
      dimensions: 1536,
      batchSize: 16,
    },
    ollama: {
      modelName: 'nomic-embed-text',
      baseUrl: 'http://localhost:11434',
      batchSize: 16,
    },
  };
}

export function createDefaultModelRerankConfig(): ModelRerankConfig {
  return {
    enabled: true,
    provider: 'dashscope',
    topN: 10,
    dashscope: {
      apiKey: '',
      modelName: 'qwen3.7-text-rerank',
      baseUrl: '',
      instruct: '',
    },
    jina: {
      apiKey: '',
      modelName: 'jina-reranker-v2-base-multilingual',
      baseUrl: '',
    },
    xinference: {
      apiKey: '',
      modelName: '',
      baseUrl: 'http://localhost:9997',
    },
  };
}

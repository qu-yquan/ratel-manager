import React, {useEffect, useMemo, useState} from 'react';
import {Alert, App, Button, Form, Input, InputNumber, Modal, Radio, Select, Space, Steps, Switch} from 'antd';
import type {
  EnumOption,
  OAuthClientAuthenticationMethod,
  OAuthClientInfo,
  OAuthClientStatus,
  OAuthClientType,
  OAuthGrantType,
  OAuthScopeInfo,
} from '../../types';
import {linesToText, textToLines} from '../../utils';
import {getOAuthScopeOptions} from '../../services/oauthClient';
import styles from './index.module.less';

const {TextArea} = Input;

interface OAuthClientFormValues extends Omit<OAuthClientInfo, 'redirectUris' | 'postLogoutRedirectUris' | 'scopes'> {
  redirectUrisText?: string;
  postLogoutRedirectUrisText?: string;
  scopeCodes?: string[];
  inheritUserPermissions?: boolean;
}

const INHERIT_USER_PERMISSIONS_SCOPE = 'inherit_user_permissions';

interface OAuthClientFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  data?: OAuthClientInfo | null;
  clientTypeOptions: EnumOption<OAuthClientType>[];
  accountTypeOptions: EnumOption<string>[];
  statusOptions: EnumOption<OAuthClientStatus>[];
  grantTypeOptions: EnumOption<OAuthGrantType>[];
  authenticationMethodOptions: EnumOption<OAuthClientAuthenticationMethod>[];
  onSave: (data: OAuthClientInfo) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

const OAuthClientFormModal: React.FC<OAuthClientFormModalProps> = ({
  visible,
  mode,
  data,
  clientTypeOptions,
  accountTypeOptions,
  statusOptions,
  grantTypeOptions,
  authenticationMethodOptions,
  onSave,
  onClose,
  onSuccess,
}) => {
  const {message} = App.useApp();
  const [form] = Form.useForm<OAuthClientFormValues>();
  const [loading, setLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [scopeLoading, setScopeLoading] = useState(false);
  const [scopeOptions, setScopeOptions] = useState<OAuthScopeInfo[]>([]);
  const isEdit = mode === 'edit';
  const clientType = Form.useWatch('clientType', form);
  const grantTypes = Form.useWatch('authorizationGrantTypes', form) || [];
  const authenticationMethods = Form.useWatch('clientAuthenticationMethods', form) || [];
  const accountType = Form.useWatch('accountType', form);
  const inheritUserPermissions = Form.useWatch('inheritUserPermissions', form);
  const hasAuthorizationCode = grantTypes.includes('AUTHORIZATION_CODE');
  const hasClientCredentials = grantTypes.includes('CLIENT_CREDENTIALS');
  const hasDeviceCode = grantTypes.includes('DEVICE_CODE');
  const showAuthorizationConsent = hasAuthorizationCode || hasDeviceCode;

  const authMethodOptions = useMemo(() => {
    if (clientType && clientType !== 'CONFIDENTIAL') {
      return authenticationMethodOptions.filter((item) => item.value === 'NONE');
    }
    return authenticationMethodOptions.filter((item) => item.value !== 'NONE');
  }, [authenticationMethodOptions, clientType]);

  const stepFieldNames: Array<Array<keyof OAuthClientFormValues>> = [
    ['accountType'],
    ['clientName', 'clientType', 'status', 'remark'],
    [
      'authorizationGrantTypes',
      'clientAuthenticationMethods',
      ...(hasAuthorizationCode
        ? (['redirectUrisText', 'postLogoutRedirectUrisText', 'requireProofKey'] as Array<keyof OAuthClientFormValues>)
        : []),
      ...(showAuthorizationConsent
        ? (['requireAuthorizationConsent'] as Array<keyof OAuthClientFormValues>)
        : []),
    ],
    ['scopeCodes', 'inheritUserPermissions'],
    [
      'reuseRefreshTokens',
      'accessTokenTtlSeconds',
      'refreshTokenTtlSeconds',
      ...(hasAuthorizationCode ? (['authorizationCodeTtlSeconds'] as Array<keyof OAuthClientFormValues>) : []),
      ...(hasDeviceCode ? (['deviceCodeTtlSeconds'] as Array<keyof OAuthClientFormValues>) : []),
    ],
  ];

  useEffect(() => {
    if (!visible) {
      return;
    }
    setCurrentStep(0);
    if (data) {
      form.setFieldsValue({
        ...data,
        redirectUrisText: linesToText(data.redirectUris),
        postLogoutRedirectUrisText: linesToText(data.postLogoutRedirectUris),
        scopeCodes: (data.scopes || []).filter(scope => scope !== INHERIT_USER_PERMISSIONS_SCOPE),
        inheritUserPermissions: (data.scopes || []).includes(INHERIT_USER_PERMISSIONS_SCOPE),
      });
      return;
    }
    form.resetFields();
    form.setFieldsValue({
      clientType: 'CONFIDENTIAL',
      accountType: 'MANAGER',
      status: 'ENABLED',
      clientAuthenticationMethods: ['CLIENT_SECRET_BASIC'],
      authorizationGrantTypes: ['AUTHORIZATION_CODE'],
      redirectUrisText: '',
      postLogoutRedirectUrisText: '',
      requireProofKey: true,
      requireAuthorizationConsent: true,
      accessTokenTtlSeconds: 7200,
      refreshTokenTtlSeconds: 2592000,
      authorizationCodeTtlSeconds: 300,
      deviceCodeTtlSeconds: 300,
      reuseRefreshTokens: false,
      scopeCodes: [],
      inheritUserPermissions: false,
    });
  }, [data, form, visible]);

  useEffect(() => {
    if (!visible || !accountType) return;
    let active = true;
    setScopeLoading(true);
    getOAuthScopeOptions(accountType)
      .then(options => {
        if (!active) return;
        const regularScopes = (options || []).filter(scope => scope.scopeCode !== INHERIT_USER_PERMISSIONS_SCOPE);
        const availableCodes = new Set(regularScopes.map(scope => scope.scopeCode));
        setScopeOptions(regularScopes);
        form.setFieldValue('scopeCodes', (form.getFieldValue('scopeCodes') || [])
          .filter((scope: string) => availableCodes.has(scope)));
      })
      .catch(() => {
        if (active) {
          setScopeOptions([]);
          message.error('加载Scope选项失败');
        }
      })
      .finally(() => {
        if (active) setScopeLoading(false);
      });
    return () => { active = false; };
  }, [accountType, form, message, visible]);

  useEffect(() => {
    if (visible && grantTypes.length > 0 && !showAuthorizationConsent) {
      form.setFieldValue('inheritUserPermissions', false);
    }
  }, [form, grantTypes.length, showAuthorizationConsent, visible]);

  useEffect(() => {
    if (visible && inheritUserPermissions && showAuthorizationConsent) {
      form.setFieldValue('requireAuthorizationConsent', true);
    }
  }, [form, inheritUserPermissions, showAuthorizationConsent, visible]);

  useEffect(() => {
    if (!visible || !clientType) {
      return;
    }
    if (clientType === 'CONFIDENTIAL') {
      if (authenticationMethods.length === 0 || authenticationMethods.includes('NONE')) {
        form.setFieldValue('clientAuthenticationMethods', ['CLIENT_SECRET_BASIC']);
      }
      return;
    }
    form.setFieldValue('clientAuthenticationMethods', ['NONE']);
    if (grantTypes.includes('CLIENT_CREDENTIALS')) {
      form.setFieldValue('authorizationGrantTypes', grantTypes.filter((item) => item !== 'CLIENT_CREDENTIALS'));
    }
  }, [authenticationMethods, clientType, form, grantTypes, visible]);

  const handleNext = async () => {
    try {
      await form.validateFields(stepFieldNames[currentStep]);
      setCurrentStep((step) => step + 1);
    } catch {
      // 表单校验失败
    }
  };

  const handlePrev = () => {
    setCurrentStep((step) => step - 1);
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const {
        redirectUrisText,
        postLogoutRedirectUrisText,
        scopeCodes,
        inheritUserPermissions,
        ...restValues
      } = values;
      const selectedGrantTypes = restValues.authorizationGrantTypes || [];
      const selectedHasAuthorizationCode = selectedGrantTypes.includes('AUTHORIZATION_CODE');
      const selectedHasDeviceCode = selectedGrantTypes.includes('DEVICE_CODE');
      const reqData: OAuthClientInfo = {
        ...restValues,
        id: isEdit ? data?.id : undefined,
        redirectUris: selectedHasAuthorizationCode ? textToLines(redirectUrisText) : [],
        postLogoutRedirectUris: selectedHasAuthorizationCode ? textToLines(postLogoutRedirectUrisText) : [],
        scopes: [
          ...(scopeCodes || []),
          ...(inheritUserPermissions ? [INHERIT_USER_PERMISSIONS_SCOPE] : []),
        ],
        requireProofKey: selectedHasAuthorizationCode ? Boolean(restValues.requireProofKey) : true,
        requireAuthorizationConsent: selectedHasAuthorizationCode || selectedHasDeviceCode
          ? Boolean(restValues.requireAuthorizationConsent)
          : false,
        authorizationCodeTtlSeconds: selectedHasAuthorizationCode ? restValues.authorizationCodeTtlSeconds : undefined,
        deviceCodeTtlSeconds: selectedHasDeviceCode ? restValues.deviceCodeTtlSeconds : undefined,
      };
      const success = await onSave(reqData);
      if (success) {
        onSuccess();
      }
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  const footer = (
    <Space>
      <Button onClick={onClose}>取消</Button>
      {currentStep > 0 && <Button onClick={handlePrev}>上一步</Button>}
      {currentStep < 4 && <Button type="primary" onClick={handleNext}>下一步</Button>}
      {currentStep === 4 && (
        <Button type="primary" data-ai-approval loading={loading} onClick={handleOk}>
          保存
        </Button>
      )}
    </Space>
  );

  return (
    <Modal
      title={isEdit ? '编辑OAuth应用' : '新增OAuth应用'}
      open={visible}
      width={760}
      footer={footer}
      onCancel={onClose}
      destroyOnHidden
      className={styles.formModal}
    >
      <Form form={form} layout="vertical">
        <Steps
          current={currentStep}
          className={styles.steps}
          items={[
            {title: '账号体系'},
            {title: '基础信息'},
            {title: '授权模式'},
            {title: '权限配置'},
            {title: 'Token有效期'},
          ]}
        />

        <div className={currentStep === 0 ? styles.stepPanel : styles.hiddenPanel}>
          <Form.Item name="accountType" label="账号体系" rules={[{required: true, message: '请选择账号体系'}]}>
            <Radio.Group className={styles.accountSystemGroup}>
              {accountTypeOptions.map((item) => (
                <Radio.Button key={item.value} value={item.value}>
                  {item.label}
                </Radio.Button>
              ))}
            </Radio.Group>
          </Form.Item>
        </div>

        <div className={currentStep === 1 ? styles.stepPanel : styles.hiddenPanel}>
          <div className={styles.twoColumn}>
            {isEdit && (
              <Form.Item name="clientId" label="客户端ID">
                <Input disabled/>
              </Form.Item>
            )}
            <Form.Item name="clientName" label="应用名称" rules={[{required: true, message: '请输入应用名称'}]}>
              <Input placeholder="请输入应用名称"/>
            </Form.Item>
            <Form.Item name="clientType" label="客户端类型" rules={[{required: true, message: '请选择客户端类型'}]}>
              <Select options={clientTypeOptions}/>
            </Form.Item>
            <Form.Item name="status" label="状态" rules={[{required: true, message: '请选择状态'}]}>
              <Select options={statusOptions}/>
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注">
            <TextArea rows={3} maxLength={512} showCount placeholder="请输入备注"/>
          </Form.Item>
        </div>

        <div className={currentStep === 2 ? styles.stepPanel : styles.hiddenPanel}>
          <Form.Item
            name="authorizationGrantTypes"
            label="授权模式"
            rules={[
              {required: true, message: '请选择授权模式'},
              {
                validator: (_, value: OAuthGrantType[]) => {
                  if ((value || []).includes('CLIENT_CREDENTIALS') && clientType !== 'CONFIDENTIAL') {
                    return Promise.reject(new Error('客户端凭证模式仅支持保密客户端'));
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <Select mode="multiple" options={grantTypeOptions}/>
          </Form.Item>
          <Form.Item
            name="clientAuthenticationMethods"
            label="客户端认证方式"
            rules={[{required: true, message: '请选择客户端认证方式'}]}
          >
            <Select mode="multiple" options={authMethodOptions}/>
          </Form.Item>
          {hasAuthorizationCode && (
            <>
              <Form.Item
                name="redirectUrisText"
                label="重定向URI白名单"
                rules={[{required: true, message: '请输入重定向URI'}]}
              >
                <TextArea rows={3} placeholder="每行一个URI"/>
              </Form.Item>
              <Form.Item name="postLogoutRedirectUrisText" label="退出后重定向URI白名单">
                <TextArea rows={2} placeholder="每行一个URI"/>
              </Form.Item>
              <Form.Item name="requireProofKey" label="要求PKCE" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
              </Form.Item>
            </>
          )}
          {showAuthorizationConsent && (
            <Form.Item name="requireAuthorizationConsent" label="要求授权确认" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
            </Form.Item>
          )}
          {!hasAuthorizationCode && !showAuthorizationConsent && (
            <div className={styles.emptyTip}>当前授权模式无需配置重定向URI、PKCE或授权确认。</div>
          )}
          {hasClientCredentials && (
            <div className={styles.tip}>客户端凭证模式不需要重定向URI，仅支持保密客户端。</div>
          )}
        </div>

        <div className={currentStep === 3 ? styles.stepPanel : styles.hiddenPanel}>
          <Form.Item name="scopeCodes" label="授权范围">
            <Select
              mode="multiple"
              allowClear
              loading={scopeLoading}
              placeholder="请选择应用允许申请的Scope"
              optionLabelProp="label"
              options={scopeOptions.map(scope => ({
                value: scope.scopeCode,
                label: scope.scopeCode,
                title: scope.scopeName,
                description: scope.description,
              }))}
              optionRender={option => (
                <div className={styles.scopeOption}>
                  <span>{option.data.title} ({option.data.value})</span>
                  {option.data.description && <span className={styles.scopeDescription}>{option.data.description}</span>}
                </div>
              )}
            />
          </Form.Item>
          <Form.Item name="inheritUserPermissions" label="允许继承用户所有权限" valuePropName="checked">
            <Switch disabled={!showAuthorizationConsent} checkedChildren="允许" unCheckedChildren="不允许"/>
          </Form.Item>
          <Alert
            type="info"
            showIcon
            message={showAuthorizationConsent
              ? '开启后会自动添加 inherit_user_permissions，并启用授权确认，用户可在授权页面拒绝该权限。'
              : '当前授权模式没有用户参与，不能继承用户权限。'}
          />
        </div>

        <div className={currentStep === 4 ? styles.stepPanel : styles.hiddenPanel}>
          <div className={styles.twoColumn}>
            <Form.Item name="reuseRefreshTokens" label="复用Refresh Token" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
            </Form.Item>
            <Form.Item name="accessTokenTtlSeconds" label="Access Token秒数">
              <InputNumber min={60} style={{width: '100%'}}/>
            </Form.Item>
            <Form.Item name="refreshTokenTtlSeconds" label="Refresh Token秒数">
              <InputNumber min={60} style={{width: '100%'}}/>
            </Form.Item>
            {hasAuthorizationCode && (
              <Form.Item name="authorizationCodeTtlSeconds" label="授权码秒数">
                <InputNumber min={60} style={{width: '100%'}}/>
              </Form.Item>
            )}
            {hasDeviceCode && (
              <Form.Item name="deviceCodeTtlSeconds" label="设备码秒数">
                <InputNumber min={60} style={{width: '100%'}}/>
              </Form.Item>
            )}
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default OAuthClientFormModal;

import React, {useEffect, useMemo, useState} from 'react';
import {Form, Input, InputNumber, Modal, Select, Switch} from 'antd';
import type {
  EnumOption,
  OAuthClientAuthenticationMethod,
  OAuthClientInfo,
  OAuthClientStatus,
  OAuthClientType,
  OAuthGrantType,
} from '../../types';
import {linesToText, textToLines} from '../../utils';
import styles from './index.module.less';

const {TextArea} = Input;

interface OAuthClientFormValues extends Omit<OAuthClientInfo, 'redirectUris' | 'postLogoutRedirectUris' | 'scopes'> {
  redirectUrisText?: string;
  postLogoutRedirectUrisText?: string;
  scopesText?: string;
}

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
  const [form] = Form.useForm<OAuthClientFormValues>();
  const [loading, setLoading] = useState(false);
  const isEdit = mode === 'edit';
  const clientType = Form.useWatch('clientType', form);
  const grantTypes = Form.useWatch('authorizationGrantTypes', form) || [];

  const authMethodOptions = useMemo(() => {
    if (clientType && clientType !== 'CONFIDENTIAL') {
      return authenticationMethodOptions.filter((item) => item.value === 'NONE');
    }
    return authenticationMethodOptions.filter((item) => item.value !== 'NONE');
  }, [authenticationMethodOptions, clientType]);

  useEffect(() => {
    if (!visible) {
      return;
    }
    if (data) {
      form.setFieldsValue({
        ...data,
        redirectUrisText: linesToText(data.redirectUris),
        postLogoutRedirectUrisText: linesToText(data.postLogoutRedirectUris),
        scopesText: linesToText(data.scopes),
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
      scopesText: 'openid',
      requireProofKey: true,
      requireAuthorizationConsent: true,
      accessTokenTtlSeconds: 7200,
      refreshTokenTtlSeconds: 2592000,
      authorizationCodeTtlSeconds: 300,
      deviceCodeTtlSeconds: 300,
      reuseRefreshTokens: false,
    });
  }, [data, form, visible]);

  useEffect(() => {
    if (!visible) {
      return;
    }
    if (clientType && clientType !== 'CONFIDENTIAL') {
      form.setFieldValue('clientAuthenticationMethods', ['NONE']);
    }
  }, [clientType, form, visible]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const {
        redirectUrisText,
        postLogoutRedirectUrisText,
        scopesText,
        ...restValues
      } = values;
      const reqData: OAuthClientInfo = {
        ...restValues,
        id: isEdit ? data?.id : undefined,
        redirectUris: textToLines(redirectUrisText),
        postLogoutRedirectUris: textToLines(postLogoutRedirectUrisText),
        scopes: textToLines(scopesText),
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

  return (
    <Modal
      title={isEdit ? '编辑OAuth应用' : '新增OAuth应用'}
      open={visible}
      width={760}
      okText="保存"
      cancelText="取消"
      okButtonProps={{'data-ai-approval': 'true'}}
      confirmLoading={loading}
      onOk={handleOk}
      onCancel={onClose}
      destroyOnHidden
      className={styles.formModal}
    >
      <Form form={form} layout="vertical">
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
          <Form.Item name="accountType" label="账号类型" rules={[{required: true, message: '请选择账号类型'}]}>
            <Select options={accountTypeOptions}/>
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{required: true, message: '请选择状态'}]}>
            <Select options={statusOptions}/>
          </Form.Item>
        </div>

        <div className={styles.sectionTitle}>授权配置</div>
        <Form.Item name="authorizationGrantTypes" label="授权模式" rules={[{required: true, message: '请选择授权模式'}]}>
          <Select mode="multiple" options={grantTypeOptions}/>
        </Form.Item>
        <Form.Item
          name="clientAuthenticationMethods"
          label="客户端认证方式"
          rules={[{required: true, message: '请选择客户端认证方式'}]}
        >
          <Select mode="multiple" options={authMethodOptions}/>
        </Form.Item>
        <Form.Item name="scopesText" label="授权范围" rules={[{required: true, message: '请输入授权范围'}]}>
          <TextArea rows={3} placeholder="每行一个scope，例如 openid"/>
        </Form.Item>
        <Form.Item
          name="redirectUrisText"
          label="重定向URI白名单"
          rules={grantTypes.includes('AUTHORIZATION_CODE') ? [{required: true, message: '请输入重定向URI'}] : []}
        >
          <TextArea rows={3} placeholder="每行一个URI"/>
        </Form.Item>
        <Form.Item name="postLogoutRedirectUrisText" label="退出后重定向URI白名单">
          <TextArea rows={2} placeholder="每行一个URI"/>
        </Form.Item>

        <div className={styles.twoColumn}>
          <Form.Item name="requireProofKey" label="要求PKCE" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
          </Form.Item>
          <Form.Item name="requireAuthorizationConsent" label="要求授权确认" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
          </Form.Item>
          <Form.Item name="reuseRefreshTokens" label="复用Refresh Token" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用"/>
          </Form.Item>
        </div>

        <div className={styles.sectionTitle}>令牌有效期</div>
        <div className={styles.twoColumn}>
          <Form.Item name="accessTokenTtlSeconds" label="Access Token秒数">
            <InputNumber min={60} style={{width: '100%'}}/>
          </Form.Item>
          <Form.Item name="refreshTokenTtlSeconds" label="Refresh Token秒数">
            <InputNumber min={60} style={{width: '100%'}}/>
          </Form.Item>
          <Form.Item name="authorizationCodeTtlSeconds" label="授权码秒数">
            <InputNumber min={60} style={{width: '100%'}}/>
          </Form.Item>
          <Form.Item name="deviceCodeTtlSeconds" label="设备码秒数">
            <InputNumber min={60} style={{width: '100%'}}/>
          </Form.Item>
        </div>

        <Form.Item name="remark" label="备注">
          <TextArea rows={3} maxLength={512} showCount placeholder="请输入备注"/>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default OAuthClientFormModal;

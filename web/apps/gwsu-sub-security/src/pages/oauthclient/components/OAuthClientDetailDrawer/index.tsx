import React from 'react';
import {Descriptions, Drawer, Tag} from 'antd';
import type {
  OAuthClientAuthenticationMethod,
  OAuthClientInfo,
  OAuthClientStatus,
  OAuthClientType,
  OAuthGrantType,
} from '../../types';
import styles from './index.module.less';

interface OAuthClientDetailDrawerProps {
  visible: boolean;
  data?: OAuthClientInfo | null;
  clientTypeLabels: Map<OAuthClientType, string>;
  accountTypeLabels: Map<string, string>;
  statusLabels: Map<OAuthClientStatus, string>;
  grantTypeLabels: Map<OAuthGrantType, string>;
  authenticationMethodLabels: Map<OAuthClientAuthenticationMethod, string>;
  onClose: () => void;
}

const renderTags = (values: string[] | undefined, labels?: Map<string, string>) => (
  <div className={styles.tagList}>
    {(values || []).map((value) => <Tag key={value}>{labels?.get(value) || value}</Tag>)}
  </div>
);

const renderMonoList = (values: string[] | undefined) => (
  <div className={styles.monoList}>
    {(values || []).map((value) => <div key={value} className={styles.monoItem}>{value}</div>)}
  </div>
);

const OAuthClientDetailDrawer: React.FC<OAuthClientDetailDrawerProps> = ({
  visible,
  data,
  clientTypeLabels,
  accountTypeLabels,
  statusLabels,
  grantTypeLabels,
  authenticationMethodLabels,
  onClose,
}) => (
  <Drawer
    title="OAuth应用详情"
    open={visible}
    width={640}
    onClose={onClose}
    className={styles.detailDrawer}
    destroyOnHidden
  >
    {data && (
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="客户端ID">{data.clientId}</Descriptions.Item>
          <Descriptions.Item label="应用名称">{data.clientName}</Descriptions.Item>
        <Descriptions.Item label="客户端类型">{clientTypeLabels.get(data.clientType) || data.clientType}</Descriptions.Item>
        <Descriptions.Item label="账号类型">
          {accountTypeLabels.get(data.accountType || '') || data.accountType || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={data.status === 'ENABLED' ? 'green' : 'red'}>{statusLabels.get(data.status) || data.status}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="授权模式">{renderTags(data.authorizationGrantTypes, grantTypeLabels)}</Descriptions.Item>
        <Descriptions.Item label="认证方式">{renderTags(data.clientAuthenticationMethods, authenticationMethodLabels)}</Descriptions.Item>
        <Descriptions.Item label="授权范围">{renderTags(data.scopes)}</Descriptions.Item>
        <Descriptions.Item label="重定向URI">{renderMonoList(data.redirectUris)}</Descriptions.Item>
        <Descriptions.Item label="退出后重定向URI">{renderMonoList(data.postLogoutRedirectUris)}</Descriptions.Item>
        <Descriptions.Item label="要求PKCE">{data.requireProofKey ? '是' : '否'}</Descriptions.Item>
        <Descriptions.Item label="要求授权确认">{data.requireAuthorizationConsent ? '是' : '否'}</Descriptions.Item>
        <Descriptions.Item label="Access Token秒数">{data.accessTokenTtlSeconds}</Descriptions.Item>
        <Descriptions.Item label="Refresh Token秒数">{data.refreshTokenTtlSeconds}</Descriptions.Item>
        <Descriptions.Item label="授权码秒数">{data.authorizationCodeTtlSeconds}</Descriptions.Item>
        <Descriptions.Item label="设备码秒数">{data.deviceCodeTtlSeconds}</Descriptions.Item>
        <Descriptions.Item label="备注">{data.remark || '-'}</Descriptions.Item>
      </Descriptions>
    )}
  </Drawer>
);

export default OAuthClientDetailDrawer;

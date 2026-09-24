
// @ts-ignore
import { Link } from 'umi';
import { Card, Typography, Button, Space } from 'antd';
import { SecurityScanOutlined, SafetyOutlined, FileProtectOutlined, AuditOutlined } from '@ant-design/icons';

const { Title, Paragraph } = Typography;

export default function Index() {
  const securityFeatures = [
    {
      icon: <SecurityScanOutlined />,
      title: '安全扫描',
      description: '实时监控系统安全状态',
    },
    {
      icon: <SafetyOutlined />,
      title: '访问控制',
      description: '管理用户权限和访问策略',
    },
    {
      icon: <FileProtectOutlined />,
      title: '数据保护',
      description: '加密存储和传输敏感数据',
    },
    {
      icon: <AuditOutlined />,
      title: '审计日志',
      description: '记录和追踪系统操作',
    },
  ];

  return (
    <div style={{ 
      padding: '24px', 
      minHeight: '100vh',
    }}>
      <Card
        style={{
          maxWidth: '800px',
          margin: '0 auto',
          borderRadius: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{ 
            fontSize: '48px', 
            marginBottom: '16px',
          }}>
            <SafetyOutlined />
          </div>
          <Title level={2} style={{ marginBottom: '8px' }}>
            安全中心
          </Title>
          <Paragraph style={{ opacity: 0.7 }}>
            全面保护您的系统安全
          </Paragraph>
        </div>

        <Space orientation="vertical" size="large" style={{ width: '100%' }}>
          {securityFeatures.map((feature, index) => (
            <Card
              key={index}
              size="small"
              style={{
                borderRadius: '8px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div style={{ 
                  fontSize: '24px', 
                }}>
                  {feature.icon}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ 
                    fontSize: '16px', 
                    fontWeight: 500,
                    marginBottom: '4px',
                  }}>
                    {feature.title}
                  </div>
                  <div style={{ opacity: 0.7 }}>
                    {feature.description}
                  </div>
                </div>
                <Button type="primary" size="small">
                  查看详情
                </Button>
              </div>
            </Card>
          ))}
        </Space>

        <div style={{ marginTop: '32px', textAlign: 'center' }}>
          <Link to="/">
            <Button>返回主页</Button>
          </Link>
        </div>
      </Card>
    </div>
  );
}

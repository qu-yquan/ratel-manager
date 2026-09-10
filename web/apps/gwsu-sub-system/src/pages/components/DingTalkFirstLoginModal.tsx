import React, {useEffect, useRef, useState} from 'react';
import {LinkOutlined, SafetyCertificateOutlined, UserAddOutlined} from '@ant-design/icons';
import {Alert, App, Form, Input, Modal, Tabs} from 'antd';
import {encryptPassword} from '@gwsu/core';
import {
    buildDingTalkCompleteParams,
    completeDingTalkLogin,
    login,
    LoginToken,
    TerminalType,
} from '../../services/login';
import CaptchaVerify, {type CaptchaPass} from './CaptchaVerify';
import styles from './DingTalkFirstLoginModal.module.less';

type AccountMethod = 'binding' | 'create';

interface DingTalkFirstLoginModalProps {
    open: boolean;
    temporaryVoucher: string | null;
    onCancel: () => void;
    onSuccess: (token: LoginToken) => Promise<void>;
}

interface AccountFormValues {
    username: string;
    password: string;
    confirmPassword?: string;
}

interface BindingCredentials {
    username: string;
    encryptedPassword: string;
}

const DingTalkFirstLoginModal: React.FC<DingTalkFirstLoginModalProps> = ({
    open,
    temporaryVoucher,
    onCancel,
    onSuccess,
}) => {
    const {message} = App.useApp();
    const [form] = Form.useForm<AccountFormValues>();
    const [method, setMethod] = useState<AccountMethod>('binding');
    const [submitting, setSubmitting] = useState(false);
    const [captchaOpen, setCaptchaOpen] = useState(false);
    const bindingCredentialsRef = useRef<BindingCredentials | null>(null);
    const validationSequenceRef = useRef(0);

    const clearBindingVerification = () => {
        validationSequenceRef.current += 1;
        bindingCredentialsRef.current = null;
        setCaptchaOpen(false);
    };

    useEffect(() => {
        clearBindingVerification();
        if (open) {
            setMethod('binding');
            form.resetFields();
        }

        return () => {
            validationSequenceRef.current += 1;
            bindingCredentialsRef.current = null;
        };
    }, [form, open]);

    useEffect(() => {
        if (!temporaryVoucher) {
            clearBindingVerification();
        }
    }, [temporaryVoucher]);

    const handleMethodChange = (activeKey: string) => {
        setMethod(activeKey as AccountMethod);
        clearBindingVerification();
        form.resetFields();
    };

    const handleSubmit = async () => {
        if (submitting || captchaOpen) {
            return;
        }
        const currentTemporaryVoucher = temporaryVoucher;
        if (!currentTemporaryVoucher) {
            message.error('临时凭证已失效，请重新进行钉钉登录');
            return;
        }
        const validationSequence = ++validationSequenceRef.current;

        try {
            const values = await form.validateFields();
            if (validationSequence !== validationSequenceRef.current) {
                return;
            }
            if (method === 'binding') {
                bindingCredentialsRef.current = {
                    username: values.username.trim(),
                    encryptedPassword: encryptPassword(values.password),
                };
                setCaptchaOpen(true);
                return;
            }

            setSubmitting(true);
            const finalToken = await completeDingTalkLogin(
                buildDingTalkCompleteParams('create', currentTemporaryVoucher, {
                    username: values.username.trim(),
                    password: encryptPassword(values.password),
                }),
            );
            await onSuccess(finalToken);
        } catch {
            // 请求层统一展示后端业务错误；保留弹框和表单以便用户修正后重试。
        } finally {
            setSubmitting(false);
        }
    };

    const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
        const bindingCredentials = bindingCredentialsRef.current;
        clearBindingVerification();

        if (!bindingCredentials || submitting) {
            return;
        }
        const currentTemporaryVoucher = temporaryVoucher;
        if (!currentTemporaryVoucher) {
            message.error('临时凭证已失效，请重新进行钉钉登录');
            return;
        }

        setSubmitting(true);
        try {
            // 仅用于证明已有账号身份，不写入前端登录状态。
            const passwordToken = await login({
                type: 'password',
                terminal: TerminalType.WEB,
                username: bindingCredentials.username,
                password: bindingCredentials.encryptedPassword,
                captchaId: captchaPass.captchaId,
                captchaCode: captchaPass.captchaCode,
            });
            const finalToken = await completeDingTalkLogin(
                buildDingTalkCompleteParams('binding', currentTemporaryVoucher, {
                    bindingToken: passwordToken.token,
                }),
            );
            await onSuccess(finalToken);
        } catch {
            // 请求层统一展示后端业务错误；保留弹框和表单以便用户修正后重试。
        } finally {
            setSubmitting(false);
        }
    };

    const formItems = (
        <Form form={form} layout="vertical" requiredMark={false} className={styles.form}>
            <Form.Item
                label={method === 'binding' ? '已有账号用户名' : '设置用户名'}
                name="username"
                normalize={(value: string) => value?.trimStart()}
                rules={[
                    {required: true, whitespace: true, message: '请输入用户名'},
                    {max: 50, message: '用户名不能超过 50 个字符'},
                ]}
            >
                <Input
                    size="large"
                    autoComplete="username"
                    placeholder={method === 'binding' ? '请输入已有账号用户名' : '请输入新账号用户名'}
                />
            </Form.Item>
            <Form.Item
                label={method === 'binding' ? '已有账号密码' : '设置密码'}
                name="password"
                rules={[
                    {required: true, message: '请输入密码'},
                    ...(method === 'create' ? [{min: 6, message: '密码至少6位'}] : []),
                ]}
            >
                <Input.Password
                    size="large"
                    autoComplete={method === 'binding' ? 'current-password' : 'new-password'}
                    placeholder="请输入密码"
                />
            </Form.Item>
            {method === 'create' && (
                <Form.Item
                    label="确认密码"
                    name="confirmPassword"
                    dependencies={['password']}
                    rules={[
                        {required: true, message: '请再次输入密码'},
                        ({getFieldValue}) => ({
                            validator(_, value) {
                                if (!value || getFieldValue('password') === value) {
                                    return Promise.resolve();
                                }
                                return Promise.reject(new Error('两次输入的密码不一致'));
                            },
                        }),
                    ]}
                >
                    <Input.Password
                        size="large"
                        autoComplete="new-password"
                        placeholder="请再次输入密码"
                    />
                </Form.Item>
            )}
        </Form>
    );

    return (
        <Modal
            open={open}
            width={520}
            title={null}
            centered
            maskClosable={false}
            keyboard={!submitting}
            closable={!submitting}
            destroyOnHidden
            okText={method === 'binding' ? '验证并绑定' : '创建并登录'}
            cancelText="稍后处理"
            confirmLoading={submitting}
            onOk={handleSubmit}
            onCancel={() => {
                if (!submitting) {
                    clearBindingVerification();
                    onCancel();
                }
            }}
            okButtonProps={{'data-ai-approval': 'true'}}
            cancelButtonProps={{disabled: submitting}}
            className={styles.modal}
        >
            <div className={styles.hero}>
                <div className={styles.brandIcon} aria-hidden="true">
                    <SafetyCertificateOutlined/>
                </div>
                <div>
                    <h2>完成钉钉首次登录</h2>
                    <p>钉钉身份验证已通过，请选择如何关联本系统账号。</p>
                </div>
            </div>

            <Alert
                type="info"
                showIcon
                className={styles.notice}
                message="账号关联后，下次可直接使用钉钉快捷登录"
            />

            <CaptchaVerify
                open={captchaOpen}
                onCancel={clearBindingVerification}
                onSuccess={handleCaptchaSuccess}
            />

            <Tabs
                activeKey={method}
                onChange={handleMethodChange}
                className={styles.tabs}
                items={[
                    {
                        key: 'binding',
                        label: <span><LinkOutlined/>绑定已有账号</span>,
                        children: formItems,
                    },
                    {
                        key: 'create',
                        label: <span><UserAddOutlined/>创建新账号</span>,
                        children: formItems,
                    },
                ]}
            />
        </Modal>
    );
};

export default DingTalkFirstLoginModal;

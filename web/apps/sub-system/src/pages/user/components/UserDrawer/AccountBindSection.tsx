import React, { useState } from "react";
import { App, Tag } from "antd";
import type { SysAccountBindDTO, SysAccountVO } from "../../types";
import { IDENTITY_TYPE_MAP } from "../../types";
import { bindAccount, unbindAccount } from "@/services/user";
import AccountBindForm from "./AccountBindForm";
import { AuthGate } from "@gwsu/core";
import { PERM_EDIT } from '../../permissionConstants';
import styles from './AccountBindSection.module.less';

interface AccountBindSectionProps {
  userId: string;
  accounts: SysAccountVO[];
  onRefresh: () => void;
  readOnly?: boolean;
}

const AccountBindSection: React.FC<AccountBindSectionProps> = ({
  userId,
  accounts,
  onRefresh,
  readOnly = false,
}) => {
  const { message, modal } = App.useApp();
  const [expandedType, setExpandedType] = useState<string | null>(null);

  const allTypes = ["password", "phone", "dingtalk"];
  const accountMap = new Map(accounts.map((a) => [a.identityType, a]));

  const handleBind =
    (_identityType: string) => async (data: SysAccountBindDTO) => {
      // 钉钉绑定时，需要确认提示
      if (_identityType === "dingtalk" && data.originalUserId) {
        return new Promise<void>((resolve, reject) => {
          modal.confirm({
            title: "操作确认",
            content: "绑定会注销原有账号，确定操作吗？",
            okButtonProps: { "data-ai-approval": "true" },
            onOk: async () => {
              try {
                await bindAccount(userId, data);
                message.success("绑定成功");
                setExpandedType(null);
                onRefresh();
                resolve();
              } catch (err) {
                reject(err);
              }
            },
            onCancel: () => {
              // 用户取消确认，表单保持打开，不关闭
              reject(new Error("用户取消"));
            },
          });
        });
      }

      try {
        await bindAccount(userId, data);
        message.success("绑定成功");
        setExpandedType(null);
        onRefresh();
      } catch (err) {}
    };

  const handleUnbind = (accountId: string) => {
    unbindAccount(userId, accountId)
      .then((_) => {
        message.success("解绑成功");
        onRefresh();
      })
      .catch((_) => {});
  };

  return (
    <div>
      <div className={styles.sectionTitle}>账号绑定</div>
      <div className={styles.bindList}>
        {allTypes.map((type) => {
          const account = accountMap.get(type);
          const typeInfo = IDENTITY_TYPE_MAP[type] || {
            label: type,
            icon: "\u{1F511}",
          };

          return (
            <div
              key={type}
              className={`${styles.bindItem} ${expandedType === type ? styles.bindItemExpanded : ""}`}
            >
              <div className={styles.bindItemHeader}>
                <div className={styles.bindItemInfo}>
                  <span className={styles.bindItemIcon}>{typeInfo.icon}</span>
                  <div>
                    <div className={styles.bindItemLabel}>{typeInfo.label}</div>
                    <div className={styles.bindItemIdentifier}>
                      {account ? account.identifier : "未绑定"}
                    </div>
                  </div>
                </div>
                <div className={styles.bindItemActions}>
                  {account ? (
                    <>
                      <Tag color="success">已绑定</Tag>
                      {!readOnly && type !== "password" && (
                        <AuthGate buttonKey={PERM_EDIT}>
                          <a
                            className={styles.unbindLink}
                            data-ai-approval
                            onClick={() => handleUnbind(account.id)}
                          >
                            解绑
                          </a>
                        </AuthGate>
                      )}
                    </>
                  ) : (
                    !readOnly && (
                      <AuthGate buttonKey={PERM_EDIT}>
                        <a onClick={() => setExpandedType(type)}>绑定</a>
                      </AuthGate>
                    )
                  )}
                </div>
              </div>
              {expandedType === type && (
                <div className={styles.bindFormWrapper}>
                  <AccountBindForm
                    identityType={type}
                    onSubmit={handleBind(type)}
                    onCancel={() => setExpandedType(null)}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AccountBindSection;

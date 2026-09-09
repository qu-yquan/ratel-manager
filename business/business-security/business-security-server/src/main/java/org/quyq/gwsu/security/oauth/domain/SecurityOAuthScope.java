package org.quyq.gwsu.security.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthScopeVO;
import org.quyq.gwsu.common.security.enums.AccountType;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("security_oauth_scope")
@Schema(description = "OAuth Scope")
public class SecurityOAuthScope extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String scopeCode;
    private String scopeName;
    private String description;
    private AccountType accountType;
    private OAuthClientStatus status;

    public OAuthScopeVO toVo() {
        OAuthScopeVO vo = new OAuthScopeVO();
        vo.setId(id);
        vo.setScopeCode(scopeCode);
        vo.setScopeName(scopeName);
        vo.setDescription(description);
        vo.setAccountType(accountType);
        vo.setStatus(status);
        vo.copyBaseProperties(this);
        return vo;
    }
}

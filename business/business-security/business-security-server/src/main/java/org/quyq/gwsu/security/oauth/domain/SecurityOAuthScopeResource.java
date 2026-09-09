package org.quyq.gwsu.security.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("security_oauth_scope_resource")
public class SecurityOAuthScopeResource extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String scopeId;
    private String apiResourceId;
}

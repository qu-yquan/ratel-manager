package org.quyq.gwsu.security.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScope;

@Mapper
public interface SecurityOAuthScopeMapper extends BaseMapper<SecurityOAuthScope> {
}

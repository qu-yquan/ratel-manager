package org.quyq.gwsu.security.oauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientSecretVO;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthClient;

/**
 * OAuth 应用配置服务。
 *
 * @author Quyq
 */
public interface ISecurityOAuthClientService extends IService<SecurityOAuthClient> {

    OAuthClientInfoVO getInfoById(String id);

    OAuthClientInfoVO getByClientId(String clientId);

    IPage<OAuthClientInfoVO> pageByCondition(OAuthClientQueryDTO query);

    OAuthClientSecretVO saveOrUpdateClient(OAuthClientSaveDTO dto);

    OAuthClientSecretVO resetSecret(String id);

}

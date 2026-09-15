package org.quyq.gwsu.log.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;

public interface LogLoginMapper extends BaseMapper<LogLogin> {

    IPage<LogLoginVO> selectPageVo(Page<LogLoginVO> page, @Param("query") LogLoginQueryDTO query);

    String selectAuthorizationIdByToken(@Param("tokenFingerprint") String tokenFingerprint,
                                        @Param("tokenKeyVersion") String tokenKeyVersion);
}

package org.quyq.gwsu.log.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.common.log.enums.LoginEndType;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;

import java.time.LocalDateTime;
import java.util.List;

public interface LogLoginMapper extends BaseMapper<LogLogin> {

    IPage<LogLoginVO> selectPageVo(Page<LogLoginVO> page, @Param("query") LogLoginQueryDTO query);

    LogLogin selectOpenByAuthorizationId(@Param("authorizationId") String authorizationId);

    int refreshSession(@Param("authorizationId") String authorizationId,
                       @Param("modifyTime") LocalDateTime modifyTime);

    int finishSession(@Param("id") String id,
                      @Param("endType") LoginEndType endType,
                      @Param("endTime") LocalDateTime endTime);

    List<String> selectExpiredIds(@Param("expiredBefore") LocalDateTime expiredBefore,
                                  @Param("batchSize") int batchSize);
}

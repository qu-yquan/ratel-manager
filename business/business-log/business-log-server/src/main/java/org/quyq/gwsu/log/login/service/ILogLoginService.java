package org.quyq.gwsu.log.login.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;

import java.util.List;

public interface ILogLoginService extends IService<LogLogin> {

    LogLoginVO getById(String id);
    IPage<LogLoginVO> pageByCondition(LogLoginQueryDTO query);
    Boolean saveLog(LogLoginVO vo);
    Boolean removeByIds(List<String> ids);
}

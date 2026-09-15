package org.quyq.gwsu.log.login.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.quyq.gwsu.common.log.enums.LoginEventType;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;
import org.quyq.gwsu.log.login.mapper.LogLoginMapper;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LogLoginServiceImpl extends ServiceImpl<LogLoginMapper, LogLogin>
        implements ILogLoginService {

    @Override
    public LogLoginVO getById(String id) {
        LogLogin entity = super.getById(id);
        return entity == null ? null : entity.toVo();
    }

    @Override
    public IPage<LogLoginVO> pageByCondition(LogLoginQueryDTO query) {
        return baseMapper.selectPageVo(new Page<>(query.getPageNum(), query.getPageSize()), query);
    }

    @Override
    public Boolean saveLog(LogLoginVO vo) {
        if (LoginEventType.LOGOUT == vo.getEventType()
                && !StringUtils.hasText(vo.getAuthorizationId())
                && StringUtils.hasText(vo.getTokenFingerprint())) {
            vo.setAuthorizationId(baseMapper.selectAuthorizationIdByToken(
                    vo.getTokenFingerprint(), vo.getTokenKeyVersion()));
        }
        return save(LogLogin.toDo(vo));
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }
}

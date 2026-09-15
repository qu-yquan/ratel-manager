package org.quyq.gwsu.log.login.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.quyq.gwsu.common.log.enums.LoginEndType;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;
import org.quyq.gwsu.log.login.mapper.LogLoginMapper;
import org.quyq.gwsu.log.login.monitor.LoginTokenMonitor;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogLoginServiceImpl extends ServiceImpl<LogLoginMapper, LogLogin>
        implements ILogLoginService {

    private final LoginTokenMonitor tokenMonitor;

    public LogLoginServiceImpl(LoginTokenMonitor tokenMonitor) {
        this.tokenMonitor = tokenMonitor;
    }

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
        if (vo.getAction() == null) {
            return false;
        }
        return switch (vo.getAction()) {
            case LOGIN -> saveLogin(vo);
            case TOKEN_REFRESH -> refreshToken(vo);
            case LOGOUT -> finishLogout(vo);
        };
    }

    private boolean saveLogin(LogLoginVO vo) {
        String token = vo.getToken();
        vo.setToken(null);
        if (vo.getLoginTime() == null) {
            vo.setLoginTime(LocalDateTime.now());
        }
        boolean saved = save(LogLogin.toDo(vo));
        if (saved && Boolean.TRUE.equals(vo.getStatus()) && StringUtils.hasText(token)) {
            tokenMonitor.register(vo.getAuthorizationId(), token);
        }
        return saved;
    }

    private boolean refreshToken(LogLoginVO vo) {
        String token = vo.getToken();
        if (!StringUtils.hasText(vo.getAuthorizationId()) || !StringUtils.hasText(token)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean updated = baseMapper.refreshSession(vo.getAuthorizationId(), now) > 0;
        if (updated) {
            tokenMonitor.register(vo.getAuthorizationId(), token);
        }
        return updated;
    }

    private boolean finishLogout(LogLoginVO vo) {
        if (!Boolean.TRUE.equals(vo.getStatus())) {
            return true;
        }
        String token = vo.getToken();
        String authorizationId = vo.getAuthorizationId();
        if (!StringUtils.hasText(authorizationId)) {
            authorizationId = tokenMonitor.getAuthorizationId(token);
        }
        LogLogin session = findOpenSession(authorizationId);
        if (session != null) {
            finishSession(session.getId(), LoginEndType.LOGOUT,
                    vo.getEndTime() == null ? LocalDateTime.now() : vo.getEndTime());
        }
        tokenMonitor.remove(token);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean finishExpiredSession(String authorizationId) {
        if (!StringUtils.hasText(authorizationId)) {
            return true;
        }
        LogLogin session = baseMapper.selectOpenByAuthorizationId(authorizationId);
        if (session == null) {
            return true;
        }
        finishSession(session.getId(), LoginEndType.EXPIRE, LocalDateTime.now());
        return true;
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }

    private LogLogin findOpenSession(String authorizationId) {
        if (!StringUtils.hasText(authorizationId)) {
            return null;
        }
        return baseMapper.selectOpenByAuthorizationId(authorizationId);
    }

    private void finishSession(String id, LoginEndType endType, LocalDateTime endTime) {
        baseMapper.finishSession(id, endType, endTime);
    }
}

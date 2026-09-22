package org.quyq.gwsu.log.operation.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;
import org.quyq.gwsu.log.operation.mapper.LogOperationMapper;
import org.quyq.gwsu.log.operation.service.ILogOperationService;
import org.quyq.gwsu.log.login.monitor.LoginTokenMonitor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 操作日志服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class LogOperationServiceImpl extends ServiceImpl<LogOperationMapper, LogOperation> implements ILogOperationService {

    private final LoginTokenMonitor tokenMonitor;

    @Override
    public LogOperationVO getById(String id) {
        LogOperation entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public IPage<LogOperationVO> pageByCondition(LogOperationQueryDTO query) {
        Page<LogOperationVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectPageVo(page, query);
    }

    @Override
    public LogOperationVO getTreeByTid(String tid) {
        List<LogOperation> logs = baseMapper.selectByTid(tid);
        LogOperation root = logs.stream()
                .filter(item -> "gateway".equals(item.getFromApp()) && !StringUtils.hasText(item.getParentId()))
                .findFirst()
                .orElse(null);
        if (root == null) {
            return null;
        }

        Map<String, List<LogOperation>> childrenByParent = logs.stream()
                .filter(item -> StringUtils.hasText(item.getParentId()))
                .collect(Collectors.groupingBy(
                        LogOperation::getParentId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return buildLogTree(root, childrenByParent);
    }

    private LogOperationVO buildLogTree(
            LogOperation entity,
            Map<String, List<LogOperation>> childrenByParent) {
        LogOperationVO vo = entity.toVo();
        List<LogOperation> children = childrenByParent.getOrDefault(entity.getId(), Collections.emptyList());
        if (children.isEmpty()) {
            vo.setHasChildren(false);
            return vo;
        }
        vo.setHasChildren(true);
        vo.setChildren(children.stream()
                .map(child -> buildLogTree(child, childrenByParent))
                .toList());
        return vo;
    }

    @Override
    public Boolean saveLog(LogOperationVO vo) {
        if (!StringUtils.hasText(vo.getAuthorizationId()) && StringUtils.hasText(vo.getTokenId())) {
            vo.setAuthorizationId(tokenMonitor.getAuthorizationId(vo.getTokenId()));
        }
        vo.setTokenId(null);
        LogOperation entity = LogOperation.toDo(vo);
        if(Objects.nonNull(entity.getResponseTime())){
            return updateById(entity);
        }
        return save(entity);
    }

    @Override
    public int removeExpiredBefore(LocalDateTime expiredBefore, int batchSize) {
        int removed = 0;
        while (true) {
            List<String> ids = baseMapper.selectExpiredIds(expiredBefore, batchSize);
            if (ids.isEmpty()) {
                return removed;
            }
            int affected = baseMapper.deleteByIds(ids);
            removed += affected;
            if (affected == 0 || ids.size() < batchSize) {
                return removed;
            }
        }
    }
}

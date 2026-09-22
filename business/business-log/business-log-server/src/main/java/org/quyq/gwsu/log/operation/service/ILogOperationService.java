package org.quyq.gwsu.log.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;

import java.time.LocalDateTime;

/**
 * 操作日志服务接口
 *
 * @author Quyq
 */
public interface ILogOperationService extends IService<LogOperation> {

    /**
     * 根据ID查询操作日志
     *
     * @param id 日志ID
     * @return 操作日志信息
     */
    LogOperationVO getById(String id);

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<LogOperationVO> pageByCondition(LogOperationQueryDTO query);

    /**
     * 根据链路标识查询树形操作日志。
     *
     * @param tid 链路标识
     * @return 以网关日志为根节点的完整调用链
     */
    LogOperationVO getTreeByTid(String tid);

    /**
     * 保存操作日志
     *
     * @param vo 操作日志VO
     * @return 是否成功
     */
    Boolean saveLog(LogOperationVO vo);

    /**
     * 分批物理删除指定时间之前的操作日志。
     *
     * @param expiredBefore 过期截止时间
     * @param batchSize 每批数量
     * @return 删除数量
     */
    int removeExpiredBefore(LocalDateTime expiredBefore, int batchSize);

}

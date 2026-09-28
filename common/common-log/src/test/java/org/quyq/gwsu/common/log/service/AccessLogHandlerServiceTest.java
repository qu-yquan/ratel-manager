package org.quyq.gwsu.common.log.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.common.log.vo.LogOperationVO;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccessLogHandlerServiceTest {

    @Test
    void shouldQueueIndependentRequestAndResponseSnapshots() throws Exception {
        BlockingQueue<LogOperationVO> receivedLogs = new LinkedBlockingQueue<>();
        ILogClientApi logClient = new ILogClientApi() {
            @Override
            public R<Boolean> saveOperLog(LogOperationVO vo) {
                receivedLogs.add(vo);
                return R.ok(true);
            }

            @Override
            public R<Boolean> saveLoginLog(LogLoginVO vo) {
                return R.ok(true);
            }
        };
        AccessLogHandlerService service = new AccessLogHandlerService(logClient, 1);
        service.afterPropertiesSet();

        try {
            LogOperationVO log = new LogOperationVO()
                    .setOperId("operation-id")
                    .setRequestTime(LocalDateTime.now())
                    .setStatus(false);
            service.save(log);
            LogOperationVO requestSnapshot = receivedLogs.poll(2, TimeUnit.SECONDS);

            LocalDateTime responseTime = LocalDateTime.now();
            log.setResponseTime(responseTime).setStatus(true);
            service.save(log);
            LogOperationVO responseSnapshot = receivedLogs.poll(2, TimeUnit.SECONDS);

            assertNotNull(requestSnapshot);
            assertNotNull(responseSnapshot);
            assertNotSame(log, requestSnapshot);
            assertNotSame(requestSnapshot, responseSnapshot);
            assertNull(requestSnapshot.getResponseTime());
            assertEquals(Boolean.FALSE, requestSnapshot.getStatus());
            assertEquals(responseTime, responseSnapshot.getResponseTime());
            assertEquals(Boolean.TRUE, responseSnapshot.getStatus());
        } finally {
            service.destroy();
        }
    }
}

package org.quyq.gwsu.common.log.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogIdUtilsTest {

    @AfterEach
    void clearLogId() {
        LogIdUtils.clear();
    }

    @Test
    void shouldReuseLogIdInCurrentThread() {
        String first = LogIdUtils.getLogId();

        assertEquals(first, LogIdUtils.getLogId());
    }

    @Test
    void shouldInheritLogIdInChildPlatformThread() throws Exception {
        String parentLogId = LogIdUtils.getLogId();
        CompletableFuture<String> childLogId = new CompletableFuture<>();

        Thread.ofPlatform().start(() -> childLogId.complete(LogIdUtils.getLogId())).join();

        assertEquals(parentLogId, childLogId.get());
    }

    @Test
    void shouldInheritLogIdInChildVirtualThread() throws Exception {
        String parentLogId = LogIdUtils.getLogId();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertEquals(parentLogId, executor.submit(LogIdUtils::getLogId).get());
        }
    }

    @Test
    void shouldGenerateNewLogIdAfterClear() {
        String previousLogId = LogIdUtils.getLogId();

        LogIdUtils.clear();

        assertNull(LogIdUtils.getCurrentLogId());
        assertNotEquals(previousLogId, LogIdUtils.getLogId());
    }
}

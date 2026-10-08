package org.quyq.gwsu.headless.service.impl;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.headless.graph.HeadlessGraphState;
import org.quyq.gwsu.headless.graph.HeadlessGraphStateSerializer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadlessServiceImplTest {

    @Test
    void shouldCreateChannelWithoutNullDefaultSupplier() {
        assertTrue(HeadlessServiceImpl.channelWithoutDefault().getDefault().isEmpty());
    }

    @Test
    void shouldCloneRuntimeStateWithoutJavaSerialization() throws Exception {
        Object runtimeValue = new Object();
        HeadlessGraphState state = new HeadlessGraphState(Map.of("runtime", runtimeValue));

        HeadlessGraphState cloned = new HeadlessGraphStateSerializer().cloneObject(state);

        assertSame(runtimeValue, cloned.value("runtime").orElseThrow());
    }
}

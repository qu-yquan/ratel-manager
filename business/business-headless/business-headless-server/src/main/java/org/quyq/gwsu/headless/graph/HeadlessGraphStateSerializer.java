package org.quyq.gwsu.headless.graph;

import org.bsc.langgraph4j.serializer.StateSerializer;

import java.io.IOException;
import java.util.HashMap;

/**
 * 无头工作流只在当前请求内运行，状态包含 Flux 等运行时对象，不适合持久化序列化。
 */
public final class HeadlessGraphStateSerializer extends StateSerializer<HeadlessGraphState> {

    public HeadlessGraphStateSerializer() {
        super(HeadlessGraphState::new);
    }

    @Override
    public HeadlessGraphState cloneObject(HeadlessGraphState state) {
        return new HeadlessGraphState(new HashMap<>(state.data()));
    }

    @Override
    public String contentType() {
        return "application/x-headless-graph-state";
    }

    @Override
    public String writeDataAsString(HeadlessGraphState state) throws IOException {
        throw new IOException("无头工作流状态不支持持久化序列化");
    }

    @Override
    public HeadlessGraphState readDataFromString(String data) throws IOException {
        throw new IOException("无头工作流状态不支持持久化反序列化");
    }
}

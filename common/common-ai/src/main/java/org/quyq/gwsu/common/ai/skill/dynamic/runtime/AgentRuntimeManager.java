package org.quyq.gwsu.common.ai.skill.dynamic.runtime;

import io.agentscope.core.agent.Agent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class AgentRuntimeManager {

    private final AtomicReference<AgentRuntimeSnapshot> current = new AtomicReference<>();
    private final Object rebuildMonitor = new Object();

    public Agent currentOrInitialize(Supplier<AgentRuntimeSnapshot> factory) {
        AgentRuntimeSnapshot snapshot = current.get();
        if (snapshot != null) {
            return snapshot.agent();
        }
        synchronized (rebuildMonitor) {
            snapshot = current.get();
            if (snapshot == null) {
                snapshot = factory.get();
                current.set(snapshot);
            }
            return snapshot.agent();
        }
    }

    public AgentRuntimeSnapshot rebuild(Supplier<AgentRuntimeSnapshot> factory) {
        synchronized (rebuildMonitor) {
            // 先完整构建；构建异常时不会修改当前快照。
            AgentRuntimeSnapshot replacement = factory.get();
            AgentRuntimeSnapshot previous = current.getAndSet(replacement);
            retire(previous);
            return replacement;
        }
    }

    public AgentRuntimeSnapshot current() {
        return current.get();
    }

    public boolean isInitialized() {
        return current.get() != null;
    }

    private void retire(AgentRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        // Agent 本身不实现 AutoCloseable；附属连接由快照显式声明并在切换后释放。
        snapshot.resources().forEach(resource -> {
            try {
                resource.close();
            } catch (Exception ignored) {
                // 新快照已生效，旧资源清理失败不能回滚运行时。
            }
        });
    }
}

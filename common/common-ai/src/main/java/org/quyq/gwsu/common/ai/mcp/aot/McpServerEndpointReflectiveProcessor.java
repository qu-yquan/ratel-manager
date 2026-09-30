package org.quyq.gwsu.common.ai.mcp.aot;

import io.agentscope.core.tool.Tool;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * 为 MCP Endpoint 及其 AgentScope 工具方法注册原生镜像反射信息。
 */
public final class McpServerEndpointReflectiveProcessor implements ReflectiveProcessor {

    @Override
    public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
        if (!(element instanceof Class<?> endpointType)) {
            return;
        }
        hints.registerType(endpointType,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.ACCESS_DECLARED_FIELDS);
        for (Method method : endpointType.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                hints.registerMethod(method, ExecutableMode.INVOKE);
                registerBindingType(hints, method.getReturnType());
                for (Class<?> parameterType : method.getParameterTypes()) {
                    registerBindingType(hints, parameterType);
                }
            }
        }
    }

    private void registerBindingType(ReflectionHints hints, Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.getName().startsWith("java.")) {
            return;
        }
        hints.registerType(type,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.ACCESS_DECLARED_FIELDS);
    }
}

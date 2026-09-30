package org.quyq.gwsu.common.ai.mcp.annotation;

import org.quyq.gwsu.common.ai.mcp.aot.McpServerEndpointReflectiveProcessor;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个仅提供 Tool 能力的 MCP Server Endpoint。
 *
 * <p>Endpoint 的服务内地址由 {@code PROJECT_CONFIG_PREFIX + ".ai.mcp.base-path"}
 * 与 {@code value} 共同确定；单应用部署时还会在最前方增加所属业务模块前缀。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Indexed
@Lazy(false)
@Reflective(McpServerEndpointReflectiveProcessor.class)
public @interface McpServerEndpoint {

    /**
     * 当前业务模块内唯一的 Endpoint 标识。
     */
    String value();

    /**
     * MCP Server 展示名称，默认使用 applicationName:endpointId。
     */
    String name() default "";

    String version() default "1.0.0";

    /**
     * 允许暴露的 AgentScope 工具名；为空表示暴露 Bean 上的全部 {@code @Tool}。
     */
    String[] tools() default {};

    boolean enableOutputSchema() default true;

    String instructions() default "";

    boolean enabled() default true;
}

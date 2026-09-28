package @@GROUP_ID@@.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * 单体应用 AOT 配置。
 */
@Configuration
@ImportRuntimeHints(YamlRuntimeHints.class)
public class AOTConfiguration {
}

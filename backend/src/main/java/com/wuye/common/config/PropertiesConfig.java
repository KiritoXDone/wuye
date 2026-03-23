package com.wuye.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppInfraProperties.class,
        AppAiProperties.class,
        AppBootstrapProperties.class,
        AppAuthProperties.class,
        AppSecurityProperties.class
})
public class PropertiesConfig {
}

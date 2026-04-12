package com.bica.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration. CORS is now handled by SecurityConfig.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}

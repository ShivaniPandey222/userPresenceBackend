package com.userPresence1.userPresence1;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GlobalCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4900",
                        "http://localhost:4901",
                        "http://localhost:4908",
                        "http://localhost:4800",
                        "http://localhost:4801"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept")
                .exposedHeaders("Content-Type") // optional: to expose SSE headers
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

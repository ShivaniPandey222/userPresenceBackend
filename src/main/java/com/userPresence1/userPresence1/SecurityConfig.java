package com.userPresence1.userPresence1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                .cors(Customizer.withDefaults()) // ✅ Updated CORS integration (non-deprecated)
                .csrf(csrf -> csrf.disable()) // disable CSRF if you're building APIs
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/notes/subscribe/**").permitAll()
                        .requestMatchers("/notes/**").permitAll()// allow notes endpoint (example)
                        .requestMatchers("/presence/**").permitAll() // allow your SSE/presence endpoint
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

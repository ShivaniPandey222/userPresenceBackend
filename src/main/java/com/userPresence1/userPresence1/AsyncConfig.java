package com.userPresence1.userPresence1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
  
  @Bean(name = "sseExecutor")
  public Executor sseExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);   // min threads
    executor.setMaxPoolSize(50);    // max threads
    executor.setQueueCapacity(100); // backlog queue
    executor.setThreadNamePrefix("SSE-Thread-");
    executor.initialize();
    return executor;
  }
}

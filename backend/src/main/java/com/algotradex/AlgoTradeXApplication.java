package com.algotradex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableJpaAuditing
public class AlgoTradeXApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlgoTradeXApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    public org.springframework.core.task.TaskExecutor primaryTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("primary-async-");
        executor.initialize();
        return executor;
    }
}

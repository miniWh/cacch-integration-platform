package com.cacch.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cacch 集成平台启动类
 */
@SpringBootApplication
@EnableScheduling
public class CacchIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacchIntegrationApplication.class, args);
    }
}

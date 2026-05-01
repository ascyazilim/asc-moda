package com.ascmoda.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OrderConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderConfigSourceLogger.class);

    private final Environment environment;

    public OrderConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.order.config-source", "local");
        log.info("Order service configuration source: {}", configSource);
    }
}

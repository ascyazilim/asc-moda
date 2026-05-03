package com.ascmoda.customer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CustomerConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CustomerConfigSourceLogger.class);

    private final Environment environment;

    public CustomerConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.customer.config-source", "local");
        log.info("Customer service configuration source: {}", configSource);
    }
}

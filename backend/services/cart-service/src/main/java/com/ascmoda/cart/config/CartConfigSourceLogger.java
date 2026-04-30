package com.ascmoda.cart.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CartConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CartConfigSourceLogger.class);

    private final Environment environment;

    public CartConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.cart.config-source", "local");
        log.info("Cart service configuration source: {}", configSource);
    }
}

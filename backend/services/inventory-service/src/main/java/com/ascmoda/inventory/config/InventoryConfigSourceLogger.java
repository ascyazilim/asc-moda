package com.ascmoda.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class InventoryConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryConfigSourceLogger.class);

    private final Environment environment;

    public InventoryConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.inventory.config-source", "local");
        log.info("Inventory service configuration source: {}", configSource);
    }
}

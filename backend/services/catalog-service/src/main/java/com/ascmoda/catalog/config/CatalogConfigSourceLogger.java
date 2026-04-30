package com.ascmoda.catalog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CatalogConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogConfigSourceLogger.class);

    private final Environment environment;

    public CatalogConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.catalog.config-source", "local");
        log.info("Catalog service configuration source: {}", configSource);
    }
}

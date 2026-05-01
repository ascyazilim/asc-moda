package com.ascmoda.search.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SearchConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchConfigSourceLogger.class);

    private final Environment environment;

    public SearchConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.search.config-source", "local");
        log.info("Search service configuration source: {}", configSource);
    }
}

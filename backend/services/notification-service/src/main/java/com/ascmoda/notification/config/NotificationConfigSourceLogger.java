package com.ascmoda.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class NotificationConfigSourceLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfigSourceLogger.class);

    private final Environment environment;

    public NotificationConfigSourceLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String configSource = environment.getProperty("ascmoda.notification.config-source", "local");
        log.info("Notification service configuration source: {}", configSource);
    }
}

package com.ascmoda.inventory.config;

import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class InventoryRabbitConfig {

    @Bean
    TopicExchange ascmodaEventsExchange() {
        return new TopicExchange(RabbitEventTopology.EXCHANGE, true, false);
    }
}

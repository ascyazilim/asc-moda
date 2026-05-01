package com.ascmoda.order.config;

import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class OrderRabbitConfig {

    @Bean
    TopicExchange ascmodaEventsExchange() {
        return new TopicExchange(RabbitEventTopology.EXCHANGE, true, false);
    }
}

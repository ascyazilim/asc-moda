package com.ascmoda.notification.config;

import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class NotificationRabbitConfig {

    @Bean
    TopicExchange ascmodaEventsExchange() {
        return new TopicExchange(RabbitEventTopology.EXCHANGE, true, false);
    }

    @Bean
    TopicExchange notificationOrderDeadLetterExchange() {
        return new TopicExchange(RabbitEventTopology.NOTIFICATION_ORDER_DLX, true, false);
    }

    @Bean
    Queue notificationOrderQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitEventTopology.NOTIFICATION_ORDER_DLX)
                .build();
    }

    @Bean
    Queue notificationOrderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_ORDER_DLQ).build();
    }

    @Bean
    Binding orderCreatedBinding(TopicExchange ascmodaEventsExchange, Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CREATED);
    }

    @Bean
    Binding orderConfirmedBinding(TopicExchange ascmodaEventsExchange, Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CONFIRMED);
    }

    @Bean
    Binding orderCancelledBinding(TopicExchange ascmodaEventsExchange, Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CANCELLED);
    }

    @Bean
    Binding notificationOrderDeadLetterBinding(TopicExchange notificationOrderDeadLetterExchange,
                                               Queue notificationOrderDeadLetterQueue) {
        return BindingBuilder.bind(notificationOrderDeadLetterQueue)
                .to(notificationOrderDeadLetterExchange)
                .with("#");
    }
}

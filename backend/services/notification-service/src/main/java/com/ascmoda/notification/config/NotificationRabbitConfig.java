package com.ascmoda.notification.config;

import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
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
    TopicExchange notificationInventoryDeadLetterExchange() {
        return new TopicExchange(RabbitEventTopology.NOTIFICATION_INVENTORY_DLX, true, false);
    }

    @Bean
    Queue notificationOrderQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitEventTopology.NOTIFICATION_ORDER_DLX)
                .build();
    }

    @Bean
    Queue notificationInventoryQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_INVENTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitEventTopology.NOTIFICATION_INVENTORY_DLX)
                .build();
    }

    @Bean
    Queue notificationOrderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_ORDER_DLQ).build();
    }

    @Bean
    Queue notificationInventoryDeadLetterQueue() {
        return QueueBuilder.durable(RabbitEventTopology.NOTIFICATION_INVENTORY_DLQ).build();
    }

    @Bean
    Binding orderCreatedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                @Qualifier("notificationOrderQueue") Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CREATED);
    }

    @Bean
    Binding orderConfirmedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                  @Qualifier("notificationOrderQueue") Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CONFIRMED);
    }

    @Bean
    Binding orderCancelledBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                  @Qualifier("notificationOrderQueue") Queue notificationOrderQueue) {
        return BindingBuilder.bind(notificationOrderQueue).to(ascmodaEventsExchange).with(EventTypes.ORDER_CANCELLED);
    }

    @Bean
    Binding inventoryStockReservedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                          @Qualifier("notificationInventoryQueue") Queue notificationInventoryQueue) {
        return BindingBuilder.bind(notificationInventoryQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.INVENTORY_STOCK_RESERVED);
    }

    @Bean
    Binding inventoryStockReleasedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                          @Qualifier("notificationInventoryQueue") Queue notificationInventoryQueue) {
        return BindingBuilder.bind(notificationInventoryQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.INVENTORY_STOCK_RELEASED);
    }

    @Bean
    Binding inventoryStockConsumedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                          @Qualifier("notificationInventoryQueue") Queue notificationInventoryQueue) {
        return BindingBuilder.bind(notificationInventoryQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.INVENTORY_STOCK_CONSUMED);
    }

    @Bean
    Binding inventoryStockLowBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                     @Qualifier("notificationInventoryQueue") Queue notificationInventoryQueue) {
        return BindingBuilder.bind(notificationInventoryQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.INVENTORY_STOCK_LOW);
    }

    @Bean
    Binding notificationOrderDeadLetterBinding(
            @Qualifier("notificationOrderDeadLetterExchange") TopicExchange notificationOrderDeadLetterExchange,
            @Qualifier("notificationOrderDeadLetterQueue") Queue notificationOrderDeadLetterQueue) {
        return BindingBuilder.bind(notificationOrderDeadLetterQueue)
                .to(notificationOrderDeadLetterExchange)
                .with("#");
    }

    @Bean
    Binding notificationInventoryDeadLetterBinding(
            @Qualifier("notificationInventoryDeadLetterExchange") TopicExchange notificationInventoryDeadLetterExchange,
            @Qualifier("notificationInventoryDeadLetterQueue") Queue notificationInventoryDeadLetterQueue) {
        return BindingBuilder.bind(notificationInventoryDeadLetterQueue)
                .to(notificationInventoryDeadLetterExchange)
                .with("#");
    }
}

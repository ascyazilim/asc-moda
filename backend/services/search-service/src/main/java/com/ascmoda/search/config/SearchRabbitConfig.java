package com.ascmoda.search.config;

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
public class SearchRabbitConfig {

    @Bean
    TopicExchange ascmodaEventsExchange() {
        return new TopicExchange(RabbitEventTopology.EXCHANGE, true, false);
    }

    @Bean
    TopicExchange searchCatalogDeadLetterExchange() {
        return new TopicExchange(RabbitEventTopology.SEARCH_CATALOG_DLX, true, false);
    }

    @Bean
    Queue searchCatalogQueue() {
        return QueueBuilder.durable(RabbitEventTopology.SEARCH_CATALOG_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitEventTopology.SEARCH_CATALOG_DLX)
                .build();
    }

    @Bean
    Queue searchCatalogDeadLetterQueue() {
        return QueueBuilder.durable(RabbitEventTopology.SEARCH_CATALOG_DLQ).build();
    }

    @Bean
    Binding catalogProductCreatedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                         @Qualifier("searchCatalogQueue") Queue searchCatalogQueue) {
        return BindingBuilder.bind(searchCatalogQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.CATALOG_PRODUCT_CREATED);
    }

    @Bean
    Binding catalogProductUpdatedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                         @Qualifier("searchCatalogQueue") Queue searchCatalogQueue) {
        return BindingBuilder.bind(searchCatalogQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.CATALOG_PRODUCT_UPDATED);
    }

    @Bean
    Binding catalogProductDeactivatedBinding(@Qualifier("ascmodaEventsExchange") TopicExchange ascmodaEventsExchange,
                                             @Qualifier("searchCatalogQueue") Queue searchCatalogQueue) {
        return BindingBuilder.bind(searchCatalogQueue)
                .to(ascmodaEventsExchange)
                .with(EventTypes.CATALOG_PRODUCT_DEACTIVATED);
    }

    @Bean
    Binding searchCatalogDeadLetterBinding(
            @Qualifier("searchCatalogDeadLetterExchange") TopicExchange searchCatalogDeadLetterExchange,
            @Qualifier("searchCatalogDeadLetterQueue") Queue searchCatalogDeadLetterQueue) {
        return BindingBuilder.bind(searchCatalogDeadLetterQueue)
                .to(searchCatalogDeadLetterExchange)
                .with("#");
    }
}

package com.ascmoda.search.infrastructure.rabbit;

import com.ascmoda.search.application.service.CatalogSearchEventService;
import com.ascmoda.search.domain.exception.InvalidMessagePayloadException;
import com.ascmoda.search.domain.exception.UnsupportedSearchEventException;
import com.ascmoda.shared.kernel.event.RabbitEventTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventListener.class);

    private final CatalogSearchEventService catalogSearchEventService;

    public CatalogEventListener(CatalogSearchEventService catalogSearchEventService) {
        this.catalogSearchEventService = catalogSearchEventService;
    }

    @RabbitListener(queues = RabbitEventTopology.SEARCH_CATALOG_QUEUE)
    public void onMessage(String message) {
        try {
            catalogSearchEventService.process(message);
        } catch (InvalidMessagePayloadException | UnsupportedSearchEventException ex) {
            log.warn("Skipped invalid catalog search event message reason={}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected catalog search event processing failure", ex);
            throw ex;
        }
    }
}

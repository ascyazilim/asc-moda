package com.ascmoda.search.application.service;

import com.ascmoda.search.application.dto.ParsedSearchEvent;
import com.ascmoda.search.domain.exception.InvalidMessagePayloadException;
import com.ascmoda.search.domain.exception.UnsupportedSearchEventException;
import com.ascmoda.shared.kernel.event.EventTypes;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductCreatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductDeactivatedEvent;
import com.ascmoda.shared.kernel.event.catalog.CatalogProductUpdatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SearchEventParser {

    private final ObjectMapper objectMapper;

    public SearchEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedSearchEvent parse(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(requiredText(root, "eventId"));
            String eventType = requiredText(root, "eventType");
            Instant occurredAt = Instant.parse(requiredText(root, "occurredAt"));
            String sourceService = requiredText(root, "sourceService");
            String correlationId = requiredText(root, "correlationId");
            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null || payloadNode.isNull()) {
                throw new InvalidMessagePayloadException("Event payload must be provided");
            }

            Object payload = switch (eventType) {
                case EventTypes.CATALOG_PRODUCT_CREATED ->
                        objectMapper.treeToValue(payloadNode, CatalogProductCreatedEvent.class);
                case EventTypes.CATALOG_PRODUCT_UPDATED ->
                        objectMapper.treeToValue(payloadNode, CatalogProductUpdatedEvent.class);
                case EventTypes.CATALOG_PRODUCT_DEACTIVATED ->
                        objectMapper.treeToValue(payloadNode, CatalogProductDeactivatedEvent.class);
                default -> throw new UnsupportedSearchEventException("Unsupported event type: " + eventType);
            };

            return new ParsedSearchEvent(
                    eventId,
                    eventType,
                    occurredAt,
                    sourceService,
                    correlationId,
                    payload,
                    objectMapper.writeValueAsString(payloadNode)
            );
        } catch (InvalidMessagePayloadException | UnsupportedSearchEventException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InvalidMessagePayloadException("Search event payload is invalid");
        } catch (Exception ex) {
            throw new InvalidMessagePayloadException("Search event payload is invalid");
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new InvalidMessagePayloadException("Event " + field + " must be provided");
        }
        return node.asText();
    }
}

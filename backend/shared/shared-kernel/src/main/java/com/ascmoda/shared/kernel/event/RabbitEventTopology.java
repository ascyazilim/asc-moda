package com.ascmoda.shared.kernel.event;

public final class RabbitEventTopology {

    public static final String EXCHANGE = "ascmoda.events";
    public static final String NOTIFICATION_ORDER_QUEUE = "notification.order.queue";
    public static final String NOTIFICATION_ORDER_DLQ = "notification.order.dlq";
    public static final String NOTIFICATION_ORDER_DLX = "notification.order.dlx";
    public static final String NOTIFICATION_INVENTORY_QUEUE = "notification.inventory.queue";
    public static final String NOTIFICATION_INVENTORY_DLQ = "notification.inventory.dlq";
    public static final String NOTIFICATION_INVENTORY_DLX = "notification.inventory.dlx";
    public static final String SEARCH_CATALOG_QUEUE = "search.catalog.queue";
    public static final String SEARCH_CATALOG_DLQ = "search.catalog.dlq";
    public static final String SEARCH_CATALOG_DLX = "search.catalog.dlx";

    private RabbitEventTopology() {
    }
}

package com.ascmoda.shared.kernel.event;

public final class EventTypes {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    public static final String INVENTORY_STOCK_RESERVED = "inventory.stock.reserved";
    public static final String INVENTORY_STOCK_RELEASED = "inventory.stock.released";
    public static final String INVENTORY_STOCK_CONSUMED = "inventory.stock.consumed";
    public static final String INVENTORY_STOCK_LOW = "inventory.stock.low";

    public static final String CATALOG_PRODUCT_CREATED = "catalog.product.created";
    public static final String CATALOG_PRODUCT_UPDATED = "catalog.product.updated";
    public static final String CATALOG_PRODUCT_DEACTIVATED = "catalog.product.deactivated";

    private EventTypes() {
    }
}

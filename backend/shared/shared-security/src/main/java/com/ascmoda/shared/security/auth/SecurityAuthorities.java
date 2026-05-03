package com.ascmoda.shared.security.auth;

public final class SecurityAuthorities {

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPPORT = "ROLE_SUPPORT";
    public static final String ROLE_OPERATIONS = "ROLE_OPERATIONS";
    public static final String ROLE_SERVICE = "ROLE_SERVICE";

    public static final String CATALOG_READ = "catalog:read";
    public static final String CATALOG_WRITE = "catalog:write";
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_ADJUST = "inventory:adjust";
    public static final String INVENTORY_RESERVE = "inventory:reserve";
    public static final String INVENTORY_CONSUME = "inventory:consume";
    public static final String ORDER_READ = "order:read";
    public static final String ORDER_CREATE = "order:create";
    public static final String ORDER_CONFIRM = "order:confirm";
    public static final String ORDER_CANCEL = "order:cancel";
    public static final String CUSTOMER_READ = "customer:read";
    public static final String CUSTOMER_UPDATE = "customer:update";
    public static final String CUSTOMER_MANAGE = "customer:manage";
    public static final String NOTIFICATION_READ = "notification:read";
    public static final String NOTIFICATION_RETRY = "notification:retry";
    public static final String SEARCH_REINDEX = "search:reindex";

    private SecurityAuthorities() {
    }
}

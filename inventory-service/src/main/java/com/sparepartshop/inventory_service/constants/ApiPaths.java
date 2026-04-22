package com.sparepartshop.inventory_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String INVENTORY = BASE_API + "/inventory";
    public static final String INVENTORY_BY_ID = "/{id}";
    public static final String BY_PRODUCT_ID = "/product/{productId}";
    public static final String ADD_STOCK = "/product/{productId}/add";
    public static final String REDUCE_STOCK = "/product/{productId}/reduce";
    public static final String LOW_STOCK = "/low-stock";
    public static final String CHECK_STOCK = "/product/{productId}/check";
}

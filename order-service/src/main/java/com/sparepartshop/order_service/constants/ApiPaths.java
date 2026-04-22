package com.sparepartshop.order_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String ORDERS = BASE_API + "/orders";
    public static final String ORDER_BY_ID = "/{id}";
    public static final String ORDER_BY_NUMBER = "/number/{orderNumber}";
    public static final String BY_CUSTOMER = "/customer/{customerId}";
    public static final String BY_STATUS = "/status/{status}";
    public static final String UPDATE_STATUS = "/{id}/status";
    public static final String CANCEL_ORDER = "/{id}/cancel";
}

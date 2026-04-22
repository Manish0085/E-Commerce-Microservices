package com.sparepartshop.customer_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String CUSTOMERS = BASE_API + "/customers";
    public static final String CUSTOMER_BY_ID = "/{id}";
    public static final String BY_PHONE = "/phone/{phone}";
    public static final String BY_TYPE = "/type/{type}";
    public static final String BY_CITY = "/city/{city}";
}
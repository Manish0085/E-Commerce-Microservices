package com.sparepartshop.product_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String PRODUCTS = BASE_API + "/products";
    public static final String PRODUCT_BY_ID = "/{id}";
    public static final String PRODUCTS_BY_CATEGORY = "/category/{category}";
    public static final String PRODUCTS_BY_BRAND = "/brand/{brand}";
    public static final String PRODUCTS_SEARCH = "/search";
}

package com.sparepartshop.order_service.constants;

public final class ClientApiPaths {

    private ClientApiPaths() {}

    public static final class Product {
        private Product() {}
        public static final String GET_BY_ID = "/api/v1/products/{id}";
    }

    public static final class Customer {
        private Customer() {}
        public static final String GET_BY_ID = "/api/v1/customers/{id}";
    }

    public static final class Inventory {
        private Inventory() {}
        public static final String GET_BY_PRODUCT_ID = "/api/v1/inventory/product/{productId}";
        public static final String CHECK_STOCK = "/api/v1/inventory/product/{productId}/check";
        public static final String REDUCE_STOCK = "/api/v1/inventory/product/{productId}/reduce";
        public static final String ADD_STOCK = "/api/v1/inventory/product/{productId}/add";
    }
}

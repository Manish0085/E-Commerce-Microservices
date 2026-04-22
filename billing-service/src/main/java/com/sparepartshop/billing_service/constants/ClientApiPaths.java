package com.sparepartshop.billing_service.constants;

public final class ClientApiPaths {

    private ClientApiPaths() {}

    public static final class Customer {
        private Customer() {}
        public static final String GET_BY_ID = "/api/v1/customers/{id}";
    }
}

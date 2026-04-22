package com.sparepartshop.payment_service.constants;

public final class ClientApiPaths {

    private ClientApiPaths() {}

    public static final class Billing {
        private Billing() {}
        public static final String GET_BY_ID = "/api/v1/invoices/{id}";
        public static final String RECORD_PAYMENT = "/api/v1/invoices/{id}/pay";
    }
}

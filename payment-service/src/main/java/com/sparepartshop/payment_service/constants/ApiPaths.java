package com.sparepartshop.payment_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String PAYMENTS = BASE_API + "/payments";

    public static final String INITIATE = "/initiate";
    public static final String PAYMENT_BY_ID = "/{id}";
    public static final String PAYMENT_BY_REFERENCE = "/reference/{paymentReference}";
    public static final String BY_INVOICE = "/invoice/{invoiceId}";
    public static final String BY_CUSTOMER = "/customer/{customerId}";
    public static final String BY_STATUS = "/status/{status}";
    public static final String WEBHOOK = "/webhook";
    public static final String REFUND = "/{id}/refund";
}

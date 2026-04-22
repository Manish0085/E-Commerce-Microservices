package com.sparepartshop.billing_service.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE_API = "/api/v1";
    public static final String INVOICES = BASE_API + "/invoices";

    public static final String INVOICE_BY_ID = "/{id}";
    public static final String INVOICE_BY_NUMBER = "/number/{invoiceNumber}";
    public static final String BY_CUSTOMER = "/customer/{customerId}";
    public static final String BY_ORDER = "/order/{orderId}";
    public static final String BY_STATUS = "/status/{status}";
    public static final String PAY_INVOICE = "/{id}/pay";
    public static final String VOID_INVOICE = "/{id}/void";
    public static final String OUTSTANDING = "/customer/{customerId}/outstanding";
}

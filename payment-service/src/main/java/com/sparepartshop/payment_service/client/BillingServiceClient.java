package com.sparepartshop.payment_service.client;


import com.sparepartshop.payment_service.constants.ClientApiPaths;
import com.sparepartshop.payment_service.dto.client.ApiResponseWrapper;
import com.sparepartshop.payment_service.dto.client.InvoiceClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@FeignClient(name = "billing-service")
public interface BillingServiceClient {

    @GetMapping(ClientApiPaths.Billing.GET_BY_ID)
    ApiResponseWrapper<InvoiceClientDTO> getInvoiceById(@PathVariable("id") UUID invoiceId);

    @PostMapping(ClientApiPaths.Billing.RECORD_PAYMENT)
    ApiResponseWrapper<InvoiceClientDTO> recordPayment(@PathVariable("id") UUID invoiceId,
                                            @RequestBody Map<String, Object> request);
}

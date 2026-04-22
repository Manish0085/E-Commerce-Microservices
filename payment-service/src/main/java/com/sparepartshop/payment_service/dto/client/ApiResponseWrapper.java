package com.sparepartshop.payment_service.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponseWrapper<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}

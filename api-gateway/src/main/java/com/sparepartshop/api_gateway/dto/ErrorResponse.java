package com.sparepartshop.api_gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String service;

    public ErrorResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getService() { return service; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setStatus(int status) { this.status = status; }
    public void setError(String error) { this.error = error; }
    public void setMessage(String message) { this.message = message; }
    public void setPath(String path) { this.path = path; }
    public void setService(String service) { this.service = service; }

    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder timestamp(LocalDateTime timestamp) {
            response.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            response.status = status;
            return this;
        }

        public Builder error(String error) {
            response.error = error;
            return this;
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public Builder path(String path) {
            response.path = path;
            return this;
        }

        public Builder service(String service) {
            response.service = service;
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }
}

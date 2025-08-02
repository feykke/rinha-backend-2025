package org.acme.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Payment {

    @JsonProperty("correlation_id")
    public String correlationId;
    public Double amount;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

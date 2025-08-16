package org.acme.entity;

import java.math.BigDecimal;

public final class Payment {

    private final String correlationId;
    private final BigDecimal amount;
    private final String requestedAt;

    public Payment(String correlationId, BigDecimal amount, String requestedAt) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.requestedAt = requestedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

}

package org.acme.entity;

import java.math.BigDecimal;

public final class Payment {

    private final String correlationId;
    private final BigDecimal amount;
    private final String createdAt;

    public Payment(String correlationId, BigDecimal amount, String createdAt) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

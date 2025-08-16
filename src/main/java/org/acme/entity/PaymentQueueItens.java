package org.acme.entity;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentQueueItens {

    private final UUID correlationId;
    private final BigDecimal amount;

    public PaymentQueueItens(UUID correlationId, BigDecimal amount) {
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

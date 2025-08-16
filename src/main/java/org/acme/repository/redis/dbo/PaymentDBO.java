package org.acme.repository.redis.dbo;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.UUID;

@RegisterForReflection
public class PaymentDBO {
    private final UUID correlationId;
    private final BigDecimal amount;
    private final String processor;

    public PaymentDBO(UUID correlationId, BigDecimal amount, String processor) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.processor = processor;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getProcessor() {
        return processor;
    }

    @Override
    public String toString() {
        return "PaymentDBO{" +
                "correlationId=" + correlationId +
                ", amount=" + amount +
                ", processor='" + processor + '\'' +
                '}';
    }
}

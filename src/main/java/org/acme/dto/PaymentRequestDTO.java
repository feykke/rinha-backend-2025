package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record PaymentRequestDTO(String correlationId, BigDecimal amount) {
}

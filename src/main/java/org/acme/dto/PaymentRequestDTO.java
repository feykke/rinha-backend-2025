package org.acme.dto;

import java.math.BigDecimal;

public record PaymentRequestDTO(String correlationId, BigDecimal amount) {
}

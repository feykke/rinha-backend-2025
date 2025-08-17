package org.acme.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.UUID;

@RegisterForReflection
public record PaymentQueueItens (UUID correlationId, BigDecimal amount) { }

package org.acme.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record Payment (String correlationId, BigDecimal amount, String requestedAt) { }

package org.acme.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record ProcessorSummary (int totalRequests, BigDecimal totalAmount) { }

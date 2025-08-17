package org.acme.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.json.bind.annotation.JsonbProperty;

@RegisterForReflection
public record PaymentsSummaryResponse(@JsonbProperty("default") ProcessorSummary defaultP, ProcessorSummary fallback) { }

package org.acme.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentsSummaryResponse(@JsonProperty("default") ProcessorSummary defaultP, ProcessorSummary fallback) { }

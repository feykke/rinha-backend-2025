package org.acme.entity;

import java.math.BigDecimal;

public record ProcessorSummary (int totalRequests, BigDecimal totalAmount) { }

package org.acme.dto;

public record HealthCheckResponseDTO(boolean failing, int minResponseTime) { }

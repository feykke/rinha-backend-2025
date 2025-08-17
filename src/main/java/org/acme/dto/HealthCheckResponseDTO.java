package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record HealthCheckResponseDTO(boolean failing, int minResponseTime) { }

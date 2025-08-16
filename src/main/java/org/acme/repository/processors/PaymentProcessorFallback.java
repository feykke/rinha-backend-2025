package org.acme.repository.processors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.acme.dto.HealthCheckResponseDTO;
import org.acme.entity.Payment;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/payments")
@RegisterRestClient(configKey = "payment-processor-fallback")
public interface PaymentProcessorFallback {
    @GET
    @Path("/service-health")
    RestResponse<HealthCheckResponseDTO> healthCheck();

    @POST
    RestResponse<Void> processPayment(Payment payment);
}

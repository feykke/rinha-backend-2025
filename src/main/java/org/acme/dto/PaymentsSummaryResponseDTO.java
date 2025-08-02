package org.acme.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentsSummaryResponseDTO  {
    public record PaymentDefaultResponse (Integer totalRequests, Double totalAmount) { }
    public record PaymentFallbackResponse (Integer totalRequests, Double totalAmount) { }

    @JsonProperty("default")
    public PaymentDefaultResponse defaultResponse;
    @JsonProperty("fallback")
    public PaymentFallbackResponse fallbackResponse;

    public PaymentsSummaryResponseDTO(Integer totalDefault, Integer totalFallback, Double amountDefault, Double amountFallback) {
        this.defaultResponse = new PaymentDefaultResponse(totalDefault, amountDefault);
        this.fallbackResponse = new PaymentFallbackResponse(totalFallback, amountFallback);
    }
}

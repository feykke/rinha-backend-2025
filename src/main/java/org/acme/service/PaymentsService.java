package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.dto.HealthCheckResponseDTO;
import org.acme.dto.PaymentRequestDTO;
import org.acme.entity.Payment;
import org.acme.entity.PaymentQueueItens;
import org.acme.entity.PaymentsSummaryResponse;
import org.acme.entity.ProcessorSummary;
import org.acme.repository.processors.PaymentProcessorDefault;
import org.acme.repository.processors.PaymentProcessorFallback;
import org.acme.repository.redis.RedisRepository;
import org.acme.repository.redis.dbo.PaymentDBO;
import org.acme.utils.DateUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class PaymentsService {

    private final String DEFAULT = "default";
    private final String FALLBACK = "fallback";

    private final AtomicReference<String> activeProcessor = new AtomicReference<>(DEFAULT);
    private final RedisRepository redisRepository;
    private final PaymentProcessorDefault paymentProcessorDefault;
    private final PaymentProcessorFallback paymentProcessorFallback;

    @Inject
    public PaymentsService(
            RedisRepository redisRepository,
            @RestClient PaymentProcessorDefault paymentProcessorDefault,
            @RestClient PaymentProcessorFallback paymentProcessorFallback
    ) {
        this.redisRepository = redisRepository;
        this.paymentProcessorDefault = paymentProcessorDefault;
        this.paymentProcessorFallback = paymentProcessorFallback;
    }


    public PaymentsSummaryResponse getPaymentsSummary(String from, String to) {
        Long fromMilli = DateUtils.parseIsoUtcToEpochMilli(from);
        Long toMilli = DateUtils.parseIsoUtcToEpochMilli(to);

        List<PaymentDBO> payments = redisRepository.getPayments(fromMilli, toMilli);

        ProcessorSummary summarizedDefault = summarizeProcessors(payments, DEFAULT);
        ProcessorSummary summarizedFallback = summarizeProcessors(payments, FALLBACK);

        return new PaymentsSummaryResponse(summarizedDefault, summarizedFallback);
    }

    public void enqueuePayment(final PaymentRequestDTO payment) {
        redisRepository.enqueue(payment);
    }

    public PaymentQueueItens dequeuePayment() {
        return redisRepository.dequeue();
    }

    public void processPayment(Payment payment) {
        if(activeProcessor.get().equals(DEFAULT)) {
            RestResponse<Void> response =  paymentProcessorDefault.processPayment(payment);
            handlePaymentResponse(response, payment, DEFAULT);
            return;
        }

        if(activeProcessor.get().equals(FALLBACK)) {
            RestResponse<Void> response = paymentProcessorFallback.processPayment(payment);
            handlePaymentResponse(response, payment, FALLBACK);
            return;
        }

        redisRepository.enqueue(new PaymentRequestDTO(payment.correlationId(), payment.amount()));
    }

    public void checkProcessorsHealth() {
        RestResponse<HealthCheckResponseDTO> healthDefault = paymentProcessorDefault.healthCheck();
        if (healthDefault.getEntity() != null && !healthDefault.getEntity().failing()) {
            activeProcessor.set("default");
            return;
        }

        RestResponse<HealthCheckResponseDTO> healthFallback = paymentProcessorFallback.healthCheck();
        if (healthFallback.getEntity() != null && !healthFallback.getEntity().failing()) {
            activeProcessor.set("fallback");
            return;
        }

        activeProcessor.set("none");
    }

    private void handlePaymentResponse(RestResponse<Void> response, Payment payment, String processor) {
        if (response.getStatus() == 200) {
            redisRepository.savePayment(payment, processor);
        } else {
            redisRepository.enqueue(new PaymentRequestDTO(payment.correlationId(), payment.amount()));
        }
    }

    private ProcessorSummary summarizeProcessors (List<PaymentDBO> payments, String processor) {
        List<PaymentDBO> paymentsByProcessor = payments.stream()
                .filter(payment -> processor.equals(payment.getProcessor())).toList();

        BigDecimal totalAmount = paymentsByProcessor.stream()
                .map(PaymentDBO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProcessorSummary(paymentsByProcessor.size(), totalAmount);
    }
}

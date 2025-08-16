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
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class PaymentsService {

    private final Logger LOG = Logger.getLogger(PaymentsService.class);

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
            LOG.infof("Status POST processor default: %d", response.getStatus());
            handlePaymentResponse(response, payment, DEFAULT);
            return;
        }

        if(activeProcessor.get().equals(FALLBACK)) {
            RestResponse<Void> response = paymentProcessorFallback.processPayment(payment);
            LOG.infof("Status POST processor fallback: %d", response.getStatus());
            handlePaymentResponse(response, payment, FALLBACK);
            return;
        }

        redisRepository.enqueue(new PaymentRequestDTO(payment.getCorrelationId(), payment.getAmount()));
        LOG.debugf("All processors are down, enqueuing payment %s", payment.getCorrelationId());
    }

    public void checkProcessorsHealth() {
        try {
            RestResponse<HealthCheckResponseDTO> healthDefault = paymentProcessorDefault.healthCheck();
            LOG.infof("Default Processor failing: %s", healthDefault.getEntity().failing());
            if (healthDefault.getEntity() != null && !healthDefault.getEntity().failing()) {
                activeProcessor.set("default");
                LOG.info("Default processor is healthy, setting as active");
                return;
            }
        } catch (Exception e) {
            LOG.warnf("Error checking default processor health: %s", e.getMessage());
        }

        try {
            RestResponse<HealthCheckResponseDTO> healthFallback = paymentProcessorFallback.healthCheck();
            LOG.infof("Fallback Processor failing: %s", healthFallback.getEntity().failing());
            if (healthFallback.getEntity() != null && !healthFallback.getEntity().failing()) {
                activeProcessor.set("fallback");
                LOG.info("Fallback processor is healthy, setting as active");
                return;
            }
        } catch (Exception e) {
            LOG.warnf("Error checking fallback processor health: %s", e.getMessage());
        }

        activeProcessor.set("none");
        LOG.info("No processors are healthy, setting active processor to none");
    }

    private void handlePaymentResponse(RestResponse<Void> response, Payment payment, String processor) {
        if (response.getStatus() == 200) {
            redisRepository.savePayment(payment, processor);
        } else {
            redisRepository.enqueue(new PaymentRequestDTO(payment.getCorrelationId(), payment.getAmount()));
            LOG.errorf("Error creating payment in %s processor", processor);
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

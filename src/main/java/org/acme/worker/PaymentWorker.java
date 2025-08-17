package org.acme.worker;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Payment;
import org.acme.entity.PaymentQueueItens;
import org.acme.repository.redis.RedisRepository;
import org.acme.service.PaymentsService;
import org.acme.utils.DateUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Startup
@ApplicationScoped
public class PaymentWorker {

    private ExecutorService executor;
    private volatile boolean running = true;

    private final RedisRepository redisRepo;
    private final PaymentsService paymentsService;
    private final String serviceType;
    private final int qttWorkers;

    public PaymentWorker(
            final RedisRepository redisRepo,
            final PaymentsService paymentsService,
            @ConfigProperty(name = "service.type") final String serviceType,
            @ConfigProperty(name = "payment-workers.qtt") int qttWorkers
            ) {
        this.redisRepo = redisRepo;
        this.paymentsService = paymentsService;
        this.serviceType = serviceType;
        this.qttWorkers = qttWorkers;
    }

    @PostConstruct
    void initWorkers() {
        if (!"worker".equals(serviceType)) {
            return;
        }

        executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < qttWorkers; i++) {
            final String workerId = UUID.randomUUID().toString();
            executor.submit(() -> poolLoop(workerId));
        }
    }

    @PreDestroy
    void shutdownWorkers () {
        running = false;
        if (executor != null) executor.shutdown();
        redisRepo.closeClient();
    }

    private void poolLoop(String workerId) {
        while (running) {
            try {
                if (!redisRepo.isConnected()) {
                    Thread.sleep(1000);
                    continue;
                }

               PaymentQueueItens response = paymentsService.dequeuePayment();

               if (response == null) {
                   Thread.sleep(100);
                   continue;
               }

               Payment payment = new Payment(
                       response.getCorrelationId().toString(),
                       response.getAmount(),
                       DateUtils.FORMATTER.format(Instant.now())
               );

               paymentsService.processPayment(payment);
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}

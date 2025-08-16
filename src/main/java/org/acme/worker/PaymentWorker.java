package org.acme.worker;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Payment;
import org.acme.entity.PaymentQueueItens;
import org.acme.repository.redis.RedisRepository;
import org.acme.service.PaymentsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Startup
@ApplicationScoped
public class PaymentWorker {

    private static final Logger LOG = Logger.getLogger(PaymentWorker.class);

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

        executor = Executors.newFixedThreadPool(qttWorkers);

        LOG.infof("Starting %d workers...", qttWorkers);

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
        LOG.info("Worker pool shut");
    }

    private void poolLoop(String workerId) {
        while (running) {
            try {
                if(!redisRepo.acquireLock(workerId)) {
                    Thread.sleep(100);
                    continue;
                }

                // Dealing with concurrency
                long lockExpiresAt = Instant.now().toEpochMilli() + redisRepo.LOCK_TTL_MS;
                while (Instant.now().toEpochMilli() < lockExpiresAt && running) {
                   PaymentQueueItens response = paymentsService.dequeuePayment();

                   if (response == null) {
                       break;
                   }

                   Payment payment = new Payment(
                           response.getCorrelationId().toString(),
                           response.getAmount(),
                           Instant.now().toString()
                   );

                   paymentsService.processPayment(payment);
                }

                redisRepo.releaseLock(workerId);
            } catch (Exception e) {
                LOG.errorf("Error worker %s: %s", workerId, e.getMessage(), e);
            }
        }
    }
}

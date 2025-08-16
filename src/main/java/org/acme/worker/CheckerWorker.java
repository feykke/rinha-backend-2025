package org.acme.worker;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.service.PaymentsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@Startup
public class CheckerWorker {

    private ExecutorService executor;
    private final String serviceType;
    private final PaymentsService paymentsService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CheckerWorker(
            final PaymentsService paymentsService,
            @ConfigProperty(name = "service.type") final String serviceType
    ) {
        this.serviceType = serviceType;
        this.paymentsService = paymentsService;
    }


    @PostConstruct
    public void initChecker() {
        if(!"worker".equals(serviceType)) {
            return;
        }
        executor = Executors.newFixedThreadPool(1);
        running.set(true);

        executor.submit(this::healthCheckLoop);
    }

    @PreDestroy
    public void shutdownChecker() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void healthCheckLoop() {
        while (running.get()) {
            try {
                paymentsService.checkProcessorsHealth();
                Thread.sleep(5000); // Wait 5 seconds before next check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(5000); // Wait 5 seconds even on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}

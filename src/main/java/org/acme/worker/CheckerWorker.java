package org.acme.worker;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.service.PaymentsService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@Startup
public class CheckerWorker {

    private static final Logger LOG = Logger.getLogger(CheckerWorker.class);

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
        LOG.infof("CheckerWorker init - serviceType: %s", serviceType);
        if(!"worker".equals(serviceType)) {
            LOG.info("CheckerWorker not starting - not a worker service");
            return;
        }
        executor = Executors.newFixedThreadPool(1);
        running.set(true);

        LOG.info("Starting checker with 5-second interval...");
        executor.submit(this::healthCheckLoop);
    }

    @PreDestroy
    public void shutdownChecker() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
        }
        LOG.info("Checker shut");
    }

    private void healthCheckLoop() {
        LOG.info("Health check loop started");
        while (running.get()) {
            try {
                LOG.debug("Executing health check...");
                paymentsService.checkProcessorsHealth();
                LOG.debug("Health check completed, waiting 5 seconds...");
                Thread.sleep(5000); // Wait 5 seconds before next check
            } catch (InterruptedException e) {
                LOG.warn("Health check loop interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOG.errorf("Error in health check loop: %s", e.getMessage());
                try {
                    Thread.sleep(5000); // Wait 5 seconds even on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        LOG.info("Health check loop stopped");
    }
}

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

@ApplicationScoped
@Startup
public class CheckerWorker {

    private static final Logger LOG = Logger.getLogger(CheckerWorker.class);

    private ExecutorService executor;
    private final String serviceType;
    private final PaymentsService paymentsService;

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

        LOG.info("Starting checker...");
        executor.submit(paymentsService::checkProcessorsHealth);
    }

    @PreDestroy
    public void shutdownChecker() {
        if (executor != null) executor.shutdown();
        LOG.info("Checker shut");
    }
}

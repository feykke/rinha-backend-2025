package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.dto.PaymentRequestDTO;
import org.acme.repository.redis.RedisRepository;

@ApplicationScoped
public class PaymentsService {

    private final RedisRepository redisRepository;

    @Inject
    public PaymentsService(
            RedisRepository redisRepository
    ) {
        this.redisRepository = redisRepository;
    }

    public void enqueuePayment(final PaymentRequestDTO payment) {
        redisRepository.enqueue(payment);
    }
}

package org.acme.repository.redis;


import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.dto.PaymentRequestDTO;
import org.acme.entity.PaymentQueueItens;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;


@ApplicationScoped
public class RedisRepository {

    final RedisDataSource redisDs;
    private final String PAYMENT_QUEUE = "processing_payment_queue";
    private final ListCommands<String, String> paymentList;

    public RedisRepository(RedisDataSource redisDs) {
        this.redisDs = redisDs;
        this.paymentList = redisDs.list(String.class, String.class);
    }

    public void enqueue (PaymentRequestDTO payment) {
        String paymentData = payment.correlationId() + ":" + payment.amount();
        paymentList.lpush(PAYMENT_QUEUE, paymentData);
    }

    public PaymentQueueItens dequeue() {
        KeyValue<String, String> result = paymentList.blpop(Duration.ofSeconds(1), PAYMENT_QUEUE);
        if (result != null) {
            String[] parts = result.value().split(":");
            UUID correlationId = UUID.fromString(parts[0]);
            BigDecimal amount = new BigDecimal(parts[1]);
            return new PaymentQueueItens(correlationId, amount);
        }
        return null;
    }
}

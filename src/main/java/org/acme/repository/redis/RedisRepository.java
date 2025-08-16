package org.acme.repository.redis;


import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.dto.PaymentRequestDTO;
import org.acme.entity.Payment;
import org.acme.entity.PaymentQueueItens;
import org.acme.repository.redis.dbo.PaymentDBO;
import org.acme.utils.DateUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;


@ApplicationScoped
public class RedisRepository {

    private final String PAYMENT_QUEUE = "processing_payment_queue";
    private final String PAYMENT_SET = "payment_by_date";

    private final Redis redis;
    private final ListCommands<String, String> paymentList;
    private final SortedSetCommands<String, PaymentDBO> paymentSortedSet;

    // Lock Configuration
    private static final String LOCK_KEY = "payment_worker_lock";
    public final long LOCK_TTL_MS = 2000;

    @Inject
    public RedisRepository(
            RedisDataSource redisDs,
            Redis redis
    ) {
        this.redis = redis;
        this.paymentList = redisDs.list(String.class, String.class);
        this.paymentSortedSet = redisDs.sortedSet(PaymentDBO.class);
    }

    public void enqueue (PaymentRequestDTO payment) {
        String paymentData = payment.correlationId() + ":" + payment.amount();
        paymentList.lpush(PAYMENT_QUEUE, paymentData);
    }

    public PaymentQueueItens dequeue() {
        try {
            KeyValue<String, String> result = paymentList.blpop(Duration.ofSeconds(1), PAYMENT_QUEUE);
            if (result != null) {
                String[] parts = result.value().split(":");
                UUID correlationId = UUID.fromString(parts[0]);
                BigDecimal amount = new BigDecimal(parts[1]);
                return new PaymentQueueItens(correlationId, amount);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean acquireLock(String workerId) {
        try {
            Response response = redis.send(
                    io.vertx.redis.client.Request.cmd(Command.SET)
                            .arg(LOCK_KEY)
                            .arg(workerId)
                            .arg("NX")
                            .arg("PX").arg(LOCK_TTL_MS)
            ).toCompletionStage().toCompletableFuture().get();

            if (response != null && "OK".equals(response.toString())) {
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    public void releaseLock(String workerId) {
        try {
            Request requestLockKey = Request.cmd(Command.GET).arg(LOCK_KEY);
            Response currentValue = redis.send(requestLockKey).toCompletionStage().toCompletableFuture().get();
            if(currentValue != null && workerId.equals(currentValue.toString())) {
                Request requestDelete = Request.cmd(Command.DEL).arg(LOCK_KEY);
                redis.send(requestDelete).toCompletionStage().toCompletableFuture().get();
            }
        } catch (Exception e) {
        }
    }

    public void closeClient() {
        if (redis != null) redis.close();
    }

    public boolean isConnected() {
        try {
            Response response = redis.send(Request.cmd(Command.PING)).toCompletionStage().toCompletableFuture().get();
            return response != null && "PONG".equals(response.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public List<PaymentDBO> getPayments (Long from, Long to) {
        return paymentSortedSet.zrangebyscore(PAYMENT_SET, new ScoreRange(from, to));
    }

    public void savePayment(Payment payment, String processorName) {
        try {
            final PaymentDBO paymentDBO = new PaymentDBO(UUID.fromString(payment.getCorrelationId()), payment.getAmount(), processorName);
            
            Long score = DateUtils.parseIsoUtcToEpochMilli(payment.getRequestedAt());
            
            paymentSortedSet.zadd(PAYMENT_SET, score, paymentDBO);
        } catch (Exception e) {
        }
    }
}

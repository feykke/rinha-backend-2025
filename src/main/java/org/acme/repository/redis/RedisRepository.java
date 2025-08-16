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
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;


@ApplicationScoped
public class RedisRepository {

    private final Logger LOG = Logger.getLogger(RedisRepository.class);

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
        LOG.infof("Payment enqueued: %s", paymentData);
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

    public boolean acquireLock(String workerId) {
        try {
            String result = redis.send(
                    io.vertx.redis.client.Request.cmd(Command.SET)
                            .arg(LOCK_KEY)
                            .arg(workerId)
                            .arg("NX")
                            .arg("PX").arg(LOCK_TTL_MS)
            ).toCompletionStage().toCompletableFuture().get().toString();

            if ("OK".equals(result)) {
                LOG.debug("Acquired lock!");
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error when acquiring lock", e);
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
                LOG.debug("Unlocked lock");
            }
        } catch (Exception e) {
            LOG.error("Error when unlocking lock", e);
        }
    }

    public void closeClient() {
        if (redis != null) redis.close();
    }

    public List<PaymentDBO> getPayments (Long from, Long to) {
        return paymentSortedSet.zrangebyscore(PAYMENT_SET, new ScoreRange(from, to));
    }

    public void savePayment(Payment payment, String processorName) {
        final PaymentDBO paymentDBO = new PaymentDBO(UUID.fromString(payment.getCorrelationId()), payment.getAmount(), processorName);
        paymentSortedSet.zadd(PAYMENT_SET, DateUtils.parseIsoUtcToEpochMilli(payment.getRequestedAt()), paymentDBO);
    }
}

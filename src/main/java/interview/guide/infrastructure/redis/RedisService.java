package interview.guide.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;

    public <T> void set(String key, T value){
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 设置值（带过期时间）
     */
    public <T> void set(String key, T value, Duration ttl) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttl);
    }

    /**
     * 获取值
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }


    public <T> T getOrLoad(String key, Duration ttl, Function<String, T> loader) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        if (value == null) {
            value = loader.apply(key);
            if (value != null) {
                bucket.set(value, ttl);
            }
        }
        return value;
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     *  check key existence
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * set ttl
     */
    public boolean expire(String key, Duration ttl) {
        return redissonClient.getBucket(key).expire(ttl);
    }

    /**
     *  get time to live (ms)
     */
    public long getTimeToLive(String key) {
        return redissonClient.getBucket(key).remainTimeToLive();
    }

    // ==================== Hash 操作 ====================

    public <K, V> void hSet(String key, K field, V value) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    public <K, V> V hGet(String key, K field) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.get(field);
    }

    public <K, V> Map<K, V> hGetAll(String key) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    public <K, V> boolean hDelete(String key, K field) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.remove(field) != null;
    }

    public <K> boolean hExists(String key, K field) {
        RMap<K, Object> map = redissonClient.getMap(key);
        return map.containsKey(field);
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取锁（阻塞等待）
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取锁（非阻塞）
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 执行带锁的操作
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                 TimeUnit unit, LockedOperation<T> operation) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
                try {
                    return operation.execute();
                } finally {
                    lock.unlock();
                }
            }
            throw new RuntimeException("获取锁失败: " + lockKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断: " + lockKey, e);
        }
    }


    @FunctionalInterface
    public interface LockedOperation<T> {
        T execute();
    }

// ==================== Stream 消息队列 ====================

    /**
     * Stream 消息处理器接口
     */
    @FunctionalInterface
    public interface StreamMessageProcessor {
        void process(StreamMessageId messageId, Map<String, String> data);
    }

    public void streamAck(String streamKey, String groupName, StreamMessageId messageId) {
            RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
            stream.ack(groupName,messageId);
    }

    /**
     * 消费 Stream 消息（阻塞模式）
     * 使用 Redis BLOCK 参数，让服务端等待消息，比客户端轮询更高效
     *
     * @param streamKey      Stream 键
     * @param groupName      消费者组名
     * @param consumerName   消费者名
     * @param count          每次读取数量
     * @param blockTimeoutMs 阻塞等待超时时间（毫秒），0 表示无限等待
     * @param processor      消息处理器
     * @return true 如果处理了消息，false 如果超时无消息
     */
    public boolean streamConsumeMessages(
            String streamKey,
            String groupName,
            String consumerName,
            int count,
            long blockTimeoutMs,
            StreamMessageProcessor processor) {

        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);

        // 使用阻塞读取，让 Redis 服务端等待消息
        Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                groupName,
                consumerName,
                StreamReadGroupArgs.neverDelivered()
                        .count(count)
                        .timeout(Duration.ofMillis(blockTimeoutMs))
        );

        if (messages == null || messages.isEmpty()) {
            return false;
        }

        for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
            processor.process(entry.getKey(), entry.getValue());
        }

        return true;
    }

    public void createStreamGroup(String streamKey, String groupName){
        RStream<String, String> stream = redissonClient.getStream(streamKey);
        try {
            stream.createGroup(StreamCreateGroupArgs.name(groupName).makeStream());
            log.info("create stream consumer group: stream = {}, group = {}", streamKey, groupName);
        } catch (Exception e) {
            // group exists
            if(!e.getMessage().contains("BUSYGROUP")){
                log.warn("create failed: {}", e.getMessage());
            }
        }

    }

    /**
     * 发送消息到 Stream
     */
    public String streamAdd(String streamKey, Map<String, String> message) {
        return streamAdd(streamKey, message, 0);
    }

    /**
     * 发送消息到 Stream（带长度限制）
     *
     * @param streamKey Stream 键
     * @param message   消息内容
     * @param maxLen    最大长度，超过时自动裁剪旧消息，0 表示不限制
     * @return 消息ID
     */
    public String streamAdd(String streamKey, Map<String, String> message, int maxLen) {
        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
        StreamAddArgs<String, String> args = StreamAddArgs.entries(message);
        if (maxLen > 0) {
            args.trimNonStrict().maxLen(maxLen);
        }
        StreamMessageId messageId = stream.add(args);
        log.debug("发送 Stream 消息: stream={}, messageId={}, maxLen={}", streamKey, messageId, maxLen);
        return messageId.toString();
    }


}

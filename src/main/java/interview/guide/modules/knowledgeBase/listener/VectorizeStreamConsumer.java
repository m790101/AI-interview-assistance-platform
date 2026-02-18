package interview.guide.modules.knowledgeBase.listener;

import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.knowledgeBase.model.VectorStatus;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import interview.guide.modules.knowledgeBase.service.KnowledgeBaseVectorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
public class VectorizeStreamConsumer {
    private final RedisService redisService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseVectorService vectorService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    private String consumerName;


    @PostConstruct
    public void init() {
        this.consumerName = AsyncTaskStreamConstants.KB_VECTORIZE_CONSUMER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        try {
            // init consumer for vectorize
            redisService.createStreamGroup(
                    AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY,
                    AsyncTaskStreamConstants.KB_VECTORIZE_GROUP_NAME
            );
            log.info("consumer group exist or create {}", AsyncTaskStreamConstants.KB_VECTORIZE_GROUP_NAME);

        } catch (Exception e) {
            log.warn("consumer might be exist: {}", e.getMessage());
        }
        // add tread to run this consumer
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread( r, "vectorize-consumer");
            t.setDaemon(true);
            return t;
        });
        running.set(true);
        executorService.submit(this::consumeLoop);

        log.info("vector consumer init: consumerName={}", consumerName);
    }


    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("向量化消费者已关闭: consumerName={}", consumerName);
    }

    private void consumeLoop() {
        // listen and get from stream
        while (running.get()) {
            try {
                redisService.streamConsumeMessages(
                        AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY,
                        AsyncTaskStreamConstants.KB_VECTORIZE_GROUP_NAME,
                        this.consumerName,
                        AsyncTaskStreamConstants.BATCH_SIZE,
                        AsyncTaskStreamConstants.POLL_INTERVAL_MS,
                        this::processMessage
                );

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("consumer vectorize is interrupt");
                    break;
                }
                log.error("vectorize consumer error: {}", e.getMessage(), e);
            }
        }


    }

    private void processMessage(StreamMessageId messageId, Map<String, String> data) {
        String kbIdStr = data.get(AsyncTaskStreamConstants.FIELD_KB_ID);
        String content = data.get(AsyncTaskStreamConstants.FIELD_CONTENT);
        String retryCountStr = data.getOrDefault(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0");


        if (kbIdStr == null || content == null) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            ackMessage(messageId);
            return;
        }


        Long kbId = Long.parseLong(kbIdStr);
        int retryCount = Integer.parseInt(retryCountStr);

        log.info("processing message for vector init: vectorId = {}, messageId = {}, retryCount = {}", kbId, messageId, retryCount);
        try {
            // 1. 更新状态为 PROCESSING
            updateVectorStatus(kbId, VectorStatus.PROCESSING, null);

            // 2. 执行向量化
            vectorService.vectorizeAndStore(kbId, content);

            // 3. 更新状态为 COMPLETED
            updateVectorStatus(kbId, VectorStatus.COMPLETED, null);

            // 4. 确认消息
            ackMessage(messageId);

            log.info("向量化任务完成: kbId={}", kbId);

        } catch (Exception e) {
            // if failed, retry
            if (retryCount < AsyncTaskStreamConstants.MAX_RETRY_COUNT) {
                retryMessage(kbId, content, retryCount + 1);
            } else {
                String errorMsg = truncateError("分析失败(已重试" + retryCount + "次): " + e.getMessage());
                updateVectorStatus(kbId, VectorStatus.FAILED, errorMsg);
            }

            ackMessage(messageId);
        }


    }

    /**
     * 重试消息（重新发送到 Stream）
     */
    private void retryMessage(Long kbId, String content, int retryCount) {
        try {
            Map<String, String> message = Map.of(
                    AsyncTaskStreamConstants.FIELD_KB_ID, kbId.toString(),
                    AsyncTaskStreamConstants.FIELD_CONTENT, content,
                    AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
            );

            redisService.streamAdd(
                    AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY,
                    message,
                    AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("向量化任务已重新入队: kbId={}, retryCount={}", kbId, retryCount);

        } catch (Exception e) {
            log.error("重试入队失败: kbId={}, error={}", kbId, e.getMessage(), e);
            updateVectorStatus(kbId, VectorStatus.FAILED, truncateError("重试入队失败: " + e.getMessage()));
        }
    }

    private void ackMessage(StreamMessageId messageId) {
        try {
            redisService.streamAck(
                    AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY,
                    AsyncTaskStreamConstants.KB_VECTORIZE_GROUP_NAME,
                    messageId
            );
        } catch (Exception e) {
            log.error("确认消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
        }
    }


    private void updateVectorStatus(Long kbId, VectorStatus status, String error) {
        try {
            knowledgeBaseRepository.findById(kbId)
                    .ifPresent((kb) -> {
                        kb.setVectorStatus(status);
                        kb.setVectorError(error);
                        knowledgeBaseRepository.save(kb);
                    });
        } catch (Exception e) {
            log.error("fail with update vector status", e);
        }

    }

    /**
     * 截断错误信息，避免超过数据库字段长度
     */
    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 500 ? error.substring(0, 500) : error;
    }

}

package interview.guide.modules.knowledgeBase.listener;

import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.exception.BusinessException;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.knowledgeBase.model.KnowledgeBaseEntity;
import interview.guide.modules.knowledgeBase.model.VectorStatus;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VectorizeStreamProducer {
    private final RedisService redisService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;


    public void sendVectorizeTask(Long kbId, String content) {
        try {
            Map<String, String> message = Map.of(
                    AsyncTaskStreamConstants.FIELD_KB_ID, kbId.toString(),
                    AsyncTaskStreamConstants.FIELD_CONTENT, content,
                    AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0"
            );
            String messageId = redisService.streamAdd(
                    AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY,
                    message,
                    AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("vector task send to stream kbId = {}, messageId = {}", kbId, messageId);
        } catch (Exception e) {
            log.error("发送向量化任务失败: kbId={}, error={}", kbId, e.getMessage(), e);
            updateVectorStatus(kbId, VectorStatus.FAILED, "任务入队失败: " + e.getMessage());
        }
    }

    private void updateVectorStatus(Long kbId, VectorStatus status, String error) {
        knowledgeBaseRepository.findById(kbId)
                .ifPresent(kb -> {
                            kb.setVectorStatus(status);
                            if (error != null) {
                                kb.setVectorError(error.length() > 500 ? error.substring(0, 500) : error);
                            }
                            knowledgeBaseRepository.save(kb);
                        }
                );
    }
}

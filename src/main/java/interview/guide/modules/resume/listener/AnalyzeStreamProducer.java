package interview.guide.modules.resume.listener;

import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class AnalyzeStreamProducer {
    private final RedisService redisService;
    private final ResumeRepository resumeRepository;


    public void sendAnalyzeTask(Long resumeId, String content){
        try {
            Map<String, String> message = Map.of(
                    AsyncTaskStreamConstants.FIELD_RESUME_ID, resumeId.toString(),
                    AsyncTaskStreamConstants.FIELD_CONTENT, content,
                    AsyncTaskStreamConstants.FIELD_RETRY_COUNT,"0"
            );

            String messageId = redisService.streamAdd(
                    AsyncTaskStreamConstants.RESUME_ANALYZE_STREAM_KEY,
                    message,
                    AsyncTaskStreamConstants.STREAM_MAX_LEN
            );

            log.info("send success to stream resumeId={}, messageId={}", resumeId, messageId);
        } catch (RuntimeException e) {
            log.error("send analysis failed: resumeId={}, error={}", resumeId, e.getMessage(), e);
            updateAnalyzeStatus(resumeId, AsyncTaskStatus.FAILED, "task is failed: " + e.getMessage());
        }
    }

    private void updateAnalyzeStatus(Long resumeId, AsyncTaskStatus status, String error) {
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            resume.setAnalyzeStatus(status);
            if (error != null) {
                resume.setAnalyzeError(error.length() > 500 ? error.substring(0, 500) : error);
            }

            resumeRepository.save(resume);
        });
    }


}

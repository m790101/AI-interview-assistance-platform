package interview.guide.modules.knowledgeBase.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.modules.knowledgeBase.model.KnowledgeBaseEntity;
import interview.guide.modules.knowledgeBase.model.VectorStatus;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBasePersistenceService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    // handle duplication (increment count)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleDuplicateKnowledgeBase(KnowledgeBaseEntity kb, String fileHash){
        log.info("found duplication, return history: kb = {}", kb.getId());
        kb.incrementAccessCount();
        knowledgeBaseRepository.save(kb);

        return Map.of(
                "knowledgeBase", Map.of(
                        "id", kb.getId(),
                        "name", kb.getName(),
                        "fileSize", kb.getFileSize(),
                        "contentLength", 0  // 不再存储content，所以长度为0
                ),
                "storage", Map.of(
                        "fileKey", kb.getStorageKey() != null ? kb.getStorageKey() : "",
                        "fileUrl", kb.getStorageUrl() != null ? kb.getStorageUrl() : ""
                ),
                "duplicate", true
        );
    }

    // save data to knowledge base entity

    public KnowledgeBaseEntity saveKnowledgeBase(MultipartFile file, String name, String category, String storageKey, String storageUrl, String fileHash){
        try {
            KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
            kb.setFileHash(fileHash);
            kb.setName(name != null && !name.trim().isEmpty()? name: extractNameFromFilename(file.getOriginalFilename()));
            kb.setCategory(category != null && !category.trim().isEmpty() ? category.trim() : null);
            kb.setOriginalFilename(file.getOriginalFilename());
            kb.setFileSize(file.getSize());
            kb.setContentType(file.getContentType());
            kb.setStorageKey(storageKey);
            kb.setStorageUrl(storageUrl);

            KnowledgeBaseEntity saved = knowledgeBaseRepository.save(kb);
            log.info("save successfully");
            return saved;
        } catch (Exception e) {
            log.error("save failed");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,"save failed");
        }
    }

    // update task as pending
    public void updateVectorStatusToPending(Long kbId){
        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND, "knowledge base not found"));
        kb.setVectorStatus(VectorStatus.PENDING);
        kb.setVectorError(null);
        knowledgeBaseRepository.save(kb);
        log.info("knowledge vector status update to pending kbId = {}", kbId);
    }


    // extract name from file name
    /**
     * 从文件名提取知识库名称（去除扩展名）
     */
    private String extractNameFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unknown filename";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(0, lastDot);
        }
        return filename;
    }
}

package interview.guide.modules.knowledgeBase.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.infrastructure.file.FileHashService;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.knowledgeBase.model.KnowledgeBaseEntity;
import interview.guide.modules.knowledgeBase.model.VectorStatus;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import interview.guide.modules.knowledgeBase.listener.VectorizeStreamProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseUploadService {

    private final FileValidationService fileValidationService;
    private final FileStorageService storageService;
    private final FileHashService fileHashService;
    private final KnowledgeBaseParserService parserService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBasePersistenceService persistenceService;
//    private final KnowledgeBaseParseService parseService;
    private final VectorizeStreamProducer vectorizeStreamProducer;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public Map<String, Object> uploadKnowledgeBase(MultipartFile file, String name, String category){
            // validate file
            fileValidationService.validateFile(file, MAX_FILE_SIZE, "knowledgeBase");
            String fileName = file.getOriginalFilename();

            log.info("upload knowledge init: {}, size: {} bytes, category: {}", fileName, file.getSize(), category);

            // check file type
            String contentType = parserService.detectContentType(file);
            validateContentType(contentType, fileName);

            // find the file, if it's exists then return
            String fileHash = fileHashService.calculateHash(file);
            Optional<KnowledgeBaseEntity> existingDb = knowledgeBaseRepository.findByFileHash(fileHash);
            if(existingDb.isPresent()){
                log.info("found the knowledgeBase hash = {}", fileHash);
                return persistenceService.handleDuplicateKnowledgeBase(existingDb.get(), fileHash);
            }

            // parse the file
            String content = parserService.parseContent(file);
            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法从文件中提取文本内容，请确保文件格式正确");
            }


            // upload to RustFS
            String fileKey = storageService.uploadKnowledgeBase(file);
            String fileUrl = storageService.getFileUrl(fileKey);
            log.info("save to RustFS: {}", fileKey);

            // save in the vector db
            KnowledgeBaseEntity savedKb = persistenceService.saveKnowledgeBase(file, name, category, fileKey, fileUrl, fileHash);

            // 7. 发送向量化任务到 Redis Stream（异步处理）
            vectorizeStreamProducer.sendVectorizeTask(savedKb.getId(), content);

            log.info("知识库上传完成，向量化任务已入队: {}, kbId={}", fileName, savedKb.getId());

            // 8. 返回结果（状态为 PENDING，前端可轮询获取最新状态）
            return Map.of(
                    "knowledgeBase", Map.of(
                            "id", savedKb.getId(),
                            "name", savedKb.getName(),
                            "category", savedKb.getCategory() != null ? savedKb.getCategory() : "",
                            "fileSize", savedKb.getFileSize(),
                            "contentLength", content.length(),
                            "vectorStatus", VectorStatus.PENDING.name()
                    ),
                    "storage", Map.of(
                            "fileKey", fileKey,
                            "fileUrl", fileUrl
                    ),
                    "duplicate", false
            );


    }


    private void validateContentType(String contentType, String fileName) {
        fileValidationService.validateContentType(
                contentType,
                fileName,
                fileValidationService::isKnowledgeBaseMimeType,
                fileValidationService::isMarkdownExtension,
                "不支持的文件类型: " + contentType + "，支持的类型：PDF、DOCX、DOC、TXT、MD等"
        );
    }

//    /**
//     * 重新向量化知识库（手动重试）
//     * 从 RustFS 重新下载文件并发送向量化任务
//     *
//     * @param kbId 知识库ID
//     */
//    public void revectorize(Long kbId) {
//        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(kbId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
//
//        log.info("开始重新向量化知识库: kbId={}, name={}", kbId, kb.getName());
//
//        // 1. 下载文件并解析内容
//        String content = parseService.downloadAndParseContent(kb.getStorageKey(), kb.getOriginalFilename());
//        if (content == null || content.trim().isEmpty()) {
//            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法从文件中提取文本内容");
//        }
//
//        // 2. 更新状态为 PENDING（通过单独的 Service 保证事务生效）
//        persistenceService.updateVectorStatusToPending(kbId);
//
//        // 3. 发送向量化任务到 Stream
//        vectorizeStreamProducer.sendVectorizeTask(kbId, content);
//
//        log.info("重新向量化任务已发送: kbId={}", kbId);
//    }
}

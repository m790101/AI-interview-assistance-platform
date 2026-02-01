package interview.guide.modules.resume.service;

import interview.guide.common.config.AppConfigProperties;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.infrastructure.file.FileValidationService;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.resume.model.ResumeAnalysisEntity;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadService {

    private final ResumeParseService parseService;
    private final FileStorageService storageService;
    private final ResumePersistenceService persistenceService;
    private final AppConfigProperties appConfig;
    private final FileValidationService fileValidationService;
//    private final AnalyzeStreamProducer analyzeStreamProducer;
    private final ResumeGradingService gradingService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final ResumeRepository resumeRepository;

    public Map<String, Object> uploadAndAnalyze(MultipartFile file){
        // validation
        fileValidationService.validateFile(file, MAX_FILE_SIZE, "resume");
        String contentType = parseService.detectContentType(file);
        validateContentType(contentType);

        // check file exists
        Optional<ResumeEntity> existingResume = persistenceService.findExistingResume(file);
//        if(existingResume.isPresent()){
//            return handleDuplicateResume(existingResume.get());
//        }
        // parse file
        String resumeText = parseService.parseResume(file);

        // upload
        String fileKey = storageService.uploadResume(file);
        String fileUrl = storageService.getFileUrl(fileKey);
        log.info("upload success to RustFS: {}", fileKey);

        // save to db
        ResumeEntity savedResume = persistenceService.saveResume(file, resumeText, fileKey, fileUrl);

        // ai analyze
        ResumeAnalysisResponse analysis = gradingService.analyzeResume(resumeText);

//         4. 再次检查简历是否存在（分析期间可能被删除）
//        ResumeEntity resume = resumeRepository.findById(savedResume.getId()).orElse(null);
//        if (resume == null) {
//            log.warn("简历在分析期间被删除，跳过保存结果: resumeId={}", savedResume.getId());
//        }
        persistenceService.saveAnalysis(savedResume, analysis);

        // 4. 更新状态为 COMPLETED
//        updateAnalyzeStatus(resumeId, AsyncTaskStatus.COMPLETED, null);

        // return result

        return Map.of(
                "resume", Map.of(
                        "id", savedResume.getId(),
                        "filename", savedResume.getOriginalFilename(),
                        "analyzeStatus", AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", fileKey,
                        "fileUrl", fileUrl,
                        "resumeId", savedResume.getId()
                ),
                "duplicate", false
        );
    }

    private Map<String, Object> handleDuplicateResume(ResumeEntity resume) {
        log.info("found resume, return history analysis: resumeId = {}", resume.getId());

        // get history analysis
        Optional<ResumeAnalysisResponse> analysisOpt = persistenceService.getLatestAnalysisAsDTO(resume.getId());
        // 已有分析结果，直接返回
        // 没有分析结果（可能之前分析失败），返回当前状态
        return analysisOpt.map(resumeAnalysisResponse -> Map.of(
                "analysis", resumeAnalysisResponse,
                "storage", Map.of(
                        "fileKey", resume.getStorageKey() != null ? resume.getStorageKey() : "",
                        "fileUrl", resume.getStorageUrl() != null ? resume.getStorageUrl() : "",
                        "resumeId", resume.getId()
                ),
                "duplicate", true
        )).orElseGet(() -> Map.of(
                "resume", Map.of(
                        "id", resume.getId(),
                        "filename", resume.getOriginalFilename(),
                        "analyzeStatus", resume.getAnalyzeStatus() != null ? resume.getAnalyzeStatus().name() : AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", resume.getStorageKey() != null ? resume.getStorageKey() : "",
                        "fileUrl", resume.getStorageUrl() != null ? resume.getStorageUrl() : "",
                        "resumeId", resume.getId()
                ),
                "duplicate", true
        ));
    }


    /**
     *  validate file type
     */
    private void validateContentType(String contentType) {
        fileValidationService.validateContentTypeByList(
                contentType,
                appConfig.getAllowedTypes(),
                "不支持的文件类型: " + contentType
        );
    }

}

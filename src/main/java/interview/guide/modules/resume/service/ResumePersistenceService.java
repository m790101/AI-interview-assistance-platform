package interview.guide.modules.resume.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.infrastructure.file.FileHashService;
import interview.guide.infrastructure.mapper.ResumeMapper;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.resume.model.ResumeAnalysisEntity;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.repository.ResumeAnalysisRepository;
import interview.guide.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumePersistenceService {
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final ResumeMapper resumeMapper;
    private final FileHashService fileHashService;


    /**
     * check if file is already exists (fileHash)
     *
     * @param file uploaded file
     * @return resume or empty
     */
    public Optional<ResumeEntity> findExistingResume(MultipartFile file){
        try {
            // make hash from hash service
            String fileHash = fileHashService.calculateHash(file);
            // check if it's in db
            Optional<ResumeEntity> existing = resumeRepository.findByFileHash(fileHash);
            if (existing.isPresent()) {
                log.info("Duplicate file");
                ResumeEntity resume = existing.get();
                resume.incrementAccessCount();
                resumeRepository.save(resume);
            }
            return existing;
        } catch (Exception e){
            log.error("error on finding existing: {}",e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * save new resume
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeEntity saveResume(MultipartFile file, String resumeText,
                                   String storageKey, String storageUrl) {
        try {
            String fileHash = fileHashService.calculateHash(file);

            ResumeEntity resume = new ResumeEntity();
            resume.setFileHash(fileHash);
            resume.setOriginalFilename(file.getOriginalFilename());
            resume.setFileSize(file.getSize());
            resume.setContentType(file.getContentType());
            resume.setStorageKey(storageKey);
            resume.setStorageUrl(storageUrl);
            resume.setResumeText(resumeText);

            ResumeEntity saved = resumeRepository.save(resume);
            log.info("resume saved: id={}, hash={}", saved.getId(), fileHash);

            return saved;
        } catch (Exception e) {
            log.error("保存简历失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.RESUME_UPLOAD_FAILED, "save resume failed");
        }
    }


    /**
     * get lastest analysis
     */
    public Optional<ResumeAnalysisEntity> getLatestAnalysis(Long resumeId) {
        return Optional.ofNullable(analysisRepository.findFirstByResumeIdOrderByAnalyzedAtDesc(resumeId));
    }

    /**
     * get the newest analysis as DTO
     */
    public Optional<ResumeAnalysisResponse> getLatestAnalysisAsDTO(Long resumeId) {
        return getLatestAnalysis(resumeId).map(this::entityToDTO);
    }

    /**
     * transfer to DTO
     */
    public ResumeAnalysisResponse entityToDTO(ResumeAnalysisEntity entity) {
        try {
            List<String> strengths = objectMapper.readValue(
                    entity.getStrengthsJson() != null ? entity.getStrengthsJson() : "[]",
                    new TypeReference<>() {
                    }
            );

            List<ResumeAnalysisResponse.Suggestion> suggestions = objectMapper.readValue(
                    entity.getSuggestionsJson() != null ? entity.getSuggestionsJson() : "[]",
                    new TypeReference<>() {
                    }
            );

            return new ResumeAnalysisResponse(
                    entity.getOverallScore(),
                    resumeMapper.toScoreDetail(entity),  // 使用MapStruct自动映射
                    entity.getSummary(),
                    strengths,
                    suggestions,
                    entity.getResume().getResumeText()
            );
        } catch (JacksonException e) {
            log.error("反序列化评测结果失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "获取评测结果失败");
        }
    }

    /**
     * 根据ID获取简历
     */
    public Optional<ResumeEntity> findById(Long id) {
        return resumeRepository.findById(id);
    }

    /**
     * save analysis
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeAnalysisEntity saveAnalysis(ResumeEntity resume, ResumeAnalysisResponse analysis) {
        try {
            // 使用 MapStruct 映射基础字段
            ResumeAnalysisEntity entity = resumeMapper.toAnalysisEntity(analysis);
            entity.setResume(resume);

            // JSON 字段需要手动序列化
            entity.setStrengthsJson(objectMapper.writeValueAsString(analysis.strengths()));
            entity.setSuggestionsJson(objectMapper.writeValueAsString(analysis.suggestions()));

            ResumeAnalysisEntity saved = analysisRepository.save(entity);
            log.info("简历评测结果已保存: analysisId={}, resumeId={}, score={}",
                    saved.getId(), resume.getId(), analysis.overallScore());

            return saved;
        } catch (JacksonException e) {
            log.error("序列化评测结果失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "保存评测结果失败");
        }
    }


}

package interview.guide.modules.resume.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.infrastructure.mapper.ResumeMapper;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.resume.model.ResumeAnalysisEntity;
import interview.guide.modules.resume.model.ResumeDetailDTO;
import interview.guide.modules.resume.model.ResumeEntity;
import interview.guide.modules.resume.model.ResumeListItemDTO;
import interview.guide.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeHistoryService {

    private final ResumeRepository resumeRepository;
    private final ResumePersistenceService resumePersistenceService;
    private final ResumeMapper resumeMapper;
    private final ObjectMapper objectMapper;

    public List<ResumeListItemDTO> getAllResumes(){
        return resumePersistenceService.findAllResumes()
                .stream()
                .map(resume ->{
                    Optional<ResumeAnalysisEntity> analysisOpt =  resumePersistenceService.getLatestAnalysis(resume.getId());
                    Integer latestScore = null;
                    LocalDateTime lastAnalyzedAt = null;

                    if (analysisOpt.isPresent()) {
                        ResumeAnalysisEntity analysis = analysisOpt.get();
                        latestScore = analysis.getOverallScore();
                        lastAnalyzedAt = analysis.getAnalyzedAt();
                    }
                    return new ResumeListItemDTO(
                            resume.getId(),
                            resume.getOriginalFilename(),
                            resume.getFileSize(),
                            resume.getUploadedAt(),
                            resume.getAccessCount(),
                            latestScore,
                            lastAnalyzedAt
                                );

                })
                .toList();
    }


    public ResumeDetailDTO getResumeDetail(Long id) {

        Optional<ResumeEntity> resumeOpt = resumePersistenceService.findById(id);
                if(resumeOpt.isEmpty()){
                    throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
                }
                ResumeEntity resume = resumeOpt.get();

        List<ResumeAnalysisEntity> analyses =  resumePersistenceService.findAnalysesByResumeId(id);
        List<ResumeDetailDTO.AnalysisHistoryDTO> analysisHistory = resumeMapper.toAnalysisHistoryDTOList(
                analyses,
                this::extractStrengths,
                this::extractSuggestions
        );

        return new ResumeDetailDTO(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getFileSize(),
                resume.getContentType(),
                resume.getStorageUrl(),
                resume.getUploadedAt(),
                resume.getAccessCount(),
                resume.getResumeText(),
                resume.getAnalyzeStatus(),
                resume.getAnalyzeError(),
                analysisHistory
//                interviewHistory
        );
    }


    /**
     * 从 JSON 提取 strengths
     */
    private List<String> extractStrengths(ResumeAnalysisEntity entity) {
        try {
            if (entity.getStrengthsJson() != null) {
                return objectMapper.readValue(
                        entity.getStrengthsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 strengths JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 从 JSON 提取 suggestions
     */
    private List<Object> extractSuggestions(ResumeAnalysisEntity entity) {
        try {
            if (entity.getSuggestionsJson() != null) {
                return objectMapper.readValue(
                        entity.getSuggestionsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 suggestions JSON 失败", e);
        }
        return List.of();
    }
}

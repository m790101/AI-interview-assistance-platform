package interview.guide.modules.resume.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.modules.interview.model.ResumeAnalysisResponse;
import interview.guide.modules.interview.model.ResumeAnalysisResponse.ScoreDetail;
import interview.guide.modules.interview.model.ResumeAnalysisResponse.Suggestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeGradingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeGradingService.class);

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<ResumeAnalysisResponseDTO> outputConverter;

    private record ResumeAnalysisResponseDTO(
            int overallScore,
            ScoreDetailDTO scoreDetail,
            String summary,
            List<String> strengths,
            List<SuggestionDTO> suggestions
    ){}

    private record ScoreDetailDTO(
            int contentScore,
            int structureScore,
            int skillMatchScore,
            int expressionScore,
            int projectScore
    ){}

    private record SuggestionDTO(
            String category,
            String priority,
            String issue,
            String recommendation
    ){}
    public ResumeGradingService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/resume-analysis-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/resume-analysis-user.st") Resource userPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(ResumeAnalysisResponseDTO.class);
    }


    public ResumeAnalysisResponse analyzeResume(String resumeText){
        log.info("analyze resume init, text length is {}", resumeText.length());

        try{
            // load system prompt
            String systemPrompt = systemPromptTemplate.render();


            Map<String, Object> variables = new HashMap<>();
            variables.put("resumeText", resumeText);
            String userPrompt = userPromptTemplate.render(variables);

            // add format reference to system prompt
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

            // call AI
            ResumeAnalysisResponseDTO dto;

            try{
                dto = chatClient.prompt()
                        .system(systemPromptWithFormat)
                        .user(userPrompt)
                        .call()
                        .entity(outputConverter);
                log.debug("AI response successfully, overall score is {}", dto.overallScore());

            } catch (Exception e) {
                log.error("AI response failed: {}", e.getMessage());
                throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "resume analyze failed" + e.getMessage());
            }

            ResumeAnalysisResponse result = convertToResponse(dto, resumeText);
            log.info("analyze finished, final score is {}", result.overallScore());
            return result;

        } catch (Exception e) {
            log.error("analyze failed: {}", e.getMessage());
            return createErrorResponse(resumeText, e.getMessage());
        }

    }

    /**
     *  convert to DTO
     */
    private ResumeAnalysisResponse convertToResponse(ResumeAnalysisResponseDTO dto, String originalText) {
        ScoreDetail scoreDetail = new ScoreDetail(
                dto.scoreDetail().contentScore(),
                dto.scoreDetail().structureScore(),
                dto.scoreDetail().skillMatchScore(),
                dto.scoreDetail().expressionScore(),
                dto.scoreDetail().projectScore()
        );

        List<Suggestion> suggestions = dto.suggestions().stream()
                .map(s -> new Suggestion(s.category(), s.priority(), s.issue(), s.recommendation()))
                .toList();

        return new ResumeAnalysisResponse(
                dto.overallScore(),
                scoreDetail,
                dto.summary(),
                dto.strengths(),
                suggestions,
                originalText
        );
    }

    private ResumeAnalysisResponse createErrorResponse(String originalText, String errorMessage ) {
        return new ResumeAnalysisResponse(
                0,
                new ScoreDetail(0, 0, 0, 0, 0),
                "error: " + errorMessage,
                List.of(),
                List.of(new Suggestion(
                        "system",
                        "high",
                        "AI unavailable",
                        "please try later and check AI status"
                )),
                originalText
        );
    }
}

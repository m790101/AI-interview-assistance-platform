package interview.guide.modules.knowledgeBase.model;

public record KnowledgeBaseStatsDTO(
        long totalCount,
        long totalQuestionCount,
        long totalAccessCount,
        long completedCount,
        long processingCount
) {
}

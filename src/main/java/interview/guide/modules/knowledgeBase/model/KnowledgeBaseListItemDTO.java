package interview.guide.modules.knowledgeBase.model;

import java.time.LocalDateTime;

public record KnowledgeBaseListItemDTO(
        Long id,
        String name,
        String category,
        String originalFilename,
        Long fileSize,
        String contentType,
        LocalDateTime uploadedAt,
        LocalDateTime lastAccessedAt,
        Integer accessCount,
        Integer questionCount,
        VectorStatus vectorStatus,
        String vectorError,
        Integer chunkCount
) {
}

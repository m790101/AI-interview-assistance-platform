package interview.guide.modules.knowledgeBase.model;

public record QueryResponse(
        String answer,
        Long knowledgeBaseId,
        String knowledgeBaseName
) {

}

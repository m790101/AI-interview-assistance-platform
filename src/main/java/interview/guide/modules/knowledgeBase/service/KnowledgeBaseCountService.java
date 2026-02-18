package interview.guide.modules.knowledgeBase.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.modules.knowledgeBase.model.KnowledgeBaseEntity;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseCountService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;


    public void updateQuestionCounts(List<Long> knowledgeBaseIds){
        if(knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()){
            return;
        }
        // remove duplication
        List<Long> uniqueIds = knowledgeBaseIds.stream().distinct().toList();

        // check existence
        Set<Long> existingIds = new HashSet<>(knowledgeBaseRepository
                .findAllById(uniqueIds)
                .stream()
                .map(KnowledgeBaseEntity::getId)
                .toList()
        );

        for(Long id : uniqueIds){
            if(!existingIds.contains(id)){
                throw new BusinessException(ErrorCode.NOT_FOUND, "knowledge base is not exist" + id);
            }
        }




    }

}

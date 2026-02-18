package interview.guide.modules.knowledgeBase.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.modules.knowledgeBase.model.KnowledgeBaseEntity;
import interview.guide.modules.knowledgeBase.model.RagChatSessionEntity;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import interview.guide.modules.knowledgeBase.repository.RagChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseDeleteService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileStorageService storageService;
    private final RagChatSessionRepository sessionRepository;
    private final KnowledgeBaseVectorService vectorService;

    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id){
        // 1. get knowledge base
        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,"knowledge is not exists"));

        // 2. delete all related rag
        List<RagChatSessionEntity> sessions = sessionRepository.findKnowledgeBaseIds(List.of(id));
        for (RagChatSessionEntity session : sessions) {
            session.getKnowledgeBases().removeIf(kbEntity -> kbEntity.getId().equals(id));
            sessionRepository.save(session);
            log.debug("已从会话中移除知识库关联: sessionId={}, kbId={}", session.getId(), id);
        }
        if (!sessions.isEmpty()) {
            log.info("已从 {} 个会话中移除知识库关联: kbId={}", sessions.size(), id);
        }
        // 3. delete vector data
        try {
            vectorService.deleteByKnowledgeBaseId(id);
        } catch (Exception e) {
            log.warn("删除向量数据失败，继续删除知识库: kbId={}, error={}", id, e.getMessage());
        }

        // 4. delete file in RustFs
        try {
            storageService.deleteKnowledgeBase(kb.getStorageKey());
        } catch (Exception e) {
            log.warn("删除RustFS文件失败，继续删除知识库记录: kbId={}, error={}", id, e.getMessage());
        }

        // 5. delete knowledge base
        knowledgeBaseRepository.deleteById(id);
        log.info("知识库已删除: id={}", id);

    }

}

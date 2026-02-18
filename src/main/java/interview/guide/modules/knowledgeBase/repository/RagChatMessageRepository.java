package interview.guide.modules.knowledgeBase.repository;

import interview.guide.modules.knowledgeBase.model.RagChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagChatMessageRepository extends JpaRepository<RagChatMessageEntity, Long> {
    Long countByType(RagChatMessageEntity.MessageType type);

    /**
     *
     * get all message with order
     *
     */
    List<RagChatMessageEntity> findBySessionIdOrderByMessageOrderAsc(Long sessionId);
}

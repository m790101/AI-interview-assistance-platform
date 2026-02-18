package interview.guide.modules.knowledgeBase.service;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import interview.guide.infrastructure.mapper.KnowledgeBaseMapper;
import interview.guide.infrastructure.mapper.RagChatMapper;
import interview.guide.modules.knowledgeBase.model.*;
import interview.guide.modules.knowledgeBase.repository.KnowledgeBaseRepository;
import interview.guide.modules.knowledgeBase.repository.RagChatMessageRepository;
import interview.guide.modules.knowledgeBase.repository.RagChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import interview.guide.modules.knowledgeBase.model.RagChatDTO.CreateSessionRequest;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.SessionDTO;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.SessionDetailDTO;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.SessionListItemDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagChatSessionService {

    private final RagChatSessionRepository sessionRepository;
    private final RagChatMessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagChatMapper ragChatMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseQueryService queryService;


    @Transactional
    public SessionDTO createSessions(CreateSessionRequest request) {
        // 1. validate if knowledge exists
        List<KnowledgeBaseEntity> knowledgeBases = knowledgeBaseRepository.findAllById(request.knowledgeBaseIds());

        if (knowledgeBases.size() != request.knowledgeBaseIds().size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部分知识库不存在");
        }

        // 2. create session
        RagChatSessionEntity session = new RagChatSessionEntity();
        session.setTitle(request.title() != null && !request.title().isBlank()
                ? request.title()
                : generateTitle(knowledgeBases));
        session.setKnowledgeBases(new HashSet<>(knowledgeBases));
        session = sessionRepository.save(session);

        log.info("创建 RAG 聊天会话: id={}, title={}", session.getId(), session.getTitle());
        return ragChatMapper.toSessionDTO(session);
    }

    public List<SessionListItemDTO> listSessions() {
        return sessionRepository.findAllOrderByPinnedAndUpdatedAtDesc()
                .stream()
                .map(ragChatMapper::toSessionListItemDTO)
                .toList();
    }

    public SessionDetailDTO getSessionDetail(Long sessionId) {
        RagChatSessionEntity session = sessionRepository.findByIdWithKnowledgeBases(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "session detail not found"));


        // 再单独加载消息（避免笛卡尔积）
        List<RagChatMessageEntity> messages = messageRepository
                .findBySessionIdOrderByMessageOrderAsc(sessionId);

        // 转换知识库列表
        List<KnowledgeBaseListItemDTO> kbDTOs = knowledgeBaseMapper.toListItemDTOList(
                new java.util.ArrayList<>(session.getKnowledgeBases())
        );

        return ragChatMapper.toSessionDetailDTO(session, messages, kbDTOs);

    }


    /**
     * update session's knowledge base relation
     */
    @Transactional
    public void updateSession(Long sessionId, List<Long> knowledgeBaseIds) {
        RagChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "session is not found"));

        List<KnowledgeBaseEntity> knowledgeBases = knowledgeBaseRepository.findAllById(knowledgeBaseIds);

        session.setKnowledgeBases(new HashSet<>(knowledgeBases));

        sessionRepository.save(session);
        log.info("update session successfully: sessionId={}, kbIds={}", sessionId, knowledgeBaseIds);

    }


    @Transactional
    public void deleteSession(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "session is not exists");
        }
        sessionRepository.deleteById(sessionId);
        log.info("delete session: session = {}", sessionId);
    }

    public Long prepareStreamMessage(Long sessionId, String question) {
        RagChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND, "session is not found"));


        // 获取当前消息数量作为起始顺序
        // get offset from message count
        int nextOrder = session.getMessageCount();

        // user message
        RagChatMessageEntity userMessage = new RagChatMessageEntity();
        userMessage.setType(RagChatMessageEntity.MessageType.USER);
        userMessage.setSession(session);
        userMessage.setContent(question);
        userMessage.setMessageOrder(nextOrder);
        userMessage.setCompleted(true);
        messageRepository.save(userMessage);


        // create assistant message（未完成）
        RagChatMessageEntity assistantMessage = new RagChatMessageEntity();
        assistantMessage.setSession(session);
        assistantMessage.setType(RagChatMessageEntity.MessageType.ASSISTANT);
        assistantMessage.setContent("");
        assistantMessage.setMessageOrder(nextOrder + 1);
        assistantMessage.setCompleted(false);
        assistantMessage = messageRepository.save(assistantMessage);

        // 更新会话消息数量
        session.setMessageCount(nextOrder + 2);
        sessionRepository.save(session);

        log.info("准备流式消息: sessionId={}, messageId={}", sessionId, assistantMessage.getId());

        return assistantMessage.getId();
    }

    /**
     *  finish and save to db when stream end
     */
    @Transactional
    public void completeStreamMessage(Long messageId, String content) {
        RagChatMessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "message is not exists"));

        message.setContent(content);
        message.setCompleted(true);
        messageRepository.save(message);

        log.info("finish message: messageId={}, contentLength={}", messageId, content.length());
    }


    public String getStreamAnswer(Long sessionId, String question) {
        // get knowledge base id
        RagChatSessionEntity session = sessionRepository.findByIdWithKnowledgeBases(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "session not found"));

        List<Long> kbIds = session.getKnowledgeBaseIds();

        // call query service to get answer
        return queryService.answerQuestion(kbIds, question);
    }

    // ========== 私有方法 ==========

    private String generateTitle(List<KnowledgeBaseEntity> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return "新对话";
        }
        if (knowledgeBases.size() == 1) {
            return knowledgeBases.getFirst().getName();
        }
        return knowledgeBases.size() + " 个知识库对话";
    }

}

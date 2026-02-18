package interview.guide.modules.knowledgeBase;

import interview.guide.common.result.Result;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.*;
import interview.guide.modules.knowledgeBase.service.RagChatSessionService;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.SessionDTO;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.CreateSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/rag-chat")
@RequiredArgsConstructor
@Slf4j
public class RagChatController {

    private final RagChatSessionService sessionService;

    /**
     * create new chat session
     */
    @PostMapping("/sessions")
    public Result<SessionDTO> createChatSession(@RequestBody CreateSessionRequest request){
        return Result.success(sessionService.createSessions(request));
    }

    /**
     * get sessions
     */
    @GetMapping("/sessions")
    public Result<List<SessionListItemDTO>> listSessions(){
        return Result.success(sessionService.listSessions());
    }

//    /**
//     * update new chat session
//     */
//    @PostMapping(name="/sessions/{id}")
//    public Result<SessionDTO> updateChatSession(@RequestParam String id, @RequestBody CreateSessionRequest request){
//        return Result.success(sessionService.createSessions(request));
//    }



    @GetMapping("/sessions/{sessionId}")
    public Result<SessionDetailDTO> getSessionDetail(@PathVariable Long sessionId){
        return Result.success(sessionService.getSessionDetail(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages/stream")
//    public Result<MessageDTO> sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request){
    public String sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request){
        log.info("get the RAG session request: session id ={}, question = {}", sessionId, request.question());

        // 1. prepare message
        Long messageId = sessionService.prepareStreamMessage(sessionId,request.question());
//
//        // 2. send message
//        StringBuilder fullContent = new StringBuilder();
//        String answer = sessionService.getStreamAnswer(sessionId, request.question());
        String answer =  sessionService.getStreamAnswer(sessionId, request.question());
        sessionService.completeStreamMessage(messageId, answer);
        return answer;
    }
}

package interview.guide.modules.knowledgeBase;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.*;
import interview.guide.modules.knowledgeBase.service.RagChatSessionService;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.SessionDTO;
import interview.guide.modules.knowledgeBase.model.RagChatDTO.CreateSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    public Result<SessionDTO> createChatSession(@RequestBody CreateSessionRequest request) {
        return Result.success(sessionService.createSessions(request));
    }

    /**
     * get sessions
     */
    @GetMapping("/sessions")
    public Result<List<SessionListItemDTO>> listSessions() {
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
    public Result<SessionDetailDTO> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(sessionService.getSessionDetail(sessionId));
    }

    @PostMapping(value = "/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Flux<ServerSentEvent<String>> sendMessage(@PathVariable Long sessionId,
                                                     @Valid @RequestBody SendMessageRequest request) {
        log.info("get the RAG session request: session id ={}, question = {}", sessionId, request.question());

        // 1. prepare message
        Long messageId = sessionService.prepareStreamMessage(sessionId, request.question());
        // 2. send message
        StringBuilder fullContent = new StringBuilder();
        return sessionService.getStreamAnswer(sessionId, request.question())
                .doOnNext(fullContent::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk.replace("\n", "\\n").replace("\r", "\\r"))
                        .build())
                .doOnComplete(() -> {
                    sessionService.completeStreamMessage(messageId, fullContent.toString());
                    log.info("stream response finished");
                })
                .doOnError(e -> {
                    // 错误时也保存已接收的内容
                    String content = !fullContent.isEmpty()
                            ? fullContent.toString()
                            : "【错误】回答生成失败：" + e.getMessage();
                    sessionService.completeStreamMessage(messageId, content);
                    log.error("RAG 聊天流式错误: sessionId={}", sessionId, e);
                });
    }
}

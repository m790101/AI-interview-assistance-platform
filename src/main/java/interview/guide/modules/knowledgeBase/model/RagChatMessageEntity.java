package interview.guide.modules.knowledgeBase.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rag_chat_messages", indexes = {
        @Index(name = "idx_rag_message_session", columnList = "session_id"),
        @Index(name = "idx_rag_message_order", columnList = "session_id, messageOrder")
})
@Getter
@Setter
@NoArgsConstructor
public class RagChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * related conversation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RagChatSessionEntity session;

    /**
     * message type: USER or ASSISTANT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    /**
     * message content
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;


    /**
     * message order(for sorting)
     */
    @Column(nullable = false)
    private Integer messageOrder;


    /**
     * created time
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * updated time
     */
    private LocalDateTime updatedAt;

    /**
     *  is completed
     */
    private Boolean completed = true;


    public enum MessageType {
        USER,      // 用户消息
        ASSISTANT  // AI 回答
    }


    @PrePersist
    protected void onCreat(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    /**
     *  get type(lower case, for frontend use)
     */
    public String getTypeString() {
        return type.name().toLowerCase();
    }
}

package interview.guide.modules.resume.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name="resume_analyses")
public class ResumeAnalysisEntity {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private ResumeEntity resume;


    // total score(0 - 100)
    private Integer overallScore;

    // 各维度评分
    private Integer contentScore;      // 内容完整性 (0-25)
    private Integer structureScore;    // 结构清晰度 (0-20)
    private Integer skillMatchScore;   // 技能匹配度 (0-25)
    private Integer expressionScore;   // 表达专业性 (0-15)
    private Integer projectScore;      // 项目经验 (0-15)

    // summary
    @Column(columnDefinition = "TEXT")
    private String summary;

    // list of pros (in JSON)
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    // list of cons (in JSON)
    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;

    // 评测时间
    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }

}

package interview.guide.modules.resume.model;

import interview.guide.common.model.AsyncTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resume_hash", columnList = "fileHash", unique = true)
})
@Setter
@Getter
public class ResumeEntity {
    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // file SHA-256, avoid duplicate
    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(nullable = false)
    private String originalFilename;

    private Long fileSize;

    private String contentType;

    // file key from RustFS
    @Column(length = 500)
    private String storageKey;

    // url from RustFS
    @Column(length = 1000)
    private String storageUrl;

    // text after parsing
    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private LocalDateTime lastAccessedAt;

    private Integer accessCount = 0;


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    private String analyzeError;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 1;
    }


    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

}

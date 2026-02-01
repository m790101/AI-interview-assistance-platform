package interview.guide.modules.resume.repository;

import interview.guide.modules.resume.model.ResumeAnalysisEntity;
import interview.guide.modules.resume.model.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysisEntity,Long> {
    ResumeAnalysisEntity findFirstByResumeIdOrderByAnalyzedAtDesc(Long resumeId);

    List<ResumeAnalysisEntity> findByResumeOrderByAnalyzedAtDesc(ResumeEntity resume);

    List<ResumeAnalysisEntity> findByResumeIdOrderByAnalyzedAtDesc(Long resumeId);


}

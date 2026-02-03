package interview.guide.modules.resume;

import interview.guide.common.result.Result;
import interview.guide.modules.resume.dto.ResumeResponse;
import interview.guide.modules.resume.model.ResumeDetailDTO;
import interview.guide.modules.resume.model.ResumeListItemDTO;
import interview.guide.modules.resume.service.ResumeHistoryService;
import interview.guide.modules.resume.service.ResumeUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeUploadService uploadService;
//    private final ResumeDeleteService deleteService;
    private final ResumeHistoryService historyService;

    @GetMapping(value = "/health")
    public String healthCheck(){
        return "ok";
    }


    @GetMapping
    public Result<List<ResumeListItemDTO>> getAllResumes() {
        List<ResumeListItemDTO> resumes = historyService.getAllResumes();
        return Result.success(resumes);
    }

    /**
     *  get resume/analysis detail
     */
    @GetMapping("/{id}/detail")
    public Result<ResumeDetailDTO> getResumeDetail(@PathVariable Long id) {
        ResumeDetailDTO detail = historyService.getResumeDetail(id);
        return Result.success(detail);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String,Object>> uploadAndAnalyze(@RequestParam("file") MultipartFile file){
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if(isDuplicate){
            return Result.success("analysis found", result);
        }

        return  Result.success(result);
    }
}

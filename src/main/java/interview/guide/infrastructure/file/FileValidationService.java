package interview.guide.infrastructure.file;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Service
public class FileValidationService {

    public void validateFile(MultipartFile file, long maxSizeBytes, String fileTypeName){
        if(file.isEmpty()){
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("please select file %s", fileTypeName)
                    );
        }
        if(file.getSize() > maxSizeBytes){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "exceed max size");
        }
    }

    public void validateContentTypeByList(String contentType, List<String> allowedType, String errorMessage){
        if(!isAllowedType(contentType,allowedType)){
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    errorMessage != null ? errorMessage : "unsupported file type" + contentType
                    );
        }
    }


    private boolean isAllowedType(String contentType, List<String> allowedTypes) {
        if (contentType == null || allowedTypes == null || allowedTypes.isEmpty()) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        return allowedTypes.stream()
                .anyMatch(allowed -> {
                    String lowerAllowed = allowed.toLowerCase();
                    return lowerContentType.contains(lowerAllowed) || lowerAllowed.contains(lowerContentType);
                });
    }

    public void validateContentType(String contentType, String fileName,
                                    Predicate<String> mimeTypeChecker,
                                    Predicate<String> extensionChecker,
                                    String errorMessage) {
        // check mime type
        if (mimeTypeChecker.test(contentType)) {
            return;
        }

        // 如果MIME类型不支持，再检查文件扩展名
        if (fileName != null && extensionChecker.test(fileName)) {
            return;
        }

        throw new BusinessException(ErrorCode.BAD_REQUEST,
                errorMessage != null ? errorMessage : "unsupported file type: " + contentType);
    }


    /**
     * Check if file type is Markdown
     */
    public boolean isMarkdownExtension(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".md") ||
                lowerFileName.endsWith(".markdown") ||
                lowerFileName.endsWith(".mdown");
    }

    /**
     * Check if the type is support by knowledge base
     */
    public boolean isKnowledgeBaseMimeType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.contains("pdf") ||
                lowerContentType.contains("msword") ||
                lowerContentType.contains("wordprocessingml") ||
                lowerContentType.contains("text/plain") ||
                lowerContentType.contains("text/markdown") ||
                lowerContentType.contains("text/x-markdown") ||
                lowerContentType.contains("text/x-web-markdown") ||
                lowerContentType.contains("application/rtf");
    }




}

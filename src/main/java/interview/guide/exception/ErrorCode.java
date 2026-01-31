package interview.guide.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Error code
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // ========== common error 1xxx ==========
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unAuthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    INTERNAL_ERROR(500, "internal error"),

    // ========== 简历模块错误 2xxx ==========
    RESUME_NOT_FOUND(2001, "resume not found"),
    RESUME_PARSE_FAILED(2002, "resume parse failed"),
    RESUME_UPLOAD_FAILED(2003, "resume upload failed"),
    RESUME_DUPLICATE(2004, "resume duplicate"),
    RESUME_FILE_EMPTY(2005, "resume file empty"),
    RESUME_FILE_TYPE_NOT_SUPPORTED(2006, "resume file type unsupported"),
    RESUME_ANALYSIS_FAILED(2007, "resume analysis failed"),
    RESUME_ANALYSIS_NOT_FOUND(2008, "resume analysis not found"),

    // ========== 面试模块错误 3xxx ==========
    INTERVIEW_SESSION_NOT_FOUND(3001, "面试会话不存在"),
    INTERVIEW_SESSION_EXPIRED(3002, "面试会话已过期"),
    INTERVIEW_QUESTION_NOT_FOUND(3003, "面试问题不存在"),
    INTERVIEW_ALREADY_COMPLETED(3004, "面试已完成"),
    INTERVIEW_EVALUATION_FAILED(3005, "面试评估失败"),
    INTERVIEW_QUESTION_GENERATION_FAILED(3006, "面试问题生成失败"),
    INTERVIEW_NOT_COMPLETED(3007, "面试尚未完成"),

    // ========== 存储模块错误 4xxx ==========
    STORAGE_UPLOAD_FAILED(4001, "storage upload failed"),
    STORAGE_DOWNLOAD_FAILED(4002, "storage download failed"),
    STORAGE_DELETE_FAILED(4003, "storage delete failed"),

    // ========== 导出模块错误 5xxx ==========
    EXPORT_PDF_FAILED(5001, "PDF export failed"),

    // ========== 知识库模块错误 6xxx ==========
    KNOWLEDGE_BASE_NOT_FOUND(6001, "知识库不存在"),
    KNOWLEDGE_BASE_PARSE_FAILED(6002, "知识库文件解析失败"),
    KNOWLEDGE_BASE_UPLOAD_FAILED(6003, "知识库上传失败"),
    KNOWLEDGE_BASE_QUERY_FAILED(6004, "知识库查询失败"),
    KNOWLEDGE_BASE_DELETE_FAILED(6005, "知识库删除失败"),
    KNOWLEDGE_BASE_VECTORIZATION_FAILED(6006, "知识库向量化失败"),

    // ========== AI服务错误 7xxx ==========
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂时不可用，请稍后重试"),
    AI_SERVICE_TIMEOUT(7002, "AI服务响应超时"),
    AI_SERVICE_ERROR(7003, "AI服务调用失败"),
    AI_API_KEY_INVALID(7004, "AI服务密钥无效"),
    AI_RATE_LIMIT_EXCEEDED(7005, "AI服务调用频率超限"),

    // ========== 限流模块错误 8xxx ==========
    RATE_LIMIT_EXCEEDED(8001, "请求过于频繁，请稍后再试");

    private final Integer code;
    private final String message;
}

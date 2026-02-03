package interview.guide.common.result;

import interview.guide.common.constant.CommonConstants;
import interview.guide.exception.ErrorCode;
import lombok.Getter;

@Getter
public class Result <T>{
    private final Integer code;
    private final String message;
    private final T data;


    private Result(Integer code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========== success =========
    public static <T> Result<T> success(){
        return new Result<>(CommonConstants.StatusCode.SUCCESS, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CommonConstants.StatusCode.SUCCESS, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CommonConstants.StatusCode.SUCCESS, message, data);
    }

    // ========== error =========

    public static <T> Result<T> error(String message) {
        return new Result<>(CommonConstants.StatusCode.SERVER_ERROR, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    // ========== helper ==========

    public boolean isSuccess() {
        return CommonConstants.StatusCode.SUCCESS == this.code;
    }

}

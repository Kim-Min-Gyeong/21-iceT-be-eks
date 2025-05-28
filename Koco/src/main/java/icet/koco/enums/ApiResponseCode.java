package icet.koco.enums;

import lombok.Getter;

@Getter
public enum ApiResponseCode {
    SUCCESS("SUCCESS"),
    UNAUTHORIZED("UNAUTHORIZED"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    COMMENT_FORBIDDEN("COMMENT_FORBIDDEN"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    BAD_REQUEST("BAD_REQUEST"),
    FORBIDDEN("FORBIDDEN"),
    NOT_FOUND("NOT_FOUND"),
    MAX_UPLOAD_SIZE_EXCEEDED("MAX_UPLOAD_SIZE_EXCEEDED");

    private final String code;

    ApiResponseCode(String code) {
        this.code = code;
    }
}

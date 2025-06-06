package icet.koco.enums;

import lombok.Getter;

@Getter
public enum ApiResponseCode {
    // 200
    SUCCESS("SUCCESS"),

    // 201
    CREATED("CREATED"),
    POST_CREATED("POST_CREATED"),

    // 400
    BAD_REQUEST("BAD_REQUEST"),

    // 401
    UNAUTHORIZED("UNAUTHORIZED"),

    // 403
    FORBIDDEN("FORBIDDEN"),

    // 404
    NOT_FOUND("NOT_FOUND"),
    USER_NOT_FOUND("USER_NOT_FOUND"),

    // 500
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),

    MAX_UPLOAD_SIZE_EXCEEDED("MAX_UPLOAD_SIZE_EXCEEDED");

    private final String code;

    ApiResponseCode(String code) {
        this.code = code;
    }
}

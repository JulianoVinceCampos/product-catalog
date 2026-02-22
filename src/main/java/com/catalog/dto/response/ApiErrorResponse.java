package com.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private int                 status;
    private String              error;
    private String              message;
    private String              path;
    private Instant             timestamp;
    private Map<String, String> fieldErrors;

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return ApiErrorResponse.builder().status(status).error(error)
                .message(message).path(path).timestamp(Instant.now()).build();
    }
}

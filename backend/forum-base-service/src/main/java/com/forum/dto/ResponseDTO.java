package com.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {
    private int status;
    private T data;
    private String message;

    public ResponseDTO(int status, T data) {
        this.status = status;
        this.data = data;
        this.message = null;
    }

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>(1, data);
    }

    public static <T> ResponseDTO<T> fail(T data) {
        return new ResponseDTO<>(0, data);
    }

    public static <T> ResponseDTO<T> fail(T data, String message) {
        return new ResponseDTO<>(0, data, message);
    }
}

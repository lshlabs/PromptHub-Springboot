package com.lshlabs.prompthubspring.common;

public record ApiEnvelope<T>(String status, T data, String message) {
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>("success", data, null);
    }

    public static <T> ApiEnvelope<T> success(T data, String message) {
        return new ApiEnvelope<>("success", data, message);
    }

    public static <T> ApiEnvelope<T> error(T data, String message) {
        return new ApiEnvelope<>("error", data, message);
    }
}

package com.lshlabs.prompthubspring.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fe.getField(), k -> new java.util.ArrayList<>()).add(fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of("message", "유효성 검사 실패", "errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<?> handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("message", "허용되지 않은 요청 메서드입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "서버 오류가 발생했습니다."));
    }
}

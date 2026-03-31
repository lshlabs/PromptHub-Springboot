package com.lshlabs.prompthubspring.post;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostUpsertRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validation_fails_whenTitleTooShort() {
        PostUpsertRequest request = validRequestWithTitle("짧다");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "title".equals(v.getPropertyPath().toString())));
    }

    @Test
    void validation_fails_whenSatisfactionOutOfRange() {
        PostUpsertRequest request = new PostUpsertRequest(
                "유효한 제목입니다",
                1L,
                2L,
                null,
                null,
                1L,
                null,
                List.of("tag1"),
                new BigDecimal("5.5"),
                "충분히 긴 prompt 내용입니다.",
                "충분히 긴 ai_response 내용입니다.",
                "opinion"
        );

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "satisfaction".equals(v.getPropertyPath().toString())));
    }

    @Test
    void validation_fails_whenSatisfactionBelowMinimum() {
        PostUpsertRequest request = new PostUpsertRequest(
                "유효한 제목입니다",
                1L,
                2L,
                null,
                null,
                1L,
                null,
                List.of("tag1"),
                new BigDecimal("0.0"),
                "충분히 긴 prompt 내용입니다.",
                "충분히 긴 ai_response 내용입니다.",
                "opinion"
        );

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "satisfaction".equals(v.getPropertyPath().toString())));
    }

    private PostUpsertRequest validRequestWithTitle(String title) {
        return new PostUpsertRequest(
                title,
                1L,
                2L,
                null,
                null,
                1L,
                null,
                List.of("tag1", "tag2"),
                new BigDecimal("4.5"),
                "충분히 긴 prompt 내용입니다.",
                "충분히 긴 ai_response 내용입니다.",
                "opinion"
        );
    }
}

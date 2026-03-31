package com.lshlabs.prompthubspring.post;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PostUpsertRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 5, max = 200, message = "제목은 5자 이상 200자 이하여야 합니다.")
        String title,

        @NotNull(message = "플랫폼은 필수입니다.")
        Long platform,

        Long model,

        @Size(max = 100, message = "model_etc는 100자 이하여야 합니다.")
        String model_etc,

        @Size(max = 100, message = "model_detail은 100자 이하여야 합니다.")
        String model_detail,

        @NotNull(message = "카테고리는 필수입니다.")
        Long category,

        @Size(max = 100, message = "category_etc는 100자 이하여야 합니다.")
        String category_etc,

        @Size(max = 10, message = "태그는 최대 10개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "빈 태그는 입력할 수 없습니다.") @Size(max = 50, message = "각 태그는 최대 50자까지 입력할 수 있습니다.") String> tags,

        @DecimalMin(value = "0.5", message = "만족도는 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "만족도는 5.0 이하여야 합니다.")
        BigDecimal satisfaction,

        @NotBlank(message = "prompt는 필수입니다.")
        @Size(min = 10, message = "prompt는 10자 이상이어야 합니다.")
        String prompt,

        @NotBlank(message = "ai_response는 필수입니다.")
        @Size(min = 10, message = "ai_response는 10자 이상이어야 합니다.")
        String ai_response,

        String additional_opinion
) {
}

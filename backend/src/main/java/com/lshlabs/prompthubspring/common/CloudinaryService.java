package com.lshlabs.prompthubspring.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lshlabs.prompthubspring.config.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public String uploadProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "업로드할 이미지 파일이 비어 있습니다.");
        }

        try {
            Map<?, ?> uploaded = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryProperties.getProfileFolder(),
                            "resource_type", "image",
                            "overwrite", true,
                            "unique_filename", true
                    )
            );

            Object secureUrl = uploaded.get("secure_url");
            if (secureUrl instanceof String url && StringUtils.hasText(url)) {
                return url;
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary 업로드 결과에 secure_url이 없습니다.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary 업로드 중 I/O 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary 업로드에 실패했습니다.");
        }
    }
}

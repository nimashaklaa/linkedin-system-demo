package com.linkedin.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String imageKey) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        log.info("Uploading file to S3 with key: {}", imageKey);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (var inputStream = file.getInputStream()) {
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        }
        log.info("File uploaded to S3 successfully with key: {}", imageKey);
        return imageKey;
    }

    public String getFileUrl(String imageKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}

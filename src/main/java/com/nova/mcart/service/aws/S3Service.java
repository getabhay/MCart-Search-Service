//package com.nova.mcart.service.aws;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
//import software.amazon.awssdk.core.ResponseInputStream;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectResponse;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;
//import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
//import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
//
//import java.io.InputStream;
//import java.time.Duration;
//@Service
//public class S3Service {
//
//    private final S3Client s3Client;
//    private final S3Presigner s3Presigner;
//
//    @Value("${spring.cloud.aws.s3.bucket-name}")
//    private String bucketName;
//
//    public S3Service(@Value("${spring.cloud.aws.region}") String region,
//                     S3Presigner s3Presigner) {
//        this.s3Client = S3Client.builder()
//                .region(Region.of(region))
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//        this.s3Presigner = s3Presigner;
//    }
//
//    public String uploadFile(String key,
//                             InputStream inputStream,
//                             long contentLength,
//                             String contentType) {
//        return uploadFile(key, inputStream, contentLength, contentType, null);
//    }
//
//    public String uploadFile(String key,
//                             InputStream inputStream,
//                             long contentLength,
//                             String contentType,
//                             String bucketOverride) {
//
//        String targetBucket = (bucketOverride != null && !bucketOverride.isBlank())
//                ? bucketOverride
//                : bucketName;
//
//        PutObjectRequest request = PutObjectRequest.builder()
//                .bucket(targetBucket)
//                .key(key)
//                .contentType(contentType)
//                .contentLength(contentLength)
//                .build();
//
//        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
//        return key;
//    }
//
//    public InputStream download(String key) {
//        return download(key, null);
//    }
//
//    public InputStream download(String key, String bucketOverride) {
//
//        String targetBucket = (bucketOverride != null && !bucketOverride.isBlank())
//                ? bucketOverride
//                : bucketName;
//
//        GetObjectRequest request = GetObjectRequest.builder()
//                .bucket(targetBucket)
//                .key(key)
//                .build();
//
//        return s3Client.getObject(request);
//    }
//
//    public String generatePresignedUploadUrl(String key,
//                                             String contentType,
//                                             Duration expiry) {
//
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .contentType(contentType)
//                .build();
//
//        PutObjectPresignRequest presignRequest =
//                PutObjectPresignRequest.builder()
//                        .signatureDuration(expiry)
//                        .putObjectRequest(putObjectRequest)
//                        .build();
//
//        PresignedPutObjectRequest presignedRequest =
//                s3Presigner.presignPutObject(presignRequest);
//
//        return presignedRequest.url().toString();
//    }
//}

package com.nova.mcart.service.aws;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String defaultBucket;

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
       return uploadFile(key, inputStream, contentLength, contentType, null);
    }

    /**
     * Upload using S3Template (Handles RequestBody internally)
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType, String bucketOverride) {
        String targetBucket = getBucket(bucketOverride);

        // Metadata is optional but can be set like this:
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        s3Template.upload(targetBucket, key, inputStream, metadata);
        return key;
    }

    /**
     * Download returns an S3Resource which is a standard Spring Resource
     */
    public InputStream download(String key, String bucketOverride) throws IOException {
        return s3Template.download(getBucket(bucketOverride), key).getInputStream();
    }

    public InputStream download(String key) throws IOException {
        return s3Template.download(getBucket(null), key).getInputStream();
    }

    /**
     * Pre-signing is now a one-liner
     */
    public String generatePresignedUploadUrl(String key, String contentType, Duration expiry) {
        // createSignedPutURL is built into S3Template
        return s3Template.createSignedPutURL(defaultBucket, key, expiry).toString();
    }

    private String getBucket(String override) {
        return (override != null && !override.isBlank()) ? override : defaultBucket;
    }
}
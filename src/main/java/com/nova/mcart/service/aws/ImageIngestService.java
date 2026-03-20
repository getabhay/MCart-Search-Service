package com.nova.mcart.service.aws;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageIngestService {

    private final S3Service s3Service;
    private final HttpClient httpClient;
    @Value("${spring.cloud.aws.s3.image-bucket-name}")
    private String bucketName;

    public ImageIngestService(S3Service s3Service) {
        this.s3Service = s3Service;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String ingestToS3(String url, String keyPrefix) {

        try {
            URI uri = URI.create(url);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() / 100 != 2) {
                throw new IllegalArgumentException("Failed to download image. status=" + response.statusCode());
            }

            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalArgumentException("Empty image content");
            }

            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse("image/jpeg");

            String ext = resolveExtension(contentType, uri.getPath());

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;

            String key = keyPrefix;
            if (!key.endsWith("/")) {
                key = key + "/";
            }
            key = key + filename;

            s3Service.uploadFile(
                    key,
                    new ByteArrayInputStream(bytes),
                    bytes.length,
                    contentType,
                    bucketName
            );

            return key;

        } catch (Exception ex) {
            throw new IllegalArgumentException("Image ingest failed for url=" + url + " reason=" + ex.getMessage(), ex);
        }
    }

    private String resolveExtension(String contentType, String path) {

        String ct = contentType.toLowerCase(Locale.ROOT);

        if (ct.contains("png")) return ".png";
        if (ct.contains("webp")) return ".webp";
        if (ct.contains("gif")) return ".gif";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";

        // fallback: try from path
        String p = (path == null) ? "" : path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".png")) return ".png";
        if (p.endsWith(".webp")) return ".webp";
        if (p.endsWith(".gif")) return ".gif";
        if (p.endsWith(".jpeg")) return ".jpg";
        if (p.endsWith(".jpg")) return ".jpg";

        return ".jpg";
    }
}

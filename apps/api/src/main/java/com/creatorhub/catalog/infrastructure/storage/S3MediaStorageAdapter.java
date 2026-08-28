package com.creatorhub.catalog.infrastructure.storage;

import com.creatorhub.catalog.application.port.out.DigitalAssetStoragePort;
import com.creatorhub.catalog.application.port.out.MediaStoragePort;
import com.creatorhub.catalog.domain.model.MediaUploadRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** MinIO/S3 存储适配器，负责真实预签名和对象存在性检查，不向业务层泄露 SDK 类型。 */
@Component
public class S3MediaStorageAdapter implements MediaStoragePort, DigitalAssetStoragePort {

  private final S3Presigner presigner;
  private final S3Client client;
  private final String bucket;

  public S3MediaStorageAdapter(
      S3Presigner presigner,
      @Value("${creatorhub.storage.endpoint}") String endpoint,
      @Value("${creatorhub.storage.region}") String region,
      @Value("${creatorhub.storage.access-key}") String accessKey,
      @Value("${creatorhub.storage.secret-key}") String secretKey,
      @Value("${creatorhub.storage.bucket}") String bucket) {
    this.presigner = presigner;
    this.bucket = bucket;
    this.client =
        S3Client.builder()
            .endpointOverride(java.net.URI.create(endpoint))
            .region(software.amazon.awssdk.regions.Region.of(region))
            .credentialsProvider(
                software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                        accessKey, secretKey)))
            .serviceConfiguration(
                software.amazon.awssdk.services.s3.S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build())
            .build();
  }

  @Override
  public MediaStoragePort.PresignedUpload presign(
      UUID mediaId, String objectKey, MediaUploadRequest request, Instant expiresAt) {
    PutObjectRequest putObject =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(request.contentType())
            .contentLength(request.sizeBytes())
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.between(Instant.now(), expiresAt))
            .putObjectRequest(putObject)
            .build();
    PresignedPutObjectRequest signed = presigner.presignPutObject(presignRequest);
    return new MediaStoragePort.PresignedUpload(
        signed.url().toString(), "PUT", request.contentType(), expiresAt);
  }

  @Override
  public boolean exists(String objectKey) {
    try {
      client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
      return true;
    } catch (NoSuchKeyException ex) {
      return false;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  @Override
  public String presignGet(String objectKey, String contentType, Instant expiresAt) {
    GetObjectRequest getObject =
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .responseContentType(contentType)
            .build();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.between(Instant.now(), expiresAt))
            .getObjectRequest(getObject)
            .build();
    PresignedGetObjectRequest signed = presigner.presignGetObject(presignRequest);
    return signed.url().toString();
  }

  @Override
  public DigitalAssetStoragePort.PresignedUpload presignUpload(
      String objectKey, String contentType, long sizeBytes, Instant expiresAt) {
    PutObjectRequest putObject =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.between(Instant.now(), expiresAt))
            .putObjectRequest(putObject)
            .build();
    PresignedPutObjectRequest signed = presigner.presignPutObject(presignRequest);
    return new DigitalAssetStoragePort.PresignedUpload(
        signed.url().toString(), "PUT", contentType, expiresAt);
  }
}

package com.creatorhub.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** S3/MinIO 客户端配置；凭据只从环境变量读取，不进入领域层或日志。 */
@Configuration
public class StorageConfig {

  @Bean(destroyMethod = "close")
  S3Presigner s3Presigner(
      @Value("${creatorhub.storage.endpoint}") String endpoint,
      @Value("${creatorhub.storage.region}") String region,
      @Value("${creatorhub.storage.access-key}") String accessKey,
      @Value("${creatorhub.storage.secret-key}") String secretKey) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}

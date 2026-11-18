package com.project.nyang.global.common.S3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * 이미지 저장소 설정파일입니다.
 * 프로젝트 종료 후 AWS S3에서 Supabase Storage(S3 호환 API)로 교체했습니다.
 * AWS SDK v1은 접속 주소의 경로(/storage/v1/s3)를 무시해서 서명이 맞지 않아 SDK v2로 바꿨습니다.
 *
 * @author : 선순주
 * @fileName : S3Config
 * @since : 2025-07-10
 */
@Slf4j
@Configuration
public class S3Config {
    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.region}")
    private String region;

    @Value("${storage.access-key}")
    private String accessKey;

    @Value("${storage.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                // Supabase는 path-style 주소(/버킷/파일명)만 지원
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}

package com.project.nyang.global.config.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트와 연동하기 위해 필요한 cors 설정에 대한 클래스입니다.
 *
 * @author : 오승훈
 * @fileName : WebConfig
 * @since : 2025-07-14
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 프로젝트 종료 후 리팩터링: 허용 출처를 설정(app.cors.allowed-origins)으로 분리
    // allowCredentials(true)일 때는 브라우저 규칙상 "*"를 쓸 수 없으므로 출처를 하나씩 적어야 함
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedHeaders("*")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowCredentials(true); // 로그인 세션 유지 필요시
    }
}
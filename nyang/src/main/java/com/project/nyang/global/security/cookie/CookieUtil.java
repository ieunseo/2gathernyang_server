package com.project.nyang.global.security.cookie;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 인증 쿠키(accessToken, refreshToken) 발급과 삭제를 한 곳에서 처리합니다.
 * 프로젝트 종료 후 리팩터링: 발급(secure false)과 탈퇴 시 삭제(secure true) 속성이 달라
 * localhost가 아닌 HTTP 환경에서 브라우저가 삭제 쿠키를 거부하던 문제를 고쳤습니다.
 * 발급과 삭제가 항상 같은 Path, Secure, SameSite, HttpOnly를 쓰도록 이 클래스로만 쿠키를 만듭니다.
 *
 * @fileName : CookieUtil
 * @since : 2026-11-18
 */
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";

    // 로컬, HTTP 배포: false / HTTPS 배포: true
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    // 로컬, HTTP 배포: Lax / HTTPS에서 프론트와 백엔드가 다른 사이트: None
    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @PostConstruct
    void validate() {
        // 브라우저는 Secure 없는 SameSite=None 쿠키를 거부하므로 설정 실수를 기동 시점에 막는다
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException("app.cookie.same-site=None 은 app.cookie.secure=true 와 같이 써야 합니다.");
        }
    }

    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        add(response, build(ACCESS_TOKEN, accessToken).build());
        add(response, build(REFRESH_TOKEN, refreshToken).build());
    }

    public void deleteTokenCookies(HttpServletResponse response) {
        add(response, build(ACCESS_TOKEN, "").maxAge(0).build());
        add(response, build(REFRESH_TOKEN, "").maxAge(0).build());
    }

    private ResponseCookie.ResponseCookieBuilder build(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }

    private void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

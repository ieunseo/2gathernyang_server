package com.project.nyang.global.security.oauth2;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.project.nyang.global.security.cookie.CookieUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    //로그인 동작을 커스텀으로 구현하고 싶을 때 사용하는 인터페이스

    private final CookieUtil cookieUtil;

    // 로그인 성공 후 이동할 주소. 운영 기본값은 프론트, 로컬은 APP_LOGIN_REDIRECT_URL로 덮어쓴다
    @Value("${app.login-redirect-url:http://2gaenyang.site/oauth2/redirect}")
    private String loginRedirectUrl;

    //OAuth2 로그인 성공시 호출되는 메서드
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {


        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String accessToken = (String) attributes.get("accessToken");
        String refreshToken = (String) attributes.get("refreshToken");
        String name = (String) attributes.get("name");

        System.out.println("[OAuth2_LOG]" + "소셜 로그인 시도한 이름 = "+name);

        // 사용자 ID를 안전하게 꺼내기 (null 체크 및 타입 캐스팅)
        Long id = null;
        Object idObj = attributes.get("id");
        if (idObj != null) {
            // Long 타입이 아닐 수도 있으니 안전하게 변환
            id = Long.valueOf(idObj.toString());
        }


        //토큰 전달방식: HttpOnly 쿠키 (속성은 CookieUtil에서 설정 값으로 통일)
        cookieUtil.addTokenCookies(response, accessToken, refreshToken);

        response.sendRedirect(loginRedirectUrl + "?token=" + accessToken);

    }
}
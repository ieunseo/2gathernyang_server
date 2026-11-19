package com.project.nyang.global.security.cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 발급 쿠키와 삭제 쿠키가 같은 속성(Path, Secure, SameSite, HttpOnly)을 쓰는지 검증합니다.
 * 속성이 다르면 브라우저가 삭제 쿠키를 거부해 로그아웃, 탈퇴 후에도 토큰이 남습니다.
 */
class CookieUtilTest {

    private CookieUtil cookieUtil(boolean secure, String sameSite) {
        CookieUtil util = new CookieUtil();
        ReflectionTestUtils.setField(util, "secure", secure);
        ReflectionTestUtils.setField(util, "sameSite", sameSite);
        return util;
    }

    // "accessToken=값; Path=/; ..." 에서 이름=값과 Max-Age, Expires를 뺀 나머지 속성
    private String attributes(String setCookie) {
        return String.join("; ", List.of(setCookie.split("; ")).stream()
                .skip(1)
                .filter(a -> !a.startsWith("Max-Age") && !a.startsWith("Expires"))
                .toList());
    }

    @Test
    void 발급과_삭제는_같은_속성을_쓴다_HTTP() {
        CookieUtil util = cookieUtil(false, "Lax");
        MockHttpServletResponse issued = new MockHttpServletResponse();
        MockHttpServletResponse deleted = new MockHttpServletResponse();

        util.addTokenCookies(issued, "AAA", "RRR");
        util.deleteTokenCookies(deleted);

        List<String> issuedCookies = issued.getHeaders("Set-Cookie");
        List<String> deletedCookies = deleted.getHeaders("Set-Cookie");
        assertThat(issuedCookies).hasSize(2);
        assertThat(deletedCookies).hasSize(2);
        for (int i = 0; i < 2; i++) {
            assertThat(attributes(deletedCookies.get(i))).isEqualTo(attributes(issuedCookies.get(i)));
            assertThat(deletedCookies.get(i)).contains("Max-Age=0").doesNotContain("Secure");
        }
        assertThat(issuedCookies.get(0)).startsWith("accessToken=AAA").contains("HttpOnly", "SameSite=Lax", "Path=/");
    }

    @Test
    void HTTPS_설정이면_발급과_삭제_모두_Secure() {
        CookieUtil util = cookieUtil(true, "None");
        MockHttpServletResponse issued = new MockHttpServletResponse();
        MockHttpServletResponse deleted = new MockHttpServletResponse();

        util.addTokenCookies(issued, "AAA", "RRR");
        util.deleteTokenCookies(deleted);

        assertThat(issued.getHeaders("Set-Cookie")).allMatch(c -> c.contains("Secure") && c.contains("SameSite=None"));
        assertThat(deleted.getHeaders("Set-Cookie")).allMatch(c -> c.contains("Secure") && c.contains("SameSite=None"));
    }

    @Test
    void SameSite_None은_Secure_없이_쓸_수_없다() {
        CookieUtil util = cookieUtil(false, "None");
        assertThatThrownBy(util::validate).isInstanceOf(IllegalStateException.class);
    }
}

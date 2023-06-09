package codekoi.apiserver.domain.auth.controller;

import codekoi.apiserver.domain.auth.service.UserTokenCommand;
import codekoi.apiserver.domain.auth.service.UserTokenQuery;
import codekoi.apiserver.domain.user.dto.UserToken;
import codekoi.apiserver.domain.user.service.UserQuery;
import codekoi.apiserver.global.token.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import static codekoi.apiserver.domain.auth.domain.UserToken.REFRESH_TOKEN_VALID_DURATION;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserQuery userQuery;

    private final UserTokenQuery userTokenQuery;
    private final UserTokenCommand userTokenCommand;

    private final JwtTokenProvider jwtTokenProvider;

    //todo: oauth로 추후 전환하기.
    @PostMapping("/login")
    public void login(@RequestParam String email, HttpServletResponse response) {
        final UserToken userToken = userQuery.getUserAuth(email);
        createAccessTokenToResponse(response, userToken);

        final String refreshToken = jwtTokenProvider.createRefreshToken();
        userTokenCommand.createUserToken(userToken.getUserId(), refreshToken);
        setRefreshTokenInResponse(refreshToken, REFRESH_TOKEN_VALID_DURATION, response);
    }

    @PostMapping("/login/refresh")
    public void refresh(@CookieValue(value = "refreshToken") Cookie cookie,
                        HttpServletResponse response
    ) {
        final String refreshToken = cookie.getValue();
        jwtTokenProvider.validateExpiredRefreshToken(refreshToken);

        final UserToken userToken = jwtTokenProvider.parseExpirableAccessToken(cookie.getValue());
        final Long userId = userToken.getUserId();
        userTokenQuery.validateUserRefreshToken(userId, refreshToken);

        createAccessTokenToResponse(response, userToken);
        setRefreshTokenInResponse(refreshToken, REFRESH_TOKEN_VALID_DURATION, response);
    }

    @PostMapping("/logout")
    public void logout(@CookieValue(value = "refreshToken") Cookie cookie,
                       HttpServletResponse response) {
        final String refreshToken = cookie.getValue();
        userTokenCommand.deleteUserToken(refreshToken);

        setRefreshTokenInResponse("", 0, response);
    }

    private void createAccessTokenToResponse(HttpServletResponse response, UserToken userToken) {
        final String accessToken = jwtTokenProvider.createAccessToken(userToken);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private void setRefreshTokenInResponse(String refreshToken, int maxAge, HttpServletResponse response) {
        final ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .maxAge(maxAge)
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .secure(true)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}

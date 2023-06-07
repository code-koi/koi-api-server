package codekoi.apiserver.domain.auth.controller;

import codekoi.apiserver.global.token.JwtTokenProvider;
import codekoi.apiserver.domain.auth.service.UserTokenCommand;
import codekoi.apiserver.domain.auth.service.UserTokenQuery;
import codekoi.apiserver.domain.user.dto.UserAuth;
import codekoi.apiserver.domain.user.service.UserQuery;
import codekoi.apiserver.global.token.AuthenticationPrincipal;
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
        final UserAuth userAuth = userQuery.getUserAuth(email);
        setAccessTokenInResponse(response, userAuth);

        final String newRefreshToken = jwtTokenProvider.createRefreshToken();
        userTokenCommand.createUserToken(userAuth.getUserId(), newRefreshToken);
        setRefreshTokenInResponse(newRefreshToken, REFRESH_TOKEN_VALID_DURATION, response);
    }

    @PostMapping("/login/refresh")
    public void refresh(@CookieValue(value = "refreshToken") Cookie cookie,
                        @AuthenticationPrincipal UserAuth userAuth,
                        HttpServletResponse response
    ) {
        final String refreshToken = cookie.getValue();
        jwtTokenProvider.validateRefreshToken(refreshToken);

        final Long userId = userAuth.getUserId();
        userTokenQuery.validateUserRefreshToken(userId, refreshToken);

        setAccessTokenInResponse(response, userAuth);
        setRefreshTokenInResponse(refreshToken, REFRESH_TOKEN_VALID_DURATION, response);
    }

    @PostMapping("/logout")
    public void logout(@CookieValue(value = "refreshToken") Cookie cookie,
                       HttpServletResponse response) {
        final String refreshToken = cookie.getValue();
        userTokenCommand.deleteUserToken(refreshToken);

        setRefreshTokenInResponse("", 0, response);
    }

    private void setAccessTokenInResponse(HttpServletResponse response, UserAuth userAuth) {
        final String accessToken = jwtTokenProvider.createAccessToken(userAuth);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private void setRefreshTokenInResponse(String refreshToken, int maxAge, HttpServletResponse response) {
        final ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .maxAge(maxAge)
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
package com.cryptotracker.auth;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
}

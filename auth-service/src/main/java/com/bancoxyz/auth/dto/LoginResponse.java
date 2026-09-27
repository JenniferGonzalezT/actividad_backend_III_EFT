package com.bancoxyz.auth.dto;

public record LoginResponse(String token, String tokenType, long expiresInMs) {
}

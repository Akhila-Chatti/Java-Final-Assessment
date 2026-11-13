package com.smartdesk.gateway.dto;

public record TokenResponse(String accessToken, String tokenType, UserResponse user) {}

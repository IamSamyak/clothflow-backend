package com.clothflow.user.security;

import java.util.List;

public record JwksResponse(
        List<JwkResponse> keys
) {
}
package com.clothflow.user.security;

public record JwkResponse(
        String kty,
        String kid,
        String use,
        String alg,
        String n,
        String e
) {
}
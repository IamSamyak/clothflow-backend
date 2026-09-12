package com.clothflow.user.security;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

@Service
public class JwksService {

    private final JwtKeyProvider keyProvider;

    public JwksService(
            JwtKeyProvider keyProvider
    ) {
        this.keyProvider = keyProvider;
    }

    public JwksResponse getJwks() {

        List<JwkResponse> keys =
                keyProvider.getPublicKeys()
                        .entrySet()
                        .stream()
                        .map(entry ->
                                toJwk(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .toList();

        return new JwksResponse(keys);
    }

    private JwkResponse toJwk(
            String keyId,
            PublicKey publicKey
    ) {

        if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
            throw new IllegalStateException(
                    "JWT public key must be RSA"
            );
        }

        return new JwkResponse(
                "RSA",
                keyId,
                "sig",
                "RS256",
                encode(
                        rsaPublicKey.getModulus()
                ),
                encode(
                        rsaPublicKey.getPublicExponent()
                )
        );
    }

    private String encode(BigInteger value) {

        byte[] bytes =
                value.toByteArray();

        if (bytes.length > 1 &&
                bytes[0] == 0) {

            bytes = java.util.Arrays.copyOfRange(
                    bytes,
                    1,
                    bytes.length
            );
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
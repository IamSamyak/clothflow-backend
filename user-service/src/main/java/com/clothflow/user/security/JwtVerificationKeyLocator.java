package com.clothflow.user.security;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtVerificationKeyLocator
        implements Locator<Key> {

    private static final String EXPECTED_ALGORITHM =
            Jwts.SIG.RS256.getId();

    private final JwtKeyProvider keyProvider;

    public JwtVerificationKeyLocator(
            JwtKeyProvider keyProvider
    ) {
        this.keyProvider = keyProvider;
    }

    @Override
    public Key locate(
            Header header
    ) {

        validateAlgorithm(header);

        Object keyIdValue =
                header.get("kid");

        if (!(keyIdValue instanceof String keyId)
                || keyId.isBlank()) {

            throw new IllegalArgumentException(
                    "JWT key ID is missing"
            );
        }

        return keyProvider.getPublicKey(
                keyId
        );
    }

    private void validateAlgorithm(
            Header header
    ) {

        if (!(header instanceof JwsHeader)) {

            throw new IllegalArgumentException(
                    "JWT must be a signed JWS"
            );
        }

        String algorithm =
                header.getAlgorithm();

        if (!EXPECTED_ALGORITHM.equals(
                algorithm
        )) {

            throw new IllegalArgumentException(
                    "Unsupported JWT signing algorithm: "
                            + algorithm
            );
        }
    }
}
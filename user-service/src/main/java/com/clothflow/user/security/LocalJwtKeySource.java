package com.clothflow.user.security;

import com.clothflow.user.config.JwtKeyProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "jwt.key-source",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalJwtKeySource implements JwtKeySource {

    private final JwtKeyProperties properties;

    public LocalJwtKeySource(
            JwtKeyProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    public JwtKeyProperties load() {
        return properties;
    }
}
package com.clothflow.user.security;

import com.clothflow.user.config.JwtKeyProperties;

/**
 * Source of JWT signing and verification key configuration.
 *
 * The rest of the security layer should not care where the
 * keys came from.
 *
 * Possible implementations:
 *
 * LocalJwtKeySource
 *      -> application.yml / environment variables
 *
 * AwsSecretsManagerJwtKeySource
 *      -> AWS Secrets Manager
 *
 * This abstraction allows us to move from local configuration
 * to shared production key storage without changing JwtService.
 */
public interface JwtKeySource {

    JwtKeyProperties load();
}
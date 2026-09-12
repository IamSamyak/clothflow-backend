package com.clothflow.user.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public interface JwtKeyProvider {

    String getCurrentKeyId();

    PrivateKey getCurrentPrivateKey();

    PublicKey getPublicKey(String keyId);

    Map<String, PublicKey> getPublicKeys();
}
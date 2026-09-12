package com.clothflow.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jwt")
public class JwtKeyProperties {

    private String currentKeyId;

    private List<KeyProperties> keys =
            new ArrayList<>();


    private String keySource;

    private String keySecretId;

    public String getCurrentKeyId() {
        return currentKeyId;
    }

    public void setCurrentKeyId(
            String currentKeyId
    ) {
        this.currentKeyId = currentKeyId;
    }

    public List<KeyProperties> getKeys() {
        return keys;
    }

    public void setKeys(
            List<KeyProperties> keys
    ) {
        this.keys = keys;
    }

    public String getKeySecretId() {
        return keySecretId;
    }

    public String getKeySource() {
        return keySource;
    }

    public void setKeySource(String keySource) {
        this.keySource = keySource;
    }

    public void setKeySecretId(String keySecretId) {
        this.keySecretId = keySecretId;
    }

    public static class KeyProperties {

        private String keyId;

        private String privateKey;

        private String publicKey;

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(
                String keyId
        ) {
            this.keyId = keyId;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(
                String privateKey
        ) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(
                String publicKey
        ) {
            this.publicKey = publicKey;
        }
    }
}
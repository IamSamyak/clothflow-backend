package com.clothflow.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.hsts")
public class HstsProperties {

    private boolean enabled;

    private long maxAgeSeconds;

    private boolean includeSubdomains;

    private boolean preload;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public boolean isIncludeSubdomains() {
        return includeSubdomains;
    }

    public void setIncludeSubdomains(
            boolean includeSubdomains
    ) {
        this.includeSubdomains = includeSubdomains;
    }

    public boolean isPreload() {
        return preload;
    }

    public void setPreload(boolean preload) {
        this.preload = preload;
    }
}

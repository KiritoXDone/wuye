package com.wuye.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private final Crypto crypto = new Crypto();

    public Crypto getCrypto() {
        return crypto;
    }

    public static class Crypto {
        private String configEncryptionKey = "";

        public String getConfigEncryptionKey() {
            return configEncryptionKey;
        }

        public void setConfigEncryptionKey(String configEncryptionKey) {
            this.configEncryptionKey = configEncryptionKey;
        }
    }
}

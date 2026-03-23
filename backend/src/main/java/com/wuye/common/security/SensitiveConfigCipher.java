package com.wuye.common.security;

import com.wuye.common.config.AppSecurityProperties;
import com.wuye.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SensitiveConfigCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final AppSecurityProperties appSecurityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveConfigCipher(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(resolveKeyBytes(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new BusinessException("CONFIG_ENCRYPT_FAILED", "敏感配置加密失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return "";
        }
        if (!storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedValue.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new BusinessException("CONFIG_DECRYPT_FAILED", "敏感配置解密失败", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(resolveKeyBytes(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new BusinessException("CONFIG_DECRYPT_FAILED", "敏感配置解密失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] resolveKeyBytes() {
        String key = appSecurityProperties.getCrypto().getConfigEncryptionKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException("CONFIG_KEY_MISSING", "敏感配置加密密钥未配置", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new BusinessException("CONFIG_KEY_INVALID", "敏感配置加密密钥不可用", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

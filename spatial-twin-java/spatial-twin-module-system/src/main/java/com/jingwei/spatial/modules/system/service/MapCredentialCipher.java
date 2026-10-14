package com.jingwei.spatial.modules.system.service;

import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 使用部署环境主密钥加密地图供应商凭证，避免凭证明文落库。
 */
@Component
public class MapCredentialCipher {

    private static final String CIPHER_PREFIX = "v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MapCredentialCipher(
            @Value("${jingwei.map-config.encryption-key:}") String masterKey
    ) {
        this.masterKey = masterKey == null ? "" : masterKey.trim();
    }

    /**
     * 判断当前环境是否已提供地图凭证加密主密钥。
     *
     * @return 已配置返回true
     */
    public boolean isReady() {
        return masterKey.length() >= 16;
    }

    /**
     * 加密一项地图供应商凭证。
     *
     * @param plaintext 凭证明文
     * @return 带版本前缀的密文
     */
    public String encrypt(String plaintext) {
        requireReady();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "地图凭证加密失败");
        }
    }

    /**
     * 解密一项地图供应商凭证。
     *
     * @param ciphertext 带版本前缀的密文
     * @return 凭证明文
     */
    public String decrypt(String ciphertext) {
        requireReady();
        if (ciphertext == null || !ciphertext.startsWith(CIPHER_PREFIX)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "地图凭证密文格式不正确");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(CIPHER_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH) {
                throw new GeneralSecurityException("密文长度不足");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "地图凭证解密失败");
        }
    }

    private SecretKeySpec keySpec() throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(masterKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private void requireReady() {
        if (!isReady()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "请先通过环境变量 JW_MAP_CONFIG_ENCRYPTION_KEY 配置地图凭证加密主密钥"
            );
        }
    }
}

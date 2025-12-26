package com.pd.im.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 加密解密工具类
 * <p>
 * 提供基于 AES-256-GCM 算法的安全加解密功能。
 * 包含两层安全机制：
 * 1. 基础加密：使用指定密钥进行 AES-256-GCM 加密。
 * 2. 密钥封装 (Key Wrapping)：使用内部硬编码的 Master Secret 保护每个应用的独立密钥。
 * <p>
 * 安全设计：
 * - 随机 IV (12字节)
 * - GCM 认证标签 (128位)
 * - PBKDF2 密钥派生 (65536次迭代)
 *
 * @author Parker
 * @date 12/24/25
 */
@Slf4j
public class EncryptionUtil {

    // AES-GCM 算法配置
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128; // GCM 认证标签长度 (位)
    private static final int GCM_IV_LENGTH = 12;   // GCM 推荐 IV 长度 (字节)

    // PBKDF2 密钥派生配置 (用于 Master Secret 派生)
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536; // 迭代次数，增加破解成本
    private static final int PBKDF2_KEY_LENGTH = 256;   // 派生密钥长度 (位)
    private static final String MASTER_KEY_SALT = "MASTER_KEY_SALT"; // Master Key 派生固定盐值

    /**
     * 内部硬编码的 Master Secret (Key Encryption Key - KEK)
     * <p>
     * 用于保护存储在数据库中的 per-app keys (DEK)。
     * 注意：此密钥直接硬编码在代码中，作为一种本地混淆机制。
     * 对于更高安全级别的场景，应考虑使用 KMS (Key Management Service) 或 HSM。
     */
    private static final String INTERNAL_MASTER_SECRET = "PD_IM_LOCAL_SECURITY_CONFUSION_KEY_2025";


    /**
     * 生成随机 AES-256 密钥 (Data Encryption Key - DEK)
     *
     * @return Base64 编码的密钥字符串
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate AES key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * 封装应用密钥 (Wrap Key)
     * <p>
     * 使用内部 Master Secret 对应用生成的随机密钥进行加密保护。
     *
     * @param appKey 原始应用密钥 (DEK)
     * @return 封装后的应用密钥 (Wrapped Key)
     */
    public static String encodeKey(String appKey) {
        // 使用内部 Master Secret 进行加密封装
        return encryptWithMaster(appKey, INTERNAL_MASTER_SECRET);
    }

    /**
     * 解封装应用密钥 (Unwrap Key)
     * <p>
     * 使用内部 Master Secret 还原出原始的应用密钥。
     *
     * @param encodedAppKey 封装后的应用密钥 (Wrapped Key)
     * @return 原始应用密钥 (DEK)
     */
    public static String decodeKey(String encodedAppKey) {
        return decryptWithMaster(encodedAppKey, INTERNAL_MASTER_SECRET);
    }

    /**
     * 使用指定密钥加密数据 (通用加密方法)
     *
     * @param plainText 明文数据
     * @param secretKey Base64编码的密钥
     * @return Base64编码的加密数据 (格式: [IV 12bytes] + [Ciphertext])
     */
    public static String encrypt(String plainText, String secretKey) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 1. 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            // 2. 生成随机 IV (每个消息必须唯一)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 3. 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

            // 4. 执行加密
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 5. 拼接 IV 和密文 (便于解密时提取 IV)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            // 6. 返回 Base64 编码结果
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 使用指定密钥解密数据 (通用解密方法)
     *
     * @param encryptedText Base64编码的加密数据 (包含 IV)
     * @param secretKey     Base64编码的密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String secretKey) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 1. 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

            // 2. 解码加密数据
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            // 3. 提取 IV 和 密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 4. 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            // 5. 执行解密
            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // =================================================================================
    // 内部私有方法
    // =================================================================================

    /**
     * 使用 Master Secret 派生密钥进行加密
     */
    private static String encryptWithMaster(String data, String masterSecret) {
        try {
            String derivedMasterKey = deriveKey(masterSecret, MASTER_KEY_SALT);
            return encrypt(data, derivedMasterKey);
        } catch (Exception e) {
            throw new RuntimeException("Master key encryption failed", e);
        }
    }

    /**
     * 使用 Master Secret 派生密钥进行解密
     */
    private static String decryptWithMaster(String data, String masterSecret) {
         try {
            String derivedMasterKey = deriveKey(masterSecret, MASTER_KEY_SALT);
            return decrypt(data, derivedMasterKey);
        } catch (Exception e) {
            throw new RuntimeException("Master key decryption failed", e);
        }
    }

    /**
     * PBKDF2 密钥派生函数
     */
    private static String deriveKey(String password, String salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_LENGTH
        );
        SecretKey secretKey = factory.generateSecret(spec);
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}

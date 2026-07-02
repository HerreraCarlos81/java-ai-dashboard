package com.aiusage.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {
    private static final Path KEY_DIR = Paths.get(
        System.getProperty("user.home"), ".ai-usage-dashboard"
    );
    private static final String KEY_FILE = ".keystore";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public EncryptionUtil() {
        this.secretKey = loadOrCreateKey();
    }

    private static final String PREFIX = "ENC:";

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            String data = encrypted.startsWith(PREFIX) ? encrypted.substring(PREFIX.length()) : encrypted;
            byte[] combined = Base64.getDecoder().decode(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private SecretKey loadOrCreateKey() {
        try {
            KEY_DIR.toFile().mkdirs();
            File keyFile = KEY_DIR.resolve(KEY_FILE).toFile();
            if (keyFile.exists()) {
                byte[] encoded = Files.readAllBytes(keyFile.toPath());
                return new SecretKeySpec(encoded, ALGORITHM);
            }
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
            kg.init(256);
            SecretKey key = kg.generateKey();
            Files.write(keyFile.toPath(), key.getEncoded());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load/create encryption key", e);
        }
    }

    public static boolean keyFileExists() {
        return KEY_DIR.resolve(KEY_FILE).toFile().exists();
    }
}

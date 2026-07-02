package com.aiusage.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    @Test
    void testEncryptDecryptRoundtrip() {
        EncryptionUtil util = new EncryptionUtil();
        String[] testCases = {"sk-test-key-12345", "sk-ant-longer-test-key-value-here", "a"};
        for (String original : testCases) {
            String encrypted = util.encrypt(original);
            assertTrue(EncryptionUtil.isEncrypted(encrypted), "Should have ENC: prefix");
            assertNotEquals(original, encrypted, "Encrypted value should differ from original");
            String decrypted = util.decrypt(encrypted);
            assertEquals(original, decrypted);
        }
    }

    @Test
    void testDecryptWithAndWithoutPrefix() {
        EncryptionUtil util = new EncryptionUtil();
        String original = "sk-test-decrypt";
        String encrypted = util.encrypt(original);
        String withoutPrefix = encrypted.substring(4);
        assertEquals(original, util.decrypt(withoutPrefix));
    }

    @Test
    void testDifferentKeysProduceDifferentCiphertexts() {
        EncryptionUtil util = new EncryptionUtil();
        String key = "same-value";
        String e1 = util.encrypt(key);
        String e2 = util.encrypt(key);
        assertNotEquals(e1, e2, "Same plaintext should produce different ciphertexts (random IV)");
        assertEquals(key, util.decrypt(e1));
        assertEquals(key, util.decrypt(e2));
    }
}

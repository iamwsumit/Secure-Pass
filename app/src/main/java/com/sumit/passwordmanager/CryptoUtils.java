package com.sumit.passwordmanager;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CryptoUtils {

    private static final String KEY_ALIAS = "biometricEncryptionKey";
    private static final int KEY_SIZE = 256;
    private static final String ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;

    static void generateSecretKey() throws Exception {
        KeyGenParameterSpec keyGenParams = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setUserAuthenticationRequired(false)
                .setKeySize(KEY_SIZE)
                .build();
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(keyGenParams);
        keyGenerator.generateKey();
    }

    protected static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");

        keyStore.load(null);
        SecretKey key = ((SecretKey) keyStore.getKey(KEY_ALIAS, null));
        if (key != null)
            return key;
        else
            generateSecretKey();
        return getSecretKey();

    }

    protected static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(ENCRYPTION_ALGORITHM + "/"
                + ENCRYPTION_BLOCK_MODE + "/"
                + ENCRYPTION_PADDING);
    }
}
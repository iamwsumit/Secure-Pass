package com.sumit.passwordmanager;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private final static String TAG = "PasswordManager";
    private final Context context;

    public Utils(Context context) {
        this.context = context;
    }

    protected static String firstCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    protected static String formatMillis(long millis) {
        long current = System.currentTimeMillis();
        long diff = current - millis;
        String format;
        if (diff < 60000)
            return "Just now";
        else if (diff < 3600000)
            format = (diff / 60000) + " minutes ago";
        else if (diff < 86400000)
            format = (diff / 3600000) + " hours ago";
        else {
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            dateFormat.applyPattern("dd MMM yyyy, hh:mm a");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            return dateFormat.format(calendar.getTime());
        }
        return format;
    }

    protected static void setTitle(AppCompatActivity activity, String title) {
        Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(activity.getSupportActionBar()).setTitle(title);
    }

    protected static void setStatusBarColor(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.MAIN);
    }

    public static void hideKeyBoard(Activity activity) {
        View view = activity.getCurrentFocus();
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void toggleTheme(Context context) {
        LocalStorage.writeString(context, "theme", isDarkTheme(context) ? "l" : "d");
        AppCompatDelegate.setDefaultNightMode(!isDarkTheme(context)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES
        );
    }

    public static boolean isDarkTheme(Context context) {
        return LocalStorage.getString(context, "theme").equals("d");
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeBase64(String string) {
        if (string.isEmpty())
            return string;
        return Base64.encodeToString(string.getBytes(), 0);
    }

    public static String encodeBase64(byte[] arr){
        return Base64.encodeToString(arr, 0);
    }

    public static String decodeBase64(String hash) {
        if (hash.isEmpty())
            return hash;
        return new String(Base64.decode(hash, 0));
    }

    public static byte[] decodeBase64Bytes(String hash){
        return Base64.decode(hash, 0);
    }

    protected static void setKey(String key) {
        Utils.key = key;
    }

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    // Method to generate a random string of the given length
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALPHABET.length());
            result.append(ALPHABET.charAt(randomIndex));
        }

        return result.toString();
    }

    private static String key = "";

    protected static String encrypt(String data, boolean ivG) {
        try {
            if (key.isEmpty())
                return "";
            String key = Utils.key.substring(0, 16);
            byte[] keyBytes = key.getBytes("UTF-8");
            String iv = ivG ? generateRandomString(16) : Utils.key.substring(16);
            byte[] ivBytes = iv.getBytes("UTF-8");

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
            return iv + Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "encrypt2: ", e);
            return "";
        }
    }

    protected static String decrypt(String hash) {
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(hash.substring(0, 16).getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(key.substring(0, 16).getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = Base64.decode(hash.substring(16), Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "decrypt: ", e);
            return "";
        }
    }

    public void share(String message) {
        try {
            final Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.TEXT", message);
            context.startActivity(Intent.createChooser(intent, "Share Using..."));
        } catch (Exception e) {
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    public static void logInfo(String info) {
        Log.i(TAG, info);
    }

    protected static boolean isBiometricAvailable(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public interface KeyGeneration {
        void Success(String key);

        void Failed();
    }

    public static class Color {
        public static final int DISABLED = -1283675409;
        public static final int MAIN = -8606993;
    }
}
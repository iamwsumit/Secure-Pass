package com.sumit.passwordmanager;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Authentication extends AppCompatActivity {
    private TextInputEditText pass;


    private TextView biometric;
    private MaterialCardView button;
    private TextInputLayout layoutF;

    private final View.OnClickListener listener = view -> {
        Utils.hideKeyBoard(this);
        Utils.setKey(Utils.hash(getPassword()));
        String enc = Utils.encrypt(getPassword(), false); // false only for encrypting password hash
        if (enc.trim().equals(LocalStorage.getString(this, "key").trim())) {
            startActivity(new Intent(this, Home.class));
            finish();
        } else {
            layoutF.setError("Invalid Password");
            layoutF.setErrorEnabled(true);
        }
    };

    private void enableButton() {
        button.setCardBackgroundColor(Utils.Color.MAIN);
        button.setRippleColor(ColorStateList.valueOf(Utils.Color.DISABLED));
        button.setOnClickListener(listener);
    }

    private void disableButton() {
        button.setCardBackgroundColor(Utils.Color.DISABLED);
        button.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setOnClickListener(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(!Utils.isDarkTheme(this)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES
        );
        Utils.setStatusBarColor(this);
        if (LocalStorage.getString(this, "key").isEmpty()) {
            startActivity(new Intent(this, Registration.class));
            finish();
        } else {
            setContentView(R.layout.authentication);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            this.pass = findViewById(R.id.pass);
            this.button = findViewById(R.id.button);
            this.layoutF = findViewById(R.id.inputLayoutF);
            if (Utils.isBiometricAvailable(this) &&
                    !LocalStorage.getString(this, "biometric").isEmpty()) {
                this.biometric = findViewById(R.id.biometric);
                this.biometric.setVisibility(View.VISIBLE);

                initializeBiometric();
            }

            final TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    checkWatcher();
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            };

            pass.addTextChangedListener(watcher);
        }
    }

    private void initializeBiometric() {
        this.biometric.setVisibility(View.VISIBLE);
        try {
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setConfirmationRequired(false)
                    .setTitle("Authentication")
                    .setSubtitle("Authenticate with fingerprint")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            Cipher cipher = CryptoUtils.getCipher();
            byte[] iv = Utils.decodeBase64Bytes(LocalStorage.getString(Authentication.this, "iv"));
            SecretKey secretKey = CryptoUtils.getSecretKey();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            BiometricPrompt prompt = new BiometricPrompt(this, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull @NotNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    try {
                        String biometric = LocalStorage.getString(Authentication.this, "biometric");
                        if (result.getCryptoObject() != null) {
                            String passwordHash = new String(cipher.doFinal(Utils.decodeBase64Bytes(biometric)));
                            Utils.setKey(passwordHash);
                            startActivity(new Intent(Authentication.this, Home.class));
                            finish();
                        } else {
                            Toast.makeText(Authentication.this, "Biometric is not configured", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            this.biometric.setOnClickListener((view -> {
                prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
            }));
            this.biometric.performClick();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void proceed() {
        startActivity(new Intent(Authentication.this, Home.class));
        finish();
    }

    private void checkWatcher() {
        if (getPassword().length() > 7) {
            layoutF.setErrorEnabled(false);
            enableButton();
        } else {
            disableButton();
            layoutF.setErrorEnabled(true);
            layoutF.setError("Min password length is 8");
            if (getPassword().isEmpty())
                layoutF.setErrorEnabled(false);
        }
    }

    private String getPassword() {
        return pass.getText().toString();
    }
}
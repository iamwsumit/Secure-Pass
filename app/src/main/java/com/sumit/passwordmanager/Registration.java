package com.sumit.passwordmanager;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.security.auth.login.LoginException;

public class Registration extends AppCompatActivity {

    private TextInputEditText cPass;
    private TextInputEditText pass;
    private MaterialCardView button;
    private TextInputLayout layout;
    private TextInputLayout layoutF;

    private void enableButton() {
        button.setCardBackgroundColor(Utils.Color.MAIN);
        button.setRippleColor(ColorStateList.valueOf(Utils.Color.DISABLED));
        button.setOnClickListener(view -> {
            final String hash = Utils.hash(getPassword());
            if (cryptoObject != null) {
                // save the encrypted password in biometric tag
                try {
                    String biometric = Utils.encodeBase64(cryptoObject.getCipher().doFinal(hash.getBytes()));
                    LocalStorage.writeString(this, "biometric", biometric);
                    LocalStorage.writeString(this, "iv", Utils.encodeBase64(cryptoObject.getCipher().getIV()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Utils.hideKeyBoard(this);
            Utils.setKey(hash);
            String enc = Utils.encrypt(getPassword(), false);
            LocalStorage.writeString(this, "key", enc.trim());
            startActivity(new Intent(this, Home.class));
            finish();
        });
    }

    private void disableButton() {
        button.setCardBackgroundColor(Utils.Color.DISABLED);
        button.setRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setOnClickListener(null);
    }

    private CheckBox biometric;
    private BiometricPrompt.CryptoObject cryptoObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Utils.setStatusBarColor(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.pass = findViewById(R.id.pass);
        this.cPass = findViewById(R.id.cPass);
        this.button = findViewById(R.id.button);
        this.layout = findViewById(R.id.inputLayout);
        this.layoutF = findViewById(R.id.inputLayoutF);
        this.biometric = findViewById(R.id.biometric);
        Utils.logInfo("Biometric Available : " + Utils.isBiometricAvailable(this));
        if (!Utils.isBiometricAvailable(this))
            this.biometric.setVisibility(View.GONE);
        else {
            this.biometric.setOnCheckedChangeListener((compoundButton, b) -> {
                if (b) {
                    initializeBiometric();
                } else {
                    cryptoObject = null;
                }
            });
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

        cPass.addTextChangedListener(watcher);
        pass.addTextChangedListener(watcher);
    }

    private void initializeBiometric() {
        if (cryptoObject != null)
            return;
        try {
            Cipher cipher = CryptoUtils.getCipher();
            SecretKey secretKey = CryptoUtils.getSecretKey();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setConfirmationRequired(false)
                    .setTitle("Authentication")
                    .setSubtitle("Authenticate with fingerprint")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            BiometricPrompt prompt = new BiometricPrompt(this, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull @NotNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    try {
                        cryptoObject = result.getCryptoObject();
                        biometric.setChecked(cryptoObject != null);
                        Toast.makeText(Registration.this, "Biometric Configured, please create your password", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        biometric.setChecked(false);
                        Toast.makeText(Registration.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        Log.e("PasswordManager", "onAuthenticationSucceeded: ", e);
                    }
                }
            });
            prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
        } catch (Exception e) {
            e.printStackTrace();
            cryptoObject = null;
            this.biometric.setChecked(false);
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkWatcher() {
        if (getPassword().length() > 7) {
            layoutF.setErrorEnabled(false);
            if (getConfirmPassword().isEmpty())
                return;
            if (getPassword().equals(getConfirmPassword())) {
                enableButton();
                layout.setErrorEnabled(false);
            } else {
                disableButton();
                layout.setErrorEnabled(true);
                layout.setError("Confirm password does not match");
            }
        } else {
            disableButton();
            layoutF.setErrorEnabled(true);
            layoutF.setError("Min password length is 8");
        }
    }

    private String getPassword() {
        return pass.getText().toString();
    }

    private String getConfirmPassword() {
        return cPass.getText().toString();
    }
}
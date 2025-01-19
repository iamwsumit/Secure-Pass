package com.sumit.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

public class Password extends AppCompatActivity {

    static Model model;

    @Override
    protected void onDestroy() {
        model = null;
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Utils.setStatusBarColor(this);
        setContentView(R.layout.activity_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(view -> finish());
        if (model == null) toolbar.getMenu().clear();
        else {
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.share) {
                    new Utils(Password.this).share(model.toString());
                }
                return true;
            });
        }

        this.titleLayout = findViewById(R.id.titleLayout);
        this.passwordLayout = findViewById(R.id.passwordLayout);
        this.usernameLayout = findViewById(R.id.usernameLayout);

        this.titleField = findViewById(R.id.title);
        this.usernameField = findViewById(R.id.username);
        this.passwordField = findViewById(R.id.password);
        this.noteField = findViewById(R.id.note);

        if (getIntent() != null) passwordField.setText(getIntent().getStringExtra("generated_pass"));

        if (model != null) {
            toolbar.setTitle(model.getTitle());
            titleField.setText(model.getTitle());
            usernameField.setText(model.getUsername());
            passwordField.setText(model.getContent());
            noteField.setText(model.getComment());
            findViewById(R.id.copy).setVisibility(View.VISIBLE);
            findViewById(R.id.copy).setOnClickListener(view -> copyPasswordText());
        } else toolbar.setTitle("Add Password");

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                titleLayout.setErrorEnabled(false);
                passwordLayout.setErrorEnabled(false);
                usernameLayout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        this.titleField.addTextChangedListener(textWatcher);
        this.passwordField.addTextChangedListener(textWatcher);
        this.usernameField.addTextChangedListener(textWatcher);

        findViewById(R.id.button).setOnClickListener(view -> check());
    }

    void copyPasswordText() {
        String passwordText = passwordField.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("password", passwordText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Password copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private TextInputLayout titleLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout usernameLayout;
    private EditText titleField;
    private EditText usernameField;
    private EditText passwordField;
    private EditText noteField;

    void check() {
        String title = titleField.getText().toString().trim();
        if (title.length() < 5) {
            titleLayout.setError("Title must contain at least 5 characters");
            return;
        }

        String username = usernameField.getText().toString().trim();
        if (username.length() < 5) {
            usernameLayout.setError("Username must contain at least 5 characters");
            return;
        }

        String password = passwordField.getText().toString().trim();
        if (password.length() < 6) {
            passwordLayout.setError("Password must contain at least 6 characters");
            return;
        }

        String note = noteField.getText().toString().trim();

        DatabaseHelper db = new DatabaseHelper(this);

        final Model model = new Model(
                Password.model != null ? Password.model.getCreatedTime() : System.currentTimeMillis(),
                System.currentTimeMillis(),
                title,
                password,
                note,
                username
        );
        db.addRow(model, new DatabaseHelper.Listener() {
            @Override
            public void onSuccess() {
                db.close();
                Toast.makeText(Password.this, "Password Saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed() {
                db.close();
                Toast.makeText(Password.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
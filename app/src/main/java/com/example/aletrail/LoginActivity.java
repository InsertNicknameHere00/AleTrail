package com.example.aletrail;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.models.Session;
import io.appwrite.models.User;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button authButton;
    private TextView toggleAuthMode;
    private TextView errorText;
    private TextView skipLogin;
    private ProgressBar loadingProgress;

    private AppwriteService appwriteService;
    private Database database;
    private ExecutorService executor;

    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appwriteService = AppwriteService.getInstance(this);
        database = Database.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Ако вече е влязъл (вкл. guest), пращаме към main.
        if (appwriteService.isLoggedInLocally()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // Скриваме системната лента за по-чист екран.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat ic = androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        ic.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        initViews();
        setupListeners();
    }

    private void initViews() {
        nameInputLayout = findViewById(R.id.nameInputLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        authButton = findViewById(R.id.authButton);
        toggleAuthMode = findViewById(R.id.toggleAuthMode);
        errorText = findViewById(R.id.errorText);
        skipLogin = findViewById(R.id.skipLogin);
        loadingProgress = findViewById(R.id.loadingProgress);
    }

    private void setupListeners() {
        authButton.setOnClickListener(v -> {
            if (isSignUpMode) {
                performSignUp();
            } else {
                performSignIn();
            }
        });

        toggleAuthMode.setOnClickListener(v -> toggleMode());

        skipLogin.setOnClickListener(v -> continueAsGuest());

        // Регистрация на бизнес е достъпна и преди логин.
        findViewById(R.id.registerBusinessButton).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateBusinessActivity.class));
        });
    }

    private void toggleMode() {
        isSignUpMode = !isSignUpMode;
        if (isSignUpMode) {
            nameInputLayout.setVisibility(View.VISIBLE);
            authButton.setText(R.string.btn_sign_up);
            toggleAuthMode.setText(R.string.toggle_to_sign_in);
        } else {
            nameInputLayout.setVisibility(View.GONE);
            authButton.setText(R.string.btn_sign_in);
            toggleAuthMode.setText(R.string.toggle_to_sign_up);
        }
        hideError();
    }

    private void performSignIn() {
        String email = getText(emailInput);
        String password = getText(passwordInput);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.login_error_empty_fields));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.login_error_invalid_email));
            return;
        }

        setLoading(true);
        hideError();

        appwriteService.login(email, password, new AppwriteService.AuthCallback<Session>() {
            @Override
            public void onSuccess(Session result) {
                // Ensure a UserEntity exists in Room
                String userId = appwriteService.getSavedUserId();
                String userName = appwriteService.getSavedUserName();
                ensureRoomUser(userId, email, userName, "email");

                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void performSignUp() {
        String name = getText(nameInput);
        String email = getText(emailInput);
        String password = getText(passwordInput);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.login_error_empty_fields));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.login_error_invalid_email));
            return;
        }
        if (password.length() < 8) {
            showError(getString(R.string.login_error_short_password));
            return;
        }

        setLoading(true);
        hideError();

        String displayName = TextUtils.isEmpty(name) ? email.split("@")[0] : name;

        appwriteService.createAccount(email, password, displayName,
                new AppwriteService.AuthCallback<User<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(User<Map<String, Object>> user) {
                        // Auto-login after creating account
                        appwriteService.login(email, password, new AppwriteService.AuthCallback<Session>() {
                            @Override
                            public void onSuccess(Session result) {
                                ensureRoomUser(user.getId(), email, displayName, "email");
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(LoginActivity.this,
                                            R.string.signup_success, Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                });
                            }

                            @Override
                            public void onError(String message) {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    showError(message);
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError(message);
                        });
                    }
                });
    }

    private void continueAsGuest() {
        // Създаваме локален guest профил.
        String guestId = "guest_" + System.currentTimeMillis();
        ensureRoomUser(guestId, null, "Guest", "guest");

        // Пазим локално, че е логнат, за да отвори main.
        getSharedPreferences("aletrail_prefs", MODE_PRIVATE).edit()
                .putString("appwrite_user_id", guestId)
                .putString("appwrite_user_name", "Guest")
                .putBoolean("is_logged_in", true)
                .apply();

        navigateToMain();
    }

    // Гарантира, че има потребител в Room, после sync към Appwrite (без guest).
    private void ensureRoomUser(String userId, String email, String name, String provider) {
        executor.execute(() -> {
            UserEntity user = new UserEntity();
            user.setUserId(userId);
            user.setEmail(email);
            user.setDisplayName(name);
            user.setAuthProvider(provider);
            user.setCreatedAt(System.currentTimeMillis());
            database.userDAO().insert(user);

            // Качваме профила в облака само за реални профили.
            if (userId != null && !userId.startsWith("guest_")) {
                appwriteService.syncUserProfile(user, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("LoginActivity", "User profile synced to Appwrite DB for: " + userId);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("LoginActivity", "Failed to sync user profile to Appwrite DB: " + message);
                    }
                });
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        authButton.setEnabled(!loading);
        toggleAuthMode.setEnabled(!loading);
        skipLogin.setEnabled(!loading);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}

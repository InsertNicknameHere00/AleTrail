package com.example.aletrail;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView avatarInitial;
    private TextInputEditText profileNameInput;
    private TextView profileEmail;
    private TextView memberSince;
    private TextView profileStamps;
    private TextView profileVisits;
    private TextView profileBadges;
    private Button saveProfileButton;
    private Button signOutButton;
    private Button signInNowButton;
    private MaterialCardView profileCard;
    private MaterialCardView guestBanner;
    private ProgressBar profileLoading;

    private AppwriteService appwriteService;
    private Database database;
    private String currentUserId;
    private boolean isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        appwriteService = AppwriteService.getInstance(this);
        database = Database.getInstance(this);
        currentUserId = appwriteService.getSavedUserId();
        isGuest = currentUserId != null && currentUserId.startsWith("guest_");

        initViews();
        setupListeners();
        loadProfile();
        loadStats();
    }

    private void initViews() {
        avatarInitial = findViewById(R.id.avatarInitial);
        profileNameInput = findViewById(R.id.profileNameInput);
        profileEmail = findViewById(R.id.profileEmail);
        memberSince = findViewById(R.id.memberSince);
        profileStamps = findViewById(R.id.profileStamps);
        profileVisits = findViewById(R.id.profileVisits);
        profileBadges = findViewById(R.id.profileBadges);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        signOutButton = findViewById(R.id.signOutButton);
        signInNowButton = findViewById(R.id.signInNowButton);
        profileCard = findViewById(R.id.profileCard);
        guestBanner = findViewById(R.id.guestBanner);
        profileLoading = findViewById(R.id.profileLoading);

        if (isGuest) {
            guestBanner.setVisibility(View.VISIBLE);
            saveProfileButton.setVisibility(View.GONE);
            profileNameInput.setEnabled(false);
        }
    }

    private void setupListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        saveProfileButton.setOnClickListener(v -> saveProfile());

        signOutButton.setOnClickListener(v -> confirmSignOut());

        signInNowButton.setOnClickListener(v -> {
            // Clear guest session and go to login
            clearSessionAndGoToLogin();
        });
    }

    private void loadProfile() {
        // Load from shared prefs first (instant)
        String savedName = appwriteService.getSavedUserName();
        String savedEmail = appwriteService.getSavedUserEmail();

        if (savedName != null) {
            profileNameInput.setText(savedName);
            setAvatarInitial(savedName);
        }
        if (savedEmail != null) {
            profileEmail.setText(savedEmail);
        } else if (isGuest) {
            profileEmail.setText(R.string.profile_guest_mode);
        }

        // Load from Room for member since date
        if (currentUserId != null) {
            LiveData<UserEntity> userLiveData = database.userDAO().getUserById(currentUserId);
            userLiveData.observe(this, user -> {
                if (user != null) {
                    if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                        profileNameInput.setText(user.getDisplayName());
                        setAvatarInitial(user.getDisplayName());
                    }
                    if (user.getEmail() != null) {
                        profileEmail.setText(user.getEmail());
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    String dateStr = sdf.format(new Date(user.getCreatedAt()));
                    memberSince.setText(getString(R.string.profile_member_since, dateStr));
                }
            });
        }

        // If not guest, also refresh from Appwrite
        if (!isGuest) {
            appwriteService.getCurrentUser(new AppwriteService.AuthCallback<User<Map<String, Object>>>() {
                @Override
                public void onSuccess(User<Map<String, Object>> user) {
                    runOnUiThread(() -> {
                        profileNameInput.setText(user.getName());
                        profileEmail.setText(user.getEmail());
                        setAvatarInitial(user.getName());
                    });
                }

                @Override
                public void onError(String message) {
                    // Use cached data, already loaded above
                }
            });
        }
    }

    private void loadStats() {
        if (currentUserId == null) return;

        LiveData<UserEntity> userLiveData = database.userDAO().getUserById(currentUserId);
        userLiveData.observe(this, user -> {
            if (user != null) {
                profileStamps.setText(String.valueOf(user.getTotalStamps()));
                profileVisits.setText(String.valueOf(user.getTotalVisits()));
            }
        });

        LiveData<Integer> badgeCountLiveData = database.badgeDAO().getEarnedBadgeCount(currentUserId);
        badgeCountLiveData.observe(this, count -> {
            if (count != null) {
                profileBadges.setText(String.valueOf(count));
            }
        });
    }

    private void saveProfile() {
        String newName = profileNameInput.getText() != null
                ? profileNameInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, R.string.login_error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        profileLoading.setVisibility(View.VISIBLE);
        saveProfileButton.setEnabled(false);

        appwriteService.updateName(newName, new AppwriteService.AuthCallback<User<Map<String, Object>>>() {
            @Override
            public void onSuccess(User<Map<String, Object>> user) {
                // Also update Room
                new Thread(() -> {
                    UserEntity roomUser = new UserEntity();
                    roomUser.setUserId(currentUserId);
                    roomUser.setEmail(user.getEmail());
                    roomUser.setDisplayName(user.getName());
                    roomUser.setAuthProvider("email");
                    database.userDAO().insert(roomUser);
                }).start();

                runOnUiThread(() -> {
                    profileLoading.setVisibility(View.GONE);
                    saveProfileButton.setEnabled(true);
                    setAvatarInitial(newName);
                    Toast.makeText(ProfileActivity.this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    profileLoading.setVisibility(View.GONE);
                    saveProfileButton.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_sign_out_confirm_title)
                .setMessage(R.string.profile_sign_out_confirm_message)
                .setPositiveButton(R.string.btn_sign_out, (dialog, which) -> performSignOut())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void performSignOut() {
        profileLoading.setVisibility(View.VISIBLE);

        if (isGuest) {
            clearSessionAndGoToLogin();
            return;
        }

        appwriteService.logout(new AppwriteService.AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> clearSessionAndGoToLogin());
            }

            @Override
            public void onError(String message) {
                // Even on error, clear local session
                runOnUiThread(() -> clearSessionAndGoToLogin());
            }
        });
    }

    private void clearSessionAndGoToLogin() {
        // Clear session FIRST so LoginActivity doesn't see is_logged_in=true
        // and immediately redirect back to MainActivity
        appwriteService.clearSession();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setAvatarInitial(String name) {
        if (name != null && !name.isEmpty()) {
            avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase(Locale.ROOT));
        } else {
            avatarInitial.setText("?");
        }
    }
}


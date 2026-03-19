package com.example.aletrail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.appwrite.models.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView avatarInitial;
    private ImageView avatarImage;
    private ImageView avatarEditButton;
    private TextInputEditText profileNameInput;
    private TextView profileEmail;
    private TextView memberSince;
    private TextView profileStamps;
    private TextView profileVisits;
    private TextView profileBadges;
    private Button saveProfileButton;
    private Button signOutButton;
    private Button deleteAccountButton;
    private Button signInNowButton;
    private MaterialCardView profileCard;
    private MaterialCardView guestBanner;
    private ProgressBar profileLoading;

    private AppwriteService appwriteService;
    private Database database;
    private String currentUserId;
    private boolean isGuest;

    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Hide system bars for immersive experience
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat ic = androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        ic.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        appwriteService = AppwriteService.getInstance(this);
        database = Database.getInstance(this);
        currentUserId = appwriteService.getSavedUserId();
        isGuest = currentUserId != null && currentUserId.startsWith("guest_");

        registerImageLaunchers();
        initViews();
        setupListeners();
        loadProfile();
        loadStats();
    }

    private void registerImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Take persistable permission so the URI survives app restarts
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                Log.w("ProfileActivity", "Could not persist URI permission: " + e.getMessage());
                            }
                            saveProfileImage(imageUri.toString());
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        saveProfileImage(cameraImageUri.toString());
                    }
                }
        );
    }

    private void initViews() {
        avatarInitial = findViewById(R.id.avatarInitial);
        avatarImage = findViewById(R.id.avatarImage);
        avatarEditButton = findViewById(R.id.avatarEditButton);
        profileNameInput = findViewById(R.id.profileNameInput);
        profileEmail = findViewById(R.id.profileEmail);
        memberSince = findViewById(R.id.memberSince);
        profileStamps = findViewById(R.id.profileStamps);
        profileVisits = findViewById(R.id.profileVisits);
        profileBadges = findViewById(R.id.profileBadges);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        signOutButton = findViewById(R.id.signOutButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        signInNowButton = findViewById(R.id.signInNowButton);
        profileCard = findViewById(R.id.profileCard);
        guestBanner = findViewById(R.id.guestBanner);
        profileLoading = findViewById(R.id.profileLoading);

        // Theme toggle
        com.google.android.material.switchmaterial.SwitchMaterial darkModeSwitch = findViewById(R.id.darkModeSwitch);
        android.content.SharedPreferences prefs = getSharedPreferences("aletrail_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        darkModeSwitch.setChecked(isDark);
        darkModeSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    checked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Email verification
        android.widget.LinearLayout verifyEmailRow = findViewById(R.id.verifyEmailRow);
        android.widget.Button verifyEmailButton = findViewById(R.id.verifyEmailButton);
        android.widget.TextView verifyEmailStatus = findViewById(R.id.verifyEmailStatus);
        if (!isGuest) {
            verifyEmailRow.setVisibility(View.VISIBLE);
            // Check verification status via Appwrite
            new Thread(() -> {
                try {
                    boolean verified = appwriteService.isEmailVerified();
                    runOnUiThread(() -> {
                        if (verified) {
                            verifyEmailStatus.setText(R.string.profile_email_verified);
                            verifyEmailStatus.setTextColor(0xFF81C784);
                            verifyEmailButton.setVisibility(View.GONE);
                        } else {
                            verifyEmailStatus.setText(R.string.profile_email_not_verified);
                            verifyEmailStatus.setTextColor(0xFFFF8A80);
                            verifyEmailButton.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Error checking email verification: " + e.getMessage());
                }
            }).start();

            verifyEmailButton.setOnClickListener(v -> {
                verifyEmailButton.setEnabled(false);
                appwriteService.sendVerificationEmail(new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(ProfileActivity.this,
                                    R.string.profile_verify_email_sent, android.widget.Toast.LENGTH_SHORT).show();
                            verifyEmailButton.setEnabled(true);
                        });
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(ProfileActivity.this,
                                    getString(R.string.profile_verify_email_error, message),
                                    android.widget.Toast.LENGTH_LONG).show();
                            verifyEmailButton.setEnabled(true);
                        });
                    }
                });
            });
        }

        if (isGuest) {
            guestBanner.setVisibility(View.VISIBLE);
            saveProfileButton.setVisibility(View.GONE);
            deleteAccountButton.setVisibility(View.GONE);
            avatarEditButton.setVisibility(View.GONE);
            profileNameInput.setEnabled(false);
        }
    }

    private void setupListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        saveProfileButton.setOnClickListener(v -> saveProfile());

        signOutButton.setOnClickListener(v -> confirmSignOut());

        deleteAccountButton.setOnClickListener(v -> confirmDeleteAccount());

        avatarEditButton.setOnClickListener(v -> showPhotoChooser());

        signInNowButton.setOnClickListener(v -> {
            // Clear guest session and go to login
            clearSessionAndGoToLogin();
        });

        // My Reviews
        findViewById(R.id.myReviewsButton).setOnClickListener(v ->
                startActivity(new Intent(this, RatingsActivity.class)));

        // Create Business
        findViewById(R.id.createBusinessButton).setOnClickListener(v ->
                startActivity(new Intent(this, CreateBusinessActivity.class)));

        // Favorites Map
        findViewById(R.id.viewFavoritesMapButton).setOnClickListener(v ->
                createFavoritesMap());
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
                    // Load profile image if available
                    loadAvatarImage(user.getProfileImageUrl(), user.getDisplayName());

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
                // Also update Room - preserve existing data
                new Thread(() -> {
                    UserEntity roomUser = database.userDAO().getUserByIdSync(currentUserId);
                    if (roomUser == null) {
                        roomUser = new UserEntity();
                        roomUser.setUserId(currentUserId);
                    }
                    roomUser.setEmail(user.getEmail());
                    roomUser.setDisplayName(user.getName());
                    roomUser.setAuthProvider("email");
                    // profileImageUrl is preserved from the existing roomUser
                    database.userDAO().insert(roomUser);

                    // Also sync updated profile to Appwrite Database
                    appwriteService.syncUserProfile(roomUser, new AppwriteService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("ProfileActivity", "Profile synced to Appwrite DB");
                        }

                        @Override
                        public void onError(String message) {
                            Log.e("ProfileActivity", "Failed to sync profile to Appwrite DB: " + message);
                        }
                    });
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_delete_confirm_title)
                .setMessage(R.string.profile_delete_confirm_message)
                .setPositiveButton(R.string.btn_delete_account, (dialog, which) -> performDeleteAccount())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void performDeleteAccount() {
        profileLoading.setVisibility(View.VISIBLE);
        deleteAccountButton.setEnabled(false);
        signOutButton.setEnabled(false);
        saveProfileButton.setEnabled(false);

        final String userId = currentUserId;

        // Step 1: Delete all local Room data for this user on a background thread
        new Thread(() -> {
            try {
                database.userDAO().deleteByUserId(userId);
                database.loyaltyCardDAO().deleteByUserId(userId);
                database.badgeDAO().deleteByUserId(userId);
                database.visitDAO().deleteByUserId(userId);
                database.beerRatingDAO().deleteByUserId(userId);
                database.AleDAO().clearAllFavorites();
                Log.d("ProfileActivity", "All local Room data deleted for user: " + userId);
            } catch (Exception e) {
                Log.e("ProfileActivity", "Error deleting local data: " + e.getMessage());
            }

            // Step 2: Delete Appwrite data + session
            appwriteService.deleteAccount(new AppwriteService.AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this,
                                R.string.profile_delete_success, Toast.LENGTH_SHORT).show();
                        clearSessionAndGoToLogin();
                    });
                }

                @Override
                public void onError(String message) {
                    Log.e("ProfileActivity", "Appwrite account deletion error: " + message);
                    // Even on Appwrite error, local data is already gone — redirect to login
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this,
                                R.string.profile_delete_success, Toast.LENGTH_SHORT).show();
                        clearSessionAndGoToLogin();
                    });
                }
            });
        }).start();
    }

    private void setAvatarInitial(String name) {
        if (name != null && !name.isEmpty()) {
            avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase(Locale.ROOT));
        } else {
            avatarInitial.setText("?");
        }
    }

    /**
     * Loads a profile image into the avatar circle via Glide.
     * If imageUrl is null or empty, falls back to the initial letter.
     */
    private void loadAvatarImage(String imageUrl, String fallbackName) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            avatarImage.setVisibility(View.VISIBLE);
            avatarInitial.setVisibility(View.GONE);
            Glide.with(this)
                    .load(Uri.parse(imageUrl))
                    .transform(new CircleCrop())
                    .into(avatarImage);
        } else {
            avatarImage.setVisibility(View.GONE);
            avatarInitial.setVisibility(View.VISIBLE);
            setAvatarInitial(fallbackName);
        }
    }

    /**
     * Shows a chooser dialog: Take Photo / Choose from Gallery / Remove Photo.
     */
    private void showPhotoChooser() {
        String[] options = {
                getString(R.string.profile_photo_take),
                getString(R.string.profile_photo_gallery),
                getString(R.string.profile_photo_remove)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_photo_chooser_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            removeProfileImage();
                            break;
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Log.e("ProfileActivity", "Error creating image file: " + e.getMessage());
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "AleTrail_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        galleryLauncher.launch(intent);
    }

    /**
     * Saves the selected profile image URI to Room and syncs to Appwrite.
     */
    private void saveProfileImage(String imageUri) {
        loadAvatarImage(imageUri, appwriteService.getSavedUserName());

        new Thread(() -> {
            UserEntity user = database.userDAO().getUserByIdSync(currentUserId);
            if (user != null) {
                user.setProfileImageUrl(imageUri);
                database.userDAO().update(user);
                Log.d("ProfileActivity", "Profile image saved to Room: " + imageUri);

                // Sync to Appwrite
                if (!isGuest) {
                    appwriteService.syncUserProfile(user, new AppwriteService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("ProfileActivity", "Profile image synced to Appwrite");
                        }

                        @Override
                        public void onError(String message) {
                            Log.e("ProfileActivity", "Failed to sync profile image: " + message);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Removes the profile image and reverts to the initial letter avatar.
     */
    private void removeProfileImage() {
        avatarImage.setVisibility(View.GONE);
        avatarInitial.setVisibility(View.VISIBLE);
        setAvatarInitial(appwriteService.getSavedUserName());

        new Thread(() -> {
            UserEntity user = database.userDAO().getUserByIdSync(currentUserId);
            if (user != null) {
                user.setProfileImageUrl(null);
                database.userDAO().update(user);
                Log.d("ProfileActivity", "Profile image removed");

                if (!isGuest) {
                    appwriteService.syncUserProfile(user, new AppwriteService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("ProfileActivity", "Profile image removal synced to Appwrite");
                        }

                        @Override
                        public void onError(String message) {
                            Log.e("ProfileActivity", "Failed to sync image removal: " + message);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Creates a Cartes.io map from the user's favorite breweries.
     */
    private void createFavoritesMap() {
        Toast.makeText(this, R.string.map_creating, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.util.List<BreweryEntity> favorites = database.AleDAO().getFavoritesSync();
                if (favorites == null || favorites.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.map_empty, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Create map via Cartes API
                String userName = appwriteService.getSavedUserName();
                String mapTitle = (userName != null ? userName : "AleTrail") + "'s Favorites";
                CartesModels.CreateMapRequest req = new CartesModels.CreateMapRequest(
                        mapTitle, "My favorite breweries from AleTrail");

                retrofit2.Response<okhttp3.ResponseBody> mapResponse =
                        RetrofitClient.getCartesAPI().createMap(req).execute();

                if (!mapResponse.isSuccessful() || mapResponse.body() == null) {
                    String errBody;
                    try (okhttp3.ResponseBody errorBody = mapResponse.errorBody()) {
                        errBody = errorBody != null ? errorBody.string() : "Failed to create map";
                    }
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, errBody),
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                String rawBody;
                try (okhttp3.ResponseBody body = mapResponse.body()) {
                    rawBody = body != null ? body.string() : "";
                }

                String mapToken = null;
                String mapUuid = null;
                try {
                    com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(rawBody);
                    if (parsed.isJsonObject()) {
                        com.google.gson.JsonObject json = parsed.getAsJsonObject();
                        if (json.has("token") && !json.get("token").isJsonNull()) {
                            mapToken = json.get("token").getAsString();
                        }
                        if (json.has("uuid") && !json.get("uuid").isJsonNull()) {
                            mapUuid = json.get("uuid").getAsString();
                        }
                    } else if (parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()) {
                        String msg = parsed.getAsString();
                        final String finalMsg = msg;
                        runOnUiThread(() -> Toast.makeText(this,
                                getString(R.string.map_error, finalMsg),
                                Toast.LENGTH_SHORT).show());
                        return;
                    }
                } catch (Exception parseException) {
                    final String parseErr = rawBody != null && !rawBody.isEmpty() ? rawBody : parseException.getMessage();
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, parseErr),
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                if (mapToken == null || mapToken.trim().isEmpty() || mapUuid == null || mapUuid.trim().isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, "Map token/uuid missing in Cartes response"),
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                int addedCount = 0;

                // Add markers for each favorite
                for (BreweryEntity brewery : favorites) {
                    if (brewery.getLatitude() != null && brewery.getLongitude() != null) {
                        String desc = brewery.getName() != null ? brewery.getName() : "Brewery";
                        CartesModels.CreateMarkerRequest marker =
                                new CartesModels.CreateMarkerRequest(
                                        mapToken,
                                        brewery.getLatitude(),
                                        brewery.getLongitude(),
                                        "Brewery",
                                        desc);
                        try {
                            retrofit2.Response<okhttp3.ResponseBody> markerResp =
                                    RetrofitClient.getCartesAPI().createMarker(mapUuid, marker).execute();
                            if (markerResp.isSuccessful()) {
                                addedCount++;
                            } else {
                                String markerErr;
                                try (okhttp3.ResponseBody eb = markerResp.errorBody()) {
                                    markerErr = eb != null ? eb.string() : "Unknown marker error";
                                }
                                Log.e("ProfileActivity", "Failed to add marker: " + markerErr);
                            }
                        } catch (Exception e) {
                            Log.e("ProfileActivity", "Failed to add marker: " + e.getMessage());
                        }
                    }
                }

                final int count = addedCount;
                final String mapId = mapUuid;
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.map_created, count), Toast.LENGTH_SHORT).show();
                    // Open map in browser
                    String url = "https://app.cartes.io/maps/" + mapId;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                });

            } catch (Exception e) {
                Log.e("ProfileActivity", "Error creating favorites map: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.map_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

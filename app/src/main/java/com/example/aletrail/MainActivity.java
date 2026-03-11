package com.example.aletrail;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private String currentUserId;

    // UI Components
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private TextInputEditText searchEditText;
    private TextInputLayout searchInputLayout;
    private TextInputEditText stateFilterInput;
    private FloatingActionButton fabFindNearby;
    private FloatingActionButton fabScanQR;
    private Button applyFilterButton;
    private ExtendedFloatingActionButton fabAddCard;
    private FloatingActionButton fabScrollTop;
    private TextView totalStampsText;
    private TextView totalVisitsText;
    private TextView badgesEarnedText;
    private TextView statsUserName;
    private TextView statsAvatarInitial;
    private ImageView statsAvatarImage;
    private TextView statsDiscountText;
    private ChipGroup typeFilterChipGroup;

    // Adapters
    private BreweryAdapter breweryAdapter;
    private BreweryAdapter favoritesAdapter;
    private LoyaltyCardAdapter loyaltyCardAdapter;
    private BadgeAdapter badgeAdapter;

    // Services & Repositories
    private BreweryRepository breweryRepository;
    private LoyaltyCardRepository loyaltyCardRepository;
    private LocationService locationService;
    private GamificationService gamificationService;
    private SyncService syncService;
    private AppwriteService appwriteService;
    private Database database;

    // Current tab state
    private int currentTab = 0;

    // Filter state
    private String currentStateFilter = null;
    private String currentTypeFilter = null;

    // LiveData instances to avoid recreating observers
    private LiveData<List<BreweryEntity>> breweriesLiveData;
    private LiveData<List<BreweryEntity>> favoritesLiveData;
    private LiveData<List<LoyaltyCardEntity>> cardsLiveData;
    private LiveData<List<BadgeEntity>> badgesLiveData;

    // Search debounce
    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved theme preference
        android.content.SharedPreferences prefs = getSharedPreferences("aletrail_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        // Check auth — redirect to login if not authenticated
        appwriteService = AppwriteService.getInstance(this);
        if (!appwriteService.isLoggedInLocally()) {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }
        currentUserId = appwriteService.getSavedUserId();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Hide system bars for immersive experience
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.statusBars());
        insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setupToolbar();
        initializeServices();
        initializeViews();
        setupRecyclerView();
        setupTabs();
        setupButtons();
        setupSearch();
        requestPermissions();
        loadUserStats();
        loadBreweries();

        // Handle email verification deep link: aletrail://verify?userId=...&secret=...
        handleVerificationDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleVerificationDeepLink(intent);
    }

    private void handleVerificationDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        android.net.Uri data = intent.getData();
        if ("aletrail".equals(data.getScheme()) && "verify".equals(data.getHost())) {
            String userId = data.getQueryParameter("userId");
            String secret = data.getQueryParameter("secret");
            if (userId != null && secret != null) {
                Toast.makeText(this, R.string.toast_verifying_email, Toast.LENGTH_SHORT).show();
                appwriteService.completeEmailVerification(userId, secret, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, R.string.toast_email_verified, Toast.LENGTH_LONG).show()
                        );
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                getString(R.string.toast_email_verify_failed, message), Toast.LENGTH_LONG).show()
                        );
                    }
                });
            }
        }
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void initializeServices() {
        database = Database.getInstance(this);
        breweryRepository = new BreweryRepository(this);
        loyaltyCardRepository = new LoyaltyCardRepository(this);
        locationService = new LocationService(this);
        gamificationService = new GamificationService(this);
        syncService = new SyncService(this);

        // Start auto sync
        syncService.scheduleAutoSync(currentUserId);

        // Initialize badges for user
        gamificationService.initializeBadgesForUser(currentUserId);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabLayout = findViewById(R.id.tabLayout);
        searchEditText = findViewById(R.id.searchEditText);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        stateFilterInput = findViewById(R.id.stateFilterInput);
        fabFindNearby = findViewById(R.id.fabFindNearby);
        fabScanQR = findViewById(R.id.fabScanQR);
        applyFilterButton = findViewById(R.id.applyFilterButton);
        fabAddCard = findViewById(R.id.fabAddCard);
        fabScrollTop = findViewById(R.id.fabScrollTop);
        totalStampsText = findViewById(R.id.totalStampsText);
        totalVisitsText = findViewById(R.id.totalVisitsText);
        badgesEarnedText = findViewById(R.id.badgesEarnedText);
        statsUserName = findViewById(R.id.statsUserName);
        statsAvatarInitial = findViewById(R.id.statsAvatarInitial);
        statsAvatarImage = findViewById(R.id.statsAvatarImage);
        statsDiscountText = findViewById(R.id.statsDiscountText);
        typeFilterChipGroup = findViewById(R.id.typeFilterChipGroup);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup Brewery Adapter
        breweryAdapter = new BreweryAdapter(new BreweryAdapter.OnBreweryClickListener() {
            @Override
            public void onBreweryClick(BreweryEntity brewery) {
                openBreweryInMaps(brewery);
            }

            @Override
            public void onFavoriteClick(BreweryEntity brewery) {
                android.util.Log.d("MainActivity", "Favorite clicked for: " + brewery.getName());

                // Use repository callback to get the resulting favorite state
                breweryRepository.toggleFavorite(brewery, new BreweryRepository.ToggleFavoriteCallback() {
                    @Override
                    public void onComplete(boolean newState) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                newState ? getString(R.string.toast_added_to_favorites) : getString(R.string.toast_removed_from_favorites),
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onCreateCardClick(BreweryEntity brewery) {
                createLoyaltyCard(brewery);
            }

            @Override
            public void onBreweryLongPress(BreweryEntity brewery) {
                showBreweryStampQR(brewery);
            }

            @Override
            public void onRateClick(BreweryEntity brewery) {
                showRateBreweryDialog(brewery);
            }
        });

        // Setup Favorites Adapter (same listener)
        favoritesAdapter = new BreweryAdapter(new BreweryAdapter.OnBreweryClickListener() {
            @Override
            public void onBreweryClick(BreweryEntity brewery) {
                openBreweryInMaps(brewery);
            }

            @Override
            public void onFavoriteClick(BreweryEntity brewery) {
                // Show confirmation dialog before removing from favorites
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.dialog_remove_favorites_title)
                    .setMessage(getString(R.string.dialog_remove_favorites_message, brewery.getName()))
                    .setPositiveButton(R.string.dialog_remove, (dialog, which) -> {
                        breweryRepository.toggleFavorite(brewery, new BreweryRepository.ToggleFavoriteCallback() {
                            @Override
                            public void onComplete(boolean newState) {
                                runOnUiThread(() -> {
                                    if (!newState) {
                                        Toast.makeText(MainActivity.this, R.string.toast_removed_from_favorites, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, R.string.toast_added_to_favorites, Toast.LENGTH_SHORT).show();
                                    }
                                    loadFavorites();
                                });
                            }
                        });
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }

            @Override
            public void onCreateCardClick(BreweryEntity brewery) {
                createLoyaltyCard(brewery);
            }

            @Override
            public void onBreweryLongPress(BreweryEntity brewery) {
                showBreweryStampQR(brewery);
            }

            @Override
            public void onRateClick(BreweryEntity brewery) {
                showRateBreweryDialog(brewery);
            }
        }, true); // Pass true to show the remove button

        // Setup Loyalty Card Adapter
        loyaltyCardAdapter = new LoyaltyCardAdapter(new LoyaltyCardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(LoyaltyCardEntity card) {
                Toast.makeText(MainActivity.this, getString(R.string.toast_card_id, String.valueOf(card.getCardId())), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShareClick(LoyaltyCardEntity card) {
                shareCard(card);
            }

            @Override
            public void onHistoryClick(LoyaltyCardEntity card) {
                showVisitHistory(card);
            }

            @Override
            public void onDeleteClick(LoyaltyCardEntity card) {
                deleteCard(card);
            }
        });

        // Setup Badge Adapter
        badgeAdapter = new BadgeAdapter(badge -> {
            if (badge.isEarned() && badge.getEarnedTimestamp() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault());
                String dateStr = sdf.format(new java.util.Date(badge.getEarnedTimestamp()));
                Toast.makeText(MainActivity.this,
                        badge.getBadgeName() + "\n" + getString(R.string.badge_unlocked_on, dateStr),
                        Toast.LENGTH_LONG).show();
            } else if (badge.isEarned()) {
                Toast.makeText(MainActivity.this, badge.getBadgeName() + " ✓", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,
                        badge.getBadgeName() + " — " + badge.getBadgeDescription(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(breweryAdapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                switchTab(currentTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void switchTab(int position) {
        View filterCard = findViewById(R.id.filterCard);

        // Crossfade animation
        recyclerView.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            switch (position) {
                case 0: // Breweries
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(breweryAdapter);
                    loadBreweries();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.VISIBLE);
                    filterCard.setVisibility(View.VISIBLE);
                    break;
                case 1: // My Cards
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(loyaltyCardAdapter);
                    loadUserCards();
                    loadBestDiscount(); // Refresh discount display
                    fabAddCard.show();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    break;
                case 2: // Badges — grid layout
                    recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                    recyclerView.setAdapter(badgeAdapter);
                    loadBadges();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    break;
                case 3: // Favorites
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(favoritesAdapter);
                    loadFavorites();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    break;
            }
            recyclerView.animate().alpha(1f).setDuration(200).start();
        }).start();
    }

    private void setupButtons() {
        fabFindNearby.setOnClickListener(v -> findNearbyBreweries());
        fabScanQR.setOnClickListener(v -> scanQRCode());
        fabAddCard.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_select_brewery_for_card, Toast.LENGTH_SHORT).show();
            tabLayout.selectTab(tabLayout.getTabAt(0));
        });
        applyFilterButton.setOnClickListener(v -> applyFilters());

        // Scroll-to-top button
        fabScrollTop.setOnClickListener(v -> {
            recyclerView.scrollToPosition(0);
            androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) findViewById(R.id.recyclerView).getParent().getParent();
            scrollView.smoothScrollTo(0, 0);
            fabScrollTop.setVisibility(View.GONE);
        });

        // Show scroll-to-top when scrolled down
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && fabScrollTop.getVisibility() != View.VISIBLE) {
                    fabScrollTop.setVisibility(View.VISIBLE);
                } else if (!rv.canScrollVertically(-1)) {
                    fabScrollTop.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentTab == 0) {
                    // Remove previous callback
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    // Create new callback with delay (debounce)
                    searchRunnable = () -> {
                        if (s.length() > 0) {
                            searchBreweries(s.toString());
                        } else {
                            loadBreweries();
                        }
                    };

                    // Post delayed (500ms debounce)
                    searchHandler.postDelayed(searchRunnable, 500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBreweries() {
        // Remove any existing observers so we always attach fresh
        if (breweriesLiveData != null) {
            breweriesLiveData.removeObservers(this);
            breweriesLiveData = null;
        }

        breweriesLiveData = breweryRepository.getAllBreweries();
        breweriesLiveData.observe(this, breweries -> {
            if (breweries != null && !breweries.isEmpty()) {
                breweryAdapter.setBreweries(breweries);
            } else {
                // Fetch from API if local DB is empty
                breweryRepository.fetchBreweriesByCity("San Francisco", 20);
            }
        });
    }

    private void searchBreweries(String query) {
        // If query is empty, reset to full list
        if (query == null || query.trim().isEmpty()) {
            loadBreweries();
            return;
        }

        // Remove previous observers to avoid memory leaks
        if (breweriesLiveData != null) {
            breweriesLiveData.removeObservers(this);
        }

        breweriesLiveData = breweryRepository.searchBreweries(query);
        breweriesLiveData.observe(this, breweries -> {
            if (breweries != null) {
                breweryAdapter.setBreweries(breweries);
            }
        });
    }

    private void loadUserCards() {
        if (cardsLiveData == null) {
            cardsLiveData = loyaltyCardRepository.getUserCards(currentUserId);
            cardsLiveData.observe(this, cards -> {
                if (cards != null) {
                    loyaltyCardAdapter.setCards(cards);
                }
            });
        }
    }

    private void loadBadges() {
        if (badgesLiveData == null) {
            badgesLiveData = database.badgeDAO().getAllBadgesForUser(currentUserId);
            badgesLiveData.observe(this, badges -> {
                if (badges != null) {
                    badgeAdapter.setBadges(badges);
                }
            });
        }
    }

    private void loadFavorites() {
        android.util.Log.d("MainActivity", "loadFavorites() called");

        // Remove existing observer and re-subscribe so UI always reflects DB
        if (favoritesLiveData != null) {
            favoritesLiveData.removeObservers(this);
            favoritesLiveData = null;
        }

        favoritesLiveData = breweryRepository.getFavoriteBreweries();
        favoritesLiveData.observe(this, favorites -> {
            android.util.Log.d("MainActivity", "Favorites observer triggered. Count: " + (favorites != null ? favorites.size() : "null"));
            if (favorites != null) {
                for (BreweryEntity brewery : favorites) {
                    android.util.Log.d("MainActivity", "Favorite brewery: " + brewery.getName() + " (favorite=" + brewery.isFavorite() + ")");
                }
                favoritesAdapter.setBreweries(favorites);
            } else {
                favoritesAdapter.setBreweries(new java.util.ArrayList<>());
            }
        });
    }

    private void loadUserStats() {
        LiveData<UserEntity> userLiveData = database.userDAO().getUserById(currentUserId);
        userLiveData.observe(this, user -> {
            if (user != null) {
                totalStampsText.setText(String.valueOf(user.getTotalStamps()));
                totalVisitsText.setText(String.valueOf(user.getTotalVisits()));

                // Display profile name and avatar initial
                String name = user.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    statsUserName.setText(name);
                    statsAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                } else {
                    statsUserName.setText(R.string.stats_title);
                    statsAvatarInitial.setText("?");
                }
            }
        });

        LiveData<Integer> badgeCountLiveData = database.badgeDAO().getEarnedBadgeCount(currentUserId);
        badgeCountLiveData.observe(this, count -> {
            if (count != null) {
                badgesEarnedText.setText(String.valueOf(count));
            }
        });

        // Load best discount from all loyalty cards
        loadBestDiscount();
    }

    private void loadBestDiscount() {
        new Thread(() -> {
            try {
                java.util.List<LoyaltyCardEntity> cards = database.loyaltyCardDAO().getCardsForUserSync(currentUserId);
                if (cards == null || cards.isEmpty()) {
                    runOnUiThread(() -> {
                        statsDiscountText.setText(R.string.stats_discount_none);
                        statsDiscountText.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                int bestDiscount = 0;
                String bestBreweryId = null;
                for (LoyaltyCardEntity card : cards) {
                    int discount = LoyaltyCardAdapter.calculateDiscount(card.getStamps());
                    if (discount > bestDiscount) {
                        bestDiscount = discount;
                        bestBreweryId = card.getBreweryId();
                    }
                }

                if (bestDiscount > 0 && bestBreweryId != null) {
                    // Look up brewery name
                    BreweryEntity brewery = database.AleDAO().getAleByIdSync(bestBreweryId);
                    String breweryName = (brewery != null && brewery.getName() != null)
                            ? brewery.getName() : bestBreweryId;
                    final int finalDiscount = bestDiscount;
                    runOnUiThread(() -> {
                        statsDiscountText.setText(getString(R.string.stats_discount_best, finalDiscount, breweryName));
                        statsDiscountText.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> {
                        statsDiscountText.setText(R.string.stats_discount_none);
                        statsDiscountText.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error loading discount: " + e.getMessage());
            }
        }).start();
    }

    private void findNearbyBreweries() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        locationService.getCurrentLocation(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                breweryRepository.fetchBreweriesByLocation(lat, lon, 20);
                Toast.makeText(this, R.string.toast_searching_nearby, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_unable_get_location, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scanQRCode() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.prompt_scan_qr));
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);

        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher =
        registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String qrValue = result.getContents();
                processQRCode(qrValue);
            }
        });

    private void processQRCode(String qrValue) {
        // ── Brewery Stamp QR (ALETRAIL_STAMP:breweryId:token) ──
        if (QRCodeService.isValidStampQR(qrValue)) {
            String breweryId = QRCodeService.extractBreweryIdFromStampQR(qrValue);

            locationService.getCurrentLocation(location -> {
                double lat = location != null ? location.getLatitude() : 0.0;
                double lon = location != null ? location.getLongitude() : 0.0;

                loyaltyCardRepository.processStampFromQR(currentUserId, breweryId, lat, lon,
                    new LoyaltyCardRepository.StampResultCallback() {
                        @Override
                        public void onSuccess(String breweryName) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    getString(R.string.toast_stamp_added) + " " + breweryName,
                                    Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    message, Toast.LENGTH_LONG).show());
                        }
                    });
            });
        } else {
            Toast.makeText(this, R.string.toast_invalid_qr, Toast.LENGTH_SHORT).show();
        }
    }

    private void createLoyaltyCard(BreweryEntity brewery) {
        loyaltyCardRepository.createLoyaltyCard(currentUserId, brewery.getId(), 100, cardId -> {
            runOnUiThread(() -> {
                if (cardId == -1) {
                    Toast.makeText(this, R.string.toast_duplicate_card, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.toast_loyalty_card_created, Toast.LENGTH_SHORT).show();
                    tabLayout.selectTab(tabLayout.getTabAt(1)); // Switch to My Cards tab
                }
            });
        });
    }

    /**
     * Shows a brewery stamp QR code for customers to scan (long-press on brewery item).
     */
    private void showBreweryStampQR(BreweryEntity brewery) {
        String stampQR = QRCodeService.generateBreweryStampQR(brewery.getId());
        android.graphics.Bitmap qrBitmap = QRCodeService.generateQRCodeBitmap(stampQR, 512, 512);

        if (qrBitmap != null) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setImageBitmap(qrBitmap);
            imageView.setPadding(50, 50, 50, 50);

            new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.dialog_brewery_stamp_qr_title)
                .setMessage(getString(R.string.dialog_brewery_stamp_qr_message, brewery.getName()))
                .setView(imageView)
                .setPositiveButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
        } else {
            Toast.makeText(this, R.string.toast_qr_generation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a dialog to rate a brewery.
     */
    private void showRateBreweryDialog(BreweryEntity brewery) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate_brewery, null);
        android.widget.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        android.widget.EditText beerNameInput = dialogView.findViewById(R.id.beerNameInput);
        android.widget.EditText commentInput = dialogView.findViewById(R.id.commentInput);

        new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_rate_brewery_title) + " — " + brewery.getName())
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_submit, (dialog, which) -> {
                float rating = ratingBar.getRating();
                String beerName = beerNameInput.getText().toString().trim();
                String comment = commentInput.getText().toString().trim();

                if (rating == 0) {
                    Toast.makeText(this, R.string.toast_select_rating, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save rating
                new Thread(() -> {
                    BeerRatingEntity ratingEntity = new BeerRatingEntity();
                    ratingEntity.setUserId(currentUserId);
                    ratingEntity.setBreweryId(brewery.getId());
                    ratingEntity.setBeerName(beerName.isEmpty() ? null : beerName);
                    ratingEntity.setRating(rating);
                    ratingEntity.setComment(comment.isEmpty() ? null : comment);
                    database.beerRatingDAO().insert(ratingEntity);

                    // Sync to Appwrite
                    appwriteService.syncRating(ratingEntity, new AppwriteService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("MainActivity", "Rating synced to Appwrite");
                        }
                        @Override
                        public void onError(String message) {
                            android.util.Log.e("MainActivity", "Rating sync failed: " + message);
                        }
                    });

                    // Check rating badges
                    int totalRatings = database.beerRatingDAO().getTotalRatingCountSync(currentUserId);
                    gamificationService.checkRatingBadges(currentUserId, totalRatings);

                    runOnUiThread(() -> Toast.makeText(this, R.string.toast_rating_saved, Toast.LENGTH_SHORT).show());
                }).start();
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void deleteCard(LoyaltyCardEntity card) {
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_card_title)
            .setMessage(R.string.dialog_delete_card_message)
            .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                loyaltyCardRepository.deleteCard(card.getCardId(), () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_card_deleted, Toast.LENGTH_SHORT).show();
                        loadUserCards(); // Reload cards
                    });
                });
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, R.string.toast_permissions_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_permissions_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareCard(LoyaltyCardEntity card) {
        new Thread(() -> {
            String breweryName = "Brewery #" + card.getBreweryId();
            String location = "";
            try {
                BreweryEntity brewery = database.AleDAO().getAleByIdSync(card.getBreweryId());
                if (brewery != null) {
                    if (brewery.getName() != null) breweryName = brewery.getName();
                    StringBuilder loc = new StringBuilder();
                    if (brewery.getCity() != null && !brewery.getCity().isEmpty()) loc.append(brewery.getCity());
                    if (brewery.getState() != null && !brewery.getState().isEmpty()) {
                        if (loc.length() > 0) loc.append(", ");
                        loc.append(brewery.getState());
                    }
                    if (brewery.getCountry() != null && !brewery.getCountry().isEmpty()) {
                        if (loc.length() > 0) loc.append(", ");
                        loc.append(brewery.getCountry());
                    }
                    location = loc.toString();
                }
            } catch (Exception ignored) {}

            int discount = LoyaltyCardAdapter.calculateDiscount(card.getStamps());
            final String finalName = breweryName;
            final String finalLocation = location;

            runOnUiThread(() -> {
                StringBuilder shareText = new StringBuilder();
                shareText.append("Check out my loyalty card!\n\n");
                shareText.append("🍺 ").append(finalName).append("\n");
                shareText.append("📊 Stamps: ").append(card.getStamps()).append("/").append(card.getMaxStamps()).append("\n");
                if (discount > 0) {
                    shareText.append("🎁 Current Discount: ").append(discount).append("%\n");
                }
                if (!finalLocation.isEmpty()) {
                    shareText.append("📍 ").append(finalLocation).append("\n");
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_loyalty_card_subject));
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_loyalty_card_chooser)));
            });
        }).start();
    }

    private void showVisitHistory(LoyaltyCardEntity card) {
        Intent intent = new Intent(this, VisitHistoryActivity.class);
        intent.putExtra("cardId", card.getCardId());
        intent.putExtra("breweryId", card.getBreweryId());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void applyFilters() {
        String state = stateFilterInput.getText().toString().trim();
        String type = null;

        // Get selected chip text
        int selectedChipId = typeFilterChipGroup.getCheckedChipId();
        if (selectedChipId != -1 && selectedChipId != R.id.chipAll) {
            android.view.View selectedChip = typeFilterChipGroup.findViewById(selectedChipId);
            type = ((com.google.android.material.chip.Chip)selectedChip).getText().toString().toLowerCase();
        }

        // Update filter state
        currentStateFilter = state.isEmpty() ? null : state;
        currentTypeFilter = type;

        // Reset the LiveData observer when filters change
        if (breweriesLiveData != null) {
            breweriesLiveData.removeObservers(this);
            breweriesLiveData = null;
        }

        // Apply filters and fetch from API, then observe filtered results from database
        if (currentStateFilter != null && currentTypeFilter != null) {
            // Both state and type
            Toast.makeText(this, "Filtering by " + currentStateFilter + " and " + currentTypeFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByStateAndType(currentStateFilter, currentTypeFilter, 50);

            // Observe filtered results from database
            breweriesLiveData = breweryRepository.getBreweriesByStateAndType(currentStateFilter, currentTypeFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else if (currentStateFilter != null) {
            // Only state
            Toast.makeText(this, "Filtering by state: " + currentStateFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByState(currentStateFilter, 50);

            // Observe filtered results from database
            breweriesLiveData = breweryRepository.getBreweriesByState(currentStateFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else if (currentTypeFilter != null) {
            // Only type
            Toast.makeText(this, "Filtering by type: " + currentTypeFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByType(currentTypeFilter, 50);

            // Observe filtered results from database
            breweriesLiveData = breweryRepository.getBreweriesByType(currentTypeFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else {
            // No filters - reset to show all breweries from database
            Toast.makeText(this, "Showing all breweries", Toast.LENGTH_SHORT).show();
            loadBreweries();
        }
    }

    /**
     * Opens the brewery location in Google Maps
     */
    private void openBreweryInMaps(BreweryEntity brewery) {
        if (brewery == null) {
            Toast.makeText(this, R.string.toast_brewery_info_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we have coordinates
        if (brewery.getLatitude() != null && brewery.getLongitude() != null) {
            // Use lat/long for precise location
            double lat = brewery.getLatitude();
            double lon = brewery.getLongitude();
            String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + android.net.Uri.encode(brewery.getName()) + ")";

            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            // Check if Google Maps is installed
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to browser if Google Maps is not installed
                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl));
                startActivity(browserIntent);
            }
        } else {
            // Use address-based search if no coordinates
            StringBuilder address = new StringBuilder();
            if (brewery.getStreet() != null && !brewery.getStreet().isEmpty()) {
                address.append(brewery.getStreet()).append(", ");
            }
            if (brewery.getCity() != null && !brewery.getCity().isEmpty()) {
                address.append(brewery.getCity()).append(", ");
            }
            if (brewery.getState() != null && !brewery.getState().isEmpty()) {
                address.append(brewery.getState()).append(" ");
            }
            if (brewery.getPostal_code() != null && !brewery.getPostal_code().isEmpty()) {
                address.append(brewery.getPostal_code());
            }

            if (address.length() > 0) {
                String query = android.net.Uri.encode(brewery.getName() + " " + address.toString());
                String uri = "geo:0,0?q=" + query;

                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // Fallback to browser
                    String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + query;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl));
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this, R.string.toast_no_location_info, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

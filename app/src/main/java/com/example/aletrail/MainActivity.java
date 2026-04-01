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

    // UI елементи
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
    private View filterCard;
    private View cardsEmptyState;
    private View favoritesEmptyState;
    private Button btnGoBreweriesFromCards;
    private Button btnGoBreweriesFromFavorites;

    // Адаптери
    private BreweryAdapter breweryAdapter;
    private BreweryAdapter favoritesAdapter;
    private LoyaltyCardAdapter loyaltyCardAdapter;
    private BadgeAdapter badgeAdapter;

    // Services и repository-та
    private BreweryRepository breweryRepository;
    private LoyaltyCardRepository loyaltyCardRepository;
    private LocationService locationService;
    private GamificationService gamificationService;
    private SyncService syncService;
    private AppwriteService appwriteService;
    private Database database;

    // Текущ таб
    private int currentTab = 0;

    // Текущи филтри
    private String currentStateFilter = null;
    private String currentTypeFilter = null;

    // LiveData, за да не закачаме излишни observer-и
    private LiveData<List<BreweryEntity>> breweriesLiveData;
    private LiveData<List<BreweryEntity>> favoritesLiveData;
    private LiveData<List<LoyaltyCardEntity>> cardsLiveData;
    private LiveData<List<BadgeEntity>> badgesLiveData;

    // Debounce за търсене
    private final android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    // По подразбиране филтрите са скрити
    private boolean isFilterExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Прилагаме запазената тема
        android.content.SharedPreferences prefs = getSharedPreferences("aletrail_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        // Проверка за auth - ако няма логин, пращаме към Login
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

        // Скриваме системната лента
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

        // Обработваме deep link за email verify
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
            int id = item.getItemId();
            if (id == R.id.action_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.action_reviews) {
                startActivity(new Intent(this, RatingsActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.action_favorites_map) {
                createFavoritesMap();
                return true;
            } else if (id == R.id.action_create_business) {
                startActivity(new Intent(this, CreateBusinessActivity.class));
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

        // Стартиране на автоматичен синхрон
        syncService.scheduleAutoSync(currentUserId);

        // Инициализиране на значките за потребителя
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
        filterCard = findViewById(R.id.filterCard);
        cardsEmptyState = findViewById(R.id.cardsEmptyState);
        favoritesEmptyState = findViewById(R.id.favoritesEmptyState);
        btnGoBreweriesFromCards = findViewById(R.id.btnGoBreweriesFromCards);
        btnGoBreweriesFromFavorites = findViewById(R.id.btnGoBreweriesFromFavorites);

        // Държим filter-ите скрити по подразбиране.
        filterCard.setVisibility(View.GONE);
        cardsEmptyState.setVisibility(View.GONE);
        favoritesEmptyState.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Адаптер за пивоварни
        breweryAdapter = new BreweryAdapter(new BreweryAdapter.OnBreweryClickListener() {
            @Override
            public void onBreweryClick(BreweryEntity brewery) {
                openBreweryInMaps(brewery);
            }

            @Override
            public void onFavoriteClick(BreweryEntity brewery) {
                android.util.Log.d("MainActivity", "Favorite clicked for: " + brewery.getName());

                // Взимаме крайното favorite състояние чрез callback
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

            @Override
            public void onViewReviewsClick(BreweryEntity brewery) {
                openBreweryReviews(brewery);
            }
        });

        // Адаптер за Favorites
        favoritesAdapter = new BreweryAdapter(new BreweryAdapter.OnBreweryClickListener() {
            @Override
            public void onBreweryClick(BreweryEntity brewery) {
                openBreweryInMaps(brewery);
            }

            @Override
            public void onFavoriteClick(BreweryEntity brewery) {
                // Потвърждение преди махане от favorites
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

            @Override
            public void onViewReviewsClick(BreweryEntity brewery) {
                openBreweryReviews(brewery);
            }
        }, true); // true = показва remove бутона

        // Адаптер за loyalty карти
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

        // Адаптер за badges
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
        View filterCard = this.filterCard;

        // Лека crossfade анимация
        recyclerView.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            switch (position) {
                case 0: // Пивоварни
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(breweryAdapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    cardsEmptyState.setVisibility(View.GONE);
                    favoritesEmptyState.setVisibility(View.GONE);
                    loadBreweries();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.VISIBLE);
                    filterCard.setVisibility(isFilterExpanded ? View.VISIBLE : View.GONE);
                    fabFindNearby.show();
                    break;
                case 1: // Моите карти
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(loyaltyCardAdapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    cardsEmptyState.setVisibility(View.GONE);
                    favoritesEmptyState.setVisibility(View.GONE);
                    loadUserCards();
                    loadBestDiscount(); // Обновяваме discount данните
                    fabAddCard.show();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    isFilterExpanded = false;
                    fabFindNearby.hide();
                    break;
                case 2: // Badges grid
                    recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                    recyclerView.setAdapter(badgeAdapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    cardsEmptyState.setVisibility(View.GONE);
                    favoritesEmptyState.setVisibility(View.GONE);
                    loadBadges();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    isFilterExpanded = false;
                    fabFindNearby.hide();
                    break;
                case 3: // Favorites
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(favoritesAdapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    cardsEmptyState.setVisibility(View.GONE);
                    favoritesEmptyState.setVisibility(View.GONE);
                    loadFavorites();
                    fabAddCard.hide();
                    searchInputLayout.setVisibility(View.GONE);
                    filterCard.setVisibility(View.GONE);
                    isFilterExpanded = false;
                    fabFindNearby.hide();
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

        btnGoBreweriesFromCards.setOnClickListener(v -> tabLayout.selectTab(tabLayout.getTabAt(0)));
        btnGoBreweriesFromFavorites.setOnClickListener(v -> tabLayout.selectTab(tabLayout.getTabAt(0)));

        // Показваме/скриваме compact филтъра от иконата в search
        searchInputLayout.setEndIconOnClickListener(v -> {
            if (currentTab != 0) return;
            isFilterExpanded = !isFilterExpanded;
            filterCard.setVisibility(isFilterExpanded ? View.VISIBLE : View.GONE);
        });

        // Бутон за скрол нагоре
        fabScrollTop.setOnClickListener(v -> {
            recyclerView.scrollToPosition(0);
            androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) findViewById(R.id.recyclerView).getParent().getParent();
            scrollView.smoothScrollTo(0, 0);
            fabScrollTop.setVisibility(View.GONE);
        });

        // Показваме бутона, когато списъкът е надолу
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
                    // Чистим стар debounce callback
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    // Нов callback с малко закъснение
                    searchRunnable = () -> {
                        if (s.length() > 0) {
                            searchBreweries(s.toString());
                        } else {
                            loadBreweries();
                        }
                    };

                    // 500ms debounce
                    searchHandler.postDelayed(searchRunnable, 500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBreweries() {
        // Махаме стар observer и закачаме нов
        if (breweriesLiveData != null) {
            breweriesLiveData.removeObservers(this);
            breweriesLiveData = null;
        }

        breweriesLiveData = breweryRepository.getAllBreweries();
        breweriesLiveData.observe(this, breweries -> {
            if (breweries != null && !breweries.isEmpty()) {
                breweryAdapter.setBreweries(breweries);
            } else {
                // Ако локално е празно, дърпаме от API
                breweryRepository.fetchBreweriesByCity("San Francisco", 20);
            }
        });
    }

    private void searchBreweries(String query) {
        // Ако няма query, връщаме целия списък
        if (query == null || query.trim().isEmpty()) {
            loadBreweries();
            return;
        }

        // Махаме стар observer
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
                java.util.List<LoyaltyCardEntity> safeCards = cards != null ? cards : new java.util.ArrayList<>();
                loyaltyCardAdapter.setCards(safeCards);

                if (currentTab == 1) {
                    boolean empty = safeCards.isEmpty();
                    cardsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
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

        // Презакачаме observer, за да е винаги актуално
        if (favoritesLiveData != null) {
            favoritesLiveData.removeObservers(this);
            favoritesLiveData = null;
        }

        favoritesLiveData = breweryRepository.getFavoriteBreweries();
        favoritesLiveData.observe(this, favorites -> {
            java.util.List<BreweryEntity> safeFavorites = favorites != null ? favorites : new java.util.ArrayList<>();
            favoritesAdapter.setBreweries(safeFavorites);

            if (currentTab == 3) {
                boolean empty = safeFavorites.isEmpty();
                favoritesEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void loadUserStats() {
        LiveData<UserEntity> userLiveData = database.userDAO().getUserById(currentUserId);
        userLiveData.observe(this, user -> {
            if (user != null) {
                totalStampsText.setText(String.valueOf(user.getTotalStamps()));
                totalVisitsText.setText(String.valueOf(user.getTotalVisits()));

                // Показваме име + буква аватар
                String name = user.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    statsUserName.setText(name);
                    statsAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                } else {
                    statsUserName.setText(R.string.stats_title);
                    statsAvatarInitial.setText("?");
                }

                // Зареждаме снимка, ако има
                String imageUrl = user.getProfileImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    statsAvatarImage.setVisibility(View.VISIBLE);
                    statsAvatarInitial.setVisibility(View.GONE);
                    com.bumptech.glide.Glide.with(MainActivity.this)
                            .load(android.net.Uri.parse(imageUrl))
                            .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .into(statsAvatarImage);
                } else {
                    statsAvatarImage.setVisibility(View.GONE);
                    statsAvatarInitial.setVisibility(View.VISIBLE);
                }
            }
        });

        LiveData<Integer> badgeCountLiveData = database.badgeDAO().getEarnedBadgeCount(currentUserId);
        badgeCountLiveData.observe(this, count -> {
            if (count != null) {
                badgesEarnedText.setText(String.valueOf(count));
            }
        });

        // Обновяваме най-добрата отстъпка
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
                    // Търсим името на пивоварната
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
        // Очакваме формат ALETRAIL_STAMP:breweryId:token
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
                    tabLayout.selectTab(tabLayout.getTabAt(1)); // Преминаваме към "My Cards"
                    gamificationService.checkCardBadges(currentUserId);
                }
            });
        });
    }

    // Показваме stamp QR за пивоварната.
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

    // Диалог за оценка на пивоварна.
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

                // Пазим рейтинга локално
                new Thread(() -> {
                    BeerRatingEntity ratingEntity = new BeerRatingEntity();
                    ratingEntity.setUserId(currentUserId);
                    ratingEntity.setBreweryId(brewery.getId());
                    ratingEntity.setBeerName(beerName.isEmpty() ? null : beerName);
                    ratingEntity.setRating(rating);
                    ratingEntity.setComment(comment.isEmpty() ? null : comment);
                    database.beerRatingDAO().insert(ratingEntity);

                    // Sync към cloud
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

                    // Проверяваме badge-овете след нов рейтинг
                    int totalRatings = database.beerRatingDAO().getTotalRatingCountSync(currentUserId);
                    gamificationService.checkRatingBadges(currentUserId, totalRatings);
                    gamificationService.checkVisitBadges(currentUserId);

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
                        loadUserCards(); // Презареждаме картите
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

                // Проверка за sharing badges (Party Starter)
                gamificationService.checkSharingBadges(currentUserId);
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

    // Отваряме екрана с ревюта за тази пивоварна.
    private void openBreweryReviews(BreweryEntity brewery) {
        Intent intent = new Intent(this, BreweryReviewsActivity.class);
        intent.putExtra("breweryId", brewery.getId());
        intent.putExtra("breweryName", brewery.getName());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void applyFilters() {
        String state = stateFilterInput.getText().toString().trim();
        String type = null;

        // Взимаме избрания type чип
        int selectedChipId = typeFilterChipGroup.getCheckedChipId();
        if (selectedChipId != -1 && selectedChipId != R.id.chipAll) {
            android.view.View selectedChip = typeFilterChipGroup.findViewById(selectedChipId);
            type = ((com.google.android.material.chip.Chip)selectedChip).getText().toString().toLowerCase();
        }

        // Запазваме текущите филтри
        currentStateFilter = state.isEmpty() ? null : state;
        currentTypeFilter = type;

        // Махаме стария observer преди нова заявка
        if (breweriesLiveData != null) {
            breweriesLiveData.removeObservers(this);
            breweriesLiveData = null;
        }

        // Прилагаме филтъра и слушаме резултатите от БД
        if (currentStateFilter != null && currentTypeFilter != null) {
            // State + type заедно
            Toast.makeText(this, "Filtering by " + currentStateFilter + " and " + currentTypeFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByStateAndType(currentStateFilter, currentTypeFilter, 50);

            // Слушаме филтрираните резултати
            breweriesLiveData = breweryRepository.getBreweriesByStateAndType(currentStateFilter, currentTypeFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else if (currentStateFilter != null) {
            // Само state
            Toast.makeText(this, "Filtering by state: " + currentStateFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByState(currentStateFilter, 50);

            // Слушаме филтрираните резултати
            breweriesLiveData = breweryRepository.getBreweriesByState(currentStateFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else if (currentTypeFilter != null) {
            // Само type
            Toast.makeText(this, "Filtering by type: " + currentTypeFilter, Toast.LENGTH_SHORT).show();
            breweryRepository.fetchBreweriesByType(currentTypeFilter, 50);

            // Слушаме филтрираните резултати
            breweriesLiveData = breweryRepository.getBreweriesByType(currentTypeFilter);
            breweriesLiveData.observe(this, breweries -> {
                if (breweries != null) {
                    breweryAdapter.setBreweries(breweries);
                }
            });
        } else {
            // Няма филтър - връщаме пълния списък
            Toast.makeText(this, "Showing all breweries", Toast.LENGTH_SHORT).show();
            loadBreweries();
        }
    }

    // Отваряме локацията в Google Maps (или браузър fallback).
    private void openBreweryInMaps(BreweryEntity brewery) {
        if (brewery == null) {
            Toast.makeText(this, R.string.toast_brewery_info_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        // Ако има координати, ползваме тях
        if (brewery.getLatitude() != null && brewery.getLongitude() != null) {
            // Изграждаме прецизен geo intent.
            double lat = brewery.getLatitude();
            double lon = brewery.getLongitude();
            String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + android.net.Uri.encode(brewery.getName()) + ")";

            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            // Проверка дали има инсталиран Google Maps
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Browser fallback
                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl));
                startActivity(browserIntent);
            }
        } else {
            // Ако няма координати, търсим по адрес
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
                    // Browser fallback
                    String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + query;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl));
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this, R.string.toast_no_location_info, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Създаваме Cartes карта от любимите пивоварни.
    private void createFavoritesMap() {
        Toast.makeText(this, R.string.map_creating, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.util.List<BreweryEntity> favorites = database.AleDAO().getFavoritesSync();
                if (favorites == null || favorites.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.map_empty, Toast.LENGTH_SHORT).show());
                    return;
                }

                String userName = appwriteService.getSavedUserName();
                String mapTitle = (userName != null ? userName : "AleTrail") + "'s Favorites";
                CartesModels.CreateMapRequest req = new CartesModels.CreateMapRequest(
                        mapTitle, "My favorite breweries from AleTrail");

                retrofit2.Response<okhttp3.ResponseBody> mapResponse =
                        RetrofitClient.getCartesAPI().createMap(req).execute();

                if (!mapResponse.isSuccessful() || mapResponse.body() == null) {
                    String errBody;
                    try (okhttp3.ResponseBody errorBody = mapResponse.errorBody()) {
                        errBody = errorBody != null ? errorBody.string() : "Unknown error";
                    }
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, errBody), Toast.LENGTH_LONG).show());
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
                        // Понякога API връща plain JSON string при грешка
                        String msg = parsed.getAsString();
                        final String finalMsg = msg;
                        runOnUiThread(() -> Toast.makeText(this,
                                getString(R.string.map_error, finalMsg), Toast.LENGTH_LONG).show());
                        return;
                    }
                } catch (Exception parseException) {
                    final String parseErr = rawBody != null && !rawBody.isEmpty() ? rawBody : parseException.getMessage();
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, parseErr), Toast.LENGTH_LONG).show());
                    return;
                }

                if (mapToken == null || mapToken.trim().isEmpty() || mapUuid == null || mapUuid.trim().isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.map_error, "Map token/uuid missing in Cartes response"),
                            Toast.LENGTH_LONG).show());
                    return;
                }

                int addedCount = 0;

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
                                android.util.Log.e("MainActivity", "Failed to add marker: " + markerErr);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("MainActivity", "Failed to add marker: " + e.getMessage());
                        }
                    }
                }

                final int count = addedCount;
                final String mapId = mapUuid;
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.map_created, count), Toast.LENGTH_SHORT).show();
                    String url = "https://app.cartes.io/maps/" + mapId;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    startActivity(browserIntent);
                });

            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error creating favorites map: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.map_error, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

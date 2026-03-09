package com.example.aletrail;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private String currentUserId; // Dynamic user ID from auth

    // UI Components
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private TextInputEditText searchEditText;
    private TextInputEditText stateFilterInput;
    private Button findNearbyButton;
    private Button scanQRButton;
    private Button applyFilterButton;
    private ExtendedFloatingActionButton fabAddCard;
    private TextView totalStampsText;
    private TextView totalVisitsText;
    private TextView badgesEarnedText;
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

        // Check auth — redirect to login if not authenticated
        appwriteService = AppwriteService.getInstance(this);
        if (!appwriteService.isLoggedInLocally()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = appwriteService.getSavedUserId();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
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
        stateFilterInput = findViewById(R.id.stateFilterInput);
        findNearbyButton = findViewById(R.id.findNearbyButton);
        scanQRButton = findViewById(R.id.scanQRButton);
        applyFilterButton = findViewById(R.id.applyFilterButton);
        fabAddCard = findViewById(R.id.fabAddCard);
        totalStampsText = findViewById(R.id.totalStampsText);
        totalVisitsText = findViewById(R.id.totalVisitsText);
        badgesEarnedText = findViewById(R.id.badgesEarnedText);
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
                                newState ? "Added to favorites ⭐" : "Removed from favorites",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onCreateCardClick(BreweryEntity brewery) {
                createLoyaltyCard(brewery);
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
                    .setTitle("Remove from Favorites?")
                    .setMessage("Do you want to remove \"" + brewery.getName() + "\" from your favorites?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        breweryRepository.toggleFavorite(brewery, new BreweryRepository.ToggleFavoriteCallback() {
                            @Override
                            public void onComplete(boolean newState) {
                                runOnUiThread(() -> {
                                    // If the resulting state is false, it was removed
                                    if (!newState) {
                                        Toast.makeText(MainActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Added to favorites ⭐", Toast.LENGTH_SHORT).show();
                                    }

                                    loadFavorites(); // Reload favorites list
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }

            @Override
            public void onCreateCardClick(BreweryEntity brewery) {
                createLoyaltyCard(brewery);
            }
        }, true); // Pass true to show the remove button

        // Setup Loyalty Card Adapter
        loyaltyCardAdapter = new LoyaltyCardAdapter(new LoyaltyCardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(LoyaltyCardEntity card) {
                Toast.makeText(MainActivity.this, "Card ID: " + card.getCardId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onQRCodeClick(LoyaltyCardEntity card) {
                showQRCodeDialog(card);
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
        badgeAdapter = new BadgeAdapter(badge ->
            Toast.makeText(MainActivity.this, badge.getBadgeName(), Toast.LENGTH_SHORT).show()
        );

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
        android.view.View filterCard = findViewById(R.id.filterCard);
        switch (position) {
            case 0: // Breweries
                recyclerView.setAdapter(breweryAdapter);
                loadBreweries();
                fabAddCard.hide();
                searchEditText.setVisibility(android.view.View.VISIBLE);
                filterCard.setVisibility(android.view.View.VISIBLE);
                break;
            case 1: // My Cards
                recyclerView.setAdapter(loyaltyCardAdapter);
                loadUserCards();
                fabAddCard.show();
                searchEditText.setVisibility(android.view.View.GONE);
                filterCard.setVisibility(android.view.View.GONE);
                break;
            case 2: // Badges
                recyclerView.setAdapter(badgeAdapter);
                loadBadges();
                fabAddCard.hide();
                searchEditText.setVisibility(android.view.View.GONE);
                filterCard.setVisibility(android.view.View.GONE);
                break;
            case 3: // Favorites
                recyclerView.setAdapter(favoritesAdapter);
                loadFavorites();
                fabAddCard.hide();
                searchEditText.setVisibility(android.view.View.GONE);
                filterCard.setVisibility(android.view.View.GONE);
                break;
        }
    }

    private void setupButtons() {
        findNearbyButton.setOnClickListener(v -> findNearbyBreweries());
        scanQRButton.setOnClickListener(v -> scanQRCode());
        fabAddCard.setOnClickListener(v -> {
            Toast.makeText(this, "Select a brewery to create a card", Toast.LENGTH_SHORT).show();
            tabLayout.selectTab(tabLayout.getTabAt(0));
        });
        applyFilterButton.setOnClickListener(v -> applyFilters());
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
            }
        });

        LiveData<Integer> badgeCountLiveData = database.badgeDAO().getEarnedBadgeCount(currentUserId);
        badgeCountLiveData.observe(this, count -> {
            if (count != null) {
                badgesEarnedText.setText(String.valueOf(count));
            }
        });
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
                Toast.makeText(this, "Searching nearby breweries...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scanQRCode() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a brewery QR code");
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
        if (QRCodeService.isValidQRCode(qrValue)) {
            String userId = QRCodeService.extractUserIdFromQR(qrValue);
            String breweryId = QRCodeService.extractBreweryIdFromQR(qrValue);

            // Add stamp to card
            locationService.getCurrentLocation(location -> {
                // TODO: Find card by userId and breweryId, then add stamp
                Toast.makeText(this, "Stamp added! 🎉", Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void createLoyaltyCard(BreweryEntity brewery) {
        loyaltyCardRepository.createLoyaltyCard(currentUserId, brewery.getId(), 10, cardId -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Loyalty card created! 🎉", Toast.LENGTH_SHORT).show();
                tabLayout.selectTab(tabLayout.getTabAt(1)); // Switch to My Cards tab
            });
        });
    }

    private void showQRCodeDialog(LoyaltyCardEntity card) {
        // Create dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);

        // Generate QR code bitmap
        android.graphics.Bitmap qrBitmap = QRCodeService.generateQRCodeBitmap(card.getQrCodeValue(), 512, 512);

        if (qrBitmap != null) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setImageBitmap(qrBitmap);
            imageView.setPadding(50, 50, 50, 50);

            builder.setTitle("Scan this QR Code")
                   .setView(imageView)
                   .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                   .setNeutralButton("Share", (dialog, which) -> shareCard(card))
                   .create()
                   .show();
        } else {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCard(LoyaltyCardEntity card) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Card")
            .setMessage("Are you sure you want to delete this loyalty card?")
            .setPositiveButton("Delete", (dialog, which) -> {
                loyaltyCardRepository.deleteCard(card.getCardId(), () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                        loadUserCards(); // Reload cards
                    });
                });
            })
            .setNegativeButton("Cancel", null)
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
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showQRCode(LoyaltyCardEntity card) {
        showQRCodeDialog(card);
    }

    private void shareCard(LoyaltyCardEntity card) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Brewery Loyalty Card");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
            "Check out my loyalty card!\n\n" +
            "Card ID: " + card.getCardId() + "\n" +
            "Stamps: " + card.getStamps() + "/" + card.getMaxStamps() + "\n" +
            "QR Code: " + card.getQrCodeValue());
        startActivity(Intent.createChooser(shareIntent, "Share loyalty card"));
    }

    private void showVisitHistory(LoyaltyCardEntity card) {
        // ...existing code...
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
            Toast.makeText(this, "Brewery information not available", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "No location information available for this brewery", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

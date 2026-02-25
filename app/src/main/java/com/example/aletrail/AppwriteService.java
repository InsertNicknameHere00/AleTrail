package com.example.aletrail;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.appwrite.Client;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

/**
 * Singleton wrapper around Appwrite Account and Databases services.
 * All SDK calls (Kotlin suspend functions) are bridged to Java via
 * BuildersKt.runBlocking on a background ExecutorService.
 */
public class AppwriteService {

    private static final String TAG = "AppwriteService";
    private static final String PREFS_NAME = "aletrail_prefs";
    private static final String KEY_USER_ID = "appwrite_user_id";
    private static final String KEY_USER_EMAIL = "appwrite_user_email";
    private static final String KEY_USER_NAME = "appwrite_user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private static volatile AppwriteService instance;

    private final Account account;
    private final Databases databases;
    private final SharedPreferences prefs;
    private final ExecutorService executor;

    private AppwriteService(Context context) {
        Client client = AppwriteClientProvider.getClient(context);
        this.account = new Account(client);
        this.databases = new Databases(client);
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newFixedThreadPool(3);
    }

    public static AppwriteService getInstance(Context context) {
        if (instance == null) {
            synchronized (AppwriteService.class) {
                if (instance == null) {
                    instance = new AppwriteService(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ─── Callback interfaces ──────────────────────────────────────────

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public interface DocumentCallback {
        void onSuccess(Document<Map<String, Object>> document);
        void onError(String message);
    }

    public interface DocumentListCallback {
        void onSuccess(List<Document<Map<String, Object>>> documents);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    // ─── Auth Methods ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void createAccount(String email, String password, String name,
                              AuthCallback<User<Map<String, Object>>> callback) {
        // Use a standard UUID as the userId — guaranteed to satisfy Appwrite's account
        // userId rules: max 36 chars, only [a-zA-Z0-9._-], cannot start with special char.
        // UUID format is xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (36 chars, hex + hyphens only).
        String uniqueId = java.util.UUID.randomUUID().toString();
        executor.execute(() -> {
            try {
                User<Map<String, Object>> user = (User<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return account.create(uniqueId, email, password, name, cont);
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                saveUserToPrefs(user);
                Log.d(TAG, "Account created: " + user.getId());
                callback.onSuccess(user);
            } catch (Exception e) {
                Log.e(TAG, "Create account failed: " + e.getMessage(), e);
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    public void login(String email, String password, AuthCallback<Session> callback) {
        executor.execute(() -> {
            try {
                Object result = BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return account.createEmailPasswordSession(email, password, cont);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                Session session = (Session) result;
                fetchAndSaveCurrentUser();
                Log.d(TAG, "Login success, session: " + session.getId());
                callback.onSuccess(session);
            } catch (Exception e) {
                Log.e(TAG, "Login failed: " + e.getMessage(), e);
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void getCurrentUser(AuthCallback<User<Map<String, Object>>> callback) {
        executor.execute(() -> {
            try {
                User<Map<String, Object>> user = (User<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return account.get(cont);
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                saveUserToPrefs(user);
                callback.onSuccess(user);
            } catch (Exception e) {
                Log.d(TAG, "No current user: " + e.getMessage());
                clearPrefs();
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    public void logout(AuthCallback<Void> callback) {
        executor.execute(() -> {
            try {
                BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return account.deleteSession("current", cont);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                clearPrefs();
                Log.d(TAG, "Logged out");
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Logout failed: " + e.getMessage(), e);
                clearPrefs();
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void updateName(String name, AuthCallback<User<Map<String, Object>>> callback) {
        executor.execute(() -> {
            try {
                User<Map<String, Object>> user = (User<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return account.updateName(name, cont);
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                saveUserToPrefs(user);
                callback.onSuccess(user);
            } catch (Exception e) {
                Log.e(TAG, "Update name failed: " + e.getMessage(), e);
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    // ─── Session helpers ──────────────────────────────────────────────

    public boolean isLoggedInLocally() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getSavedUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getSavedUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getSavedUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    private void saveUserToPrefs(User<Map<String, Object>> user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getName())
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public void clearSession() {
        clearPrefs();
    }

    private void clearPrefs() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    @SuppressWarnings("unchecked")
    private void fetchAndSaveCurrentUser() {
        try {
            User<Map<String, Object>> user = (User<Map<String, Object>>) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, cont) -> {
                        try {
                            return account.get(cont);
                        } catch (AppwriteException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            saveUserToPrefs(user);
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch user after login: " + e.getMessage());
        }
    }

    // ─── Database generic methods ─────────────────────────────────────

    public void createDocument(String collectionId, Map<String, Object> data,
                               DocumentCallback callback) {
        String uniqueId = java.util.UUID.randomUUID().toString();
        createDocument(collectionId, uniqueId, data, callback);
    }

    @SuppressWarnings("unchecked")
    public void createDocument(String collectionId, String documentId,
                               Map<String, Object> data, DocumentCallback callback) {
        // Use explicit typed list to resolve overload ambiguity
        List<String> permissions = new ArrayList<>();
        executor.execute(() -> {
            try {
                Document<Map<String, Object>> doc = (Document<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.createDocument(
                                        AppwriteConstants.DATABASE_ID,
                                        collectionId,
                                        documentId,
                                        data,
                                        permissions,
                                        cont
                                );
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                Log.d(TAG, "Document created in " + collectionId + ": " + doc.getId());
                if (callback != null) callback.onSuccess(doc);
            } catch (Exception e) {
                Log.e(TAG, "Create document failed: " + e.getMessage(), e);
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void listDocuments(String collectionId, List<String> queries,
                              DocumentListCallback callback) {
        List<String> q = queries != null ? queries : new ArrayList<>();
        executor.execute(() -> {
            try {
                DocumentList<Map<String, Object>> result = (DocumentList<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.listDocuments(
                                        AppwriteConstants.DATABASE_ID,
                                        collectionId,
                                        q,
                                        cont
                                );
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                Log.d(TAG, "Listed " + result.getDocuments().size() + " docs from " + collectionId);
                if (callback != null) callback.onSuccess(result.getDocuments());
            } catch (Exception e) {
                Log.e(TAG, "List documents failed: " + e.getMessage(), e);
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void updateDocument(String collectionId, String documentId,
                               Map<String, Object> data, DocumentCallback callback) {
        // Use explicit typed list to resolve overload ambiguity
        List<String> permissions = new ArrayList<>();
        executor.execute(() -> {
            try {
                Document<Map<String, Object>> doc = (Document<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.updateDocument(
                                        AppwriteConstants.DATABASE_ID,
                                        collectionId,
                                        documentId,
                                        data,
                                        permissions,
                                        cont
                                );
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                Log.d(TAG, "Document updated in " + collectionId + ": " + doc.getId());
                if (callback != null) callback.onSuccess(doc);
            } catch (Exception e) {
                Log.e(TAG, "Update document failed: " + e.getMessage(), e);
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }

    public void deleteDocument(String collectionId, String documentId,
                               SimpleCallback callback) {
        executor.execute(() -> {
            try {
                BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.deleteDocument(
                                        AppwriteConstants.DATABASE_ID,
                                        collectionId,
                                        documentId,
                                        cont
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                Log.d(TAG, "Document deleted from " + collectionId + ": " + documentId);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Delete document failed: " + e.getMessage(), e);
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }

    // ─── Entity-specific sync helpers ─────────────────────────────────

    public void syncVisit(VisitEntity visit, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", visit.getUserId());
        data.put("breweryId", visit.getBreweryId());
        data.put("cardId", visit.getCardId());
        data.put("visitTimestamp", visit.getVisitTimestamp());
        data.put("stampAdded", visit.isStampAdded());
        data.put("notes", visit.getNotes() != null ? visit.getNotes() : "");
        data.put("latitude", visit.getLatitude());
        data.put("longitude", visit.getLongitude());

        createDocument(AppwriteConstants.COLLECTION_VISITS, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void syncRating(BeerRatingEntity rating, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", rating.getUserId());
        data.put("breweryId", rating.getBreweryId());
        data.put("beerName", rating.getBeerName() != null ? rating.getBeerName() : "");
        data.put("rating", (double) rating.getRating());
        data.put("comment", rating.getComment() != null ? rating.getComment() : "");
        data.put("ratingTimestamp", rating.getRatingTimestamp());

        createDocument(AppwriteConstants.COLLECTION_BEER_RATINGS, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void syncFavorite(String userId, BreweryEntity brewery, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("breweryId", brewery.getId());
        data.put("breweryName", brewery.getName() != null ? brewery.getName() : "");
        data.put("addedAt", System.currentTimeMillis());

        createDocument(AppwriteConstants.COLLECTION_FAVORITES, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void syncLoyaltyCard(LoyaltyCardEntity card, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", card.getUserId());
        data.put("breweryId", card.getBreweryId());
        data.put("stamps", card.getStamps());
        data.put("maxStamps", card.getMaxStamps());
        data.put("qrCodeValue", card.getQrCodeValue() != null ? card.getQrCodeValue() : "");
        data.put("active", card.isActive());

        createDocument(AppwriteConstants.COLLECTION_LOYALTY_CARDS, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void syncBadge(BadgeEntity badge, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", badge.getUserId());
        data.put("badgeType", badge.getBadgeType());
        data.put("badgeName", badge.getBadgeName() != null ? badge.getBadgeName() : "");
        data.put("badgeDescription", badge.getBadgeDescription() != null ? badge.getBadgeDescription() : "");
        data.put("badgeIcon", badge.getBadgeIcon() != null ? badge.getBadgeIcon() : "");
        data.put("requiredCount", badge.getRequiredCount());
        data.put("earnedTimestamp", badge.getEarnedTimestamp());
        data.put("isEarned", badge.isEarned());
        data.put("breweryId", badge.getBreweryId() != null ? badge.getBreweryId() : "");

        createDocument(AppwriteConstants.COLLECTION_BADGES, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }

    public void syncUserProfile(UserEntity user, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        data.put("authProvider", user.getAuthProvider() != null ? user.getAuthProvider() : "email");
        data.put("profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");
        data.put("createdAt", user.getCreatedAt());
        data.put("totalStamps", user.getTotalStamps());
        data.put("totalVisits", user.getTotalVisits());
        data.put("totalBreweriesVisited", user.getTotalBreweriesVisited());
        data.put("isPremium", user.isPremium());

        createDocument(AppwriteConstants.COLLECTION_USERS, user.getUserId(), data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                if (message != null && message.contains("already exists")) {
                    updateDocument(AppwriteConstants.COLLECTION_USERS, user.getUserId(), data,
                            new DocumentCallback() {
                                @Override
                                public void onSuccess(Document<Map<String, Object>> doc) {
                                    if (callback != null) callback.onSuccess();
                                }
                                @Override
                                public void onError(String msg) {
                                    if (callback != null) callback.onError(msg);
                                }
                            });
                } else {
                    if (callback != null) callback.onError(message);
                }
            }
        });
    }

    // ─── Utility ──────────────────────────────────────────────────────

    private String extractErrorMessage(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof AppwriteException) {
            return cause.getMessage();
        }
        if (e instanceof AppwriteException) {
            return e.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }
}







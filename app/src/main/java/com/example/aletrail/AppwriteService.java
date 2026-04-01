package com.example.aletrail;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
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

// Помощен service за Appwrite auth и sync към базата.
public class AppwriteService {

    // Основен singleton за Appwrite операции.
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
    private final String databaseId;
    private final String verifyUrl;

    private AppwriteService(Context context) {
        Client client = AppwriteClientProvider.getClient(context);
        this.account = new Account(client);
        this.databases = new Databases(client);
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newFixedThreadPool(3);
        this.databaseId = context.getString(R.string.appwrite_database_id);
        this.verifyUrl = context.getString(R.string.appwrite_verify_url);
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


    @SuppressWarnings("unchecked")
    public void createAccount(String email, String password, String name,
                              AuthCallback<User<Map<String, Object>>> callback) {
        // UUID покрива изискванията на Appwrite за userId.
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
                Session session = createEmailPasswordSession(email, password);
                fetchAndSaveCurrentUser();
                Log.d(TAG, "Login success, session: " + session.getId());
                callback.onSuccess(session);
            } catch (Exception firstError) {
                String firstMessage = extractErrorMessage(firstError);

                // Ако има активна сесия, трием я и пробваме пак веднъж.
                if (firstMessage != null && firstMessage.toLowerCase().contains("session")
                        && firstMessage.toLowerCase().contains("active")) {
                    Log.w(TAG, "Active session detected during login, retrying after deleteSession(current)");
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

                        Session retrySession = createEmailPasswordSession(email, password);
                        fetchAndSaveCurrentUser();
                        Log.d(TAG, "Login retry success, session: " + retrySession.getId());
                        callback.onSuccess(retrySession);
                        return;
                    } catch (Exception retryError) {
                        Log.e(TAG, "Login retry failed: " + retryError.getMessage(), retryError);
                        callback.onError(extractErrorMessage(retryError));
                        return;
                    }
                }

                Log.e(TAG, "Login failed: " + firstError.getMessage(), firstError);
                callback.onError(firstMessage);
            }
        });
    }

    private Session createEmailPasswordSession(String email, String password) {
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
            return (Session) result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    // Блокира акаунта, чисти данните и изкарва сесията.
    public void deleteAccount(AuthCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 1) Трием документи на потребителя от Appwrite DB.
                String userId = getSavedUserId();
                if (userId != null && !userId.startsWith("guest_")) {
                    deleteAllUserDocuments(userId);
                }

                // 2) Блокираме акаунта чрез updateStatus()
                try {
                    BuildersKt.runBlocking(
                            EmptyCoroutineContext.INSTANCE,
                            (scope, cont) -> {
                                try {
                                    return account.updateStatus(cont);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                    Log.d(TAG, "Account status updated (blocked)");
                } catch (Exception e) {
                    Log.w(TAG, "updateStatus during account deletion: " + e.getMessage());
                }

                // 3) Трием текущата сесия (logout).
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
                } catch (Exception e) {
                    Log.w(TAG, "Session delete during account deletion: " + e.getMessage());
                }

                clearPrefs();
                Log.d(TAG, "Account deletion completed - local + remote data cleared");
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Account deletion failed: " + e.getMessage(), e);
                clearPrefs();
                callback.onError(extractErrorMessage(e));
            }
        });
    }

    // Трие всички Appwrite документи за даден user.
    private void deleteAllUserDocuments(String userId) {
        String[] collections = {
                AppwriteConstants.COLLECTION_USERS,
                AppwriteConstants.COLLECTION_FAVORITES,
                AppwriteConstants.COLLECTION_LOYALTY_CARDS,
                AppwriteConstants.COLLECTION_VISITS,
                AppwriteConstants.COLLECTION_BADGES,
                AppwriteConstants.COLLECTION_BEER_RATINGS
        };

        for (String collectionId : collections) {
            try {
                // В users колекцията docId е същото като userId.
                if (collectionId.equals(AppwriteConstants.COLLECTION_USERS)) {
                    try {
                        BuildersKt.runBlocking(
                                EmptyCoroutineContext.INSTANCE,
                                (scope, cont) -> {
                                    try {
                                        return databases.deleteDocument(
                                                databaseId,
                                                collectionId,
                                                userId,
                                                cont
                                        );
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                        Log.d(TAG, "Deleted user document from " + collectionId);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not delete user doc from " + collectionId + ": " + e.getMessage());
                    }
                    continue;
                }

                // За другите колекции: filter по userId и трием всеки doc.
                @SuppressWarnings("unchecked")
                DocumentList<Map<String, Object>> result = (DocumentList<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                List<String> queries = new ArrayList<>();
                                queries.add("equal(\"userId\", \"" + userId + "\")");
                                return databases.listDocuments(
                                        databaseId,
                                        collectionId,
                                        queries,
                                        cont
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

                for (Document<Map<String, Object>> doc : result.getDocuments()) {
                    try {
                        BuildersKt.runBlocking(
                                EmptyCoroutineContext.INSTANCE,
                                (scope, cont) -> {
                                    try {
                                        return databases.deleteDocument(
                                                databaseId,
                                                collectionId,
                                                doc.getId(),
                                                cont
                                        );
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                        Log.d(TAG, "Deleted document " + doc.getId() + " from " + collectionId);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to delete doc " + doc.getId() + " from " + collectionId + ": " + e.getMessage());
                    }
                }
                Log.d(TAG, "Cleaned " + result.getDocuments().size() + " docs from " + collectionId);
            } catch (Exception e) {
                Log.w(TAG, "Could not clean collection " + collectionId + ": " + e.getMessage());
            }
        }
    }

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


    public void createDocument(String collectionId, Map<String, Object> data,
                               DocumentCallback callback) {
        String uniqueId = java.util.UUID.randomUUID().toString();
        createDocument(collectionId, uniqueId, data, callback);
    }

    @SuppressWarnings("unchecked")
    public void createDocument(String collectionId, String documentId,
                               Map<String, Object> data, DocumentCallback callback) {
        // Добавяме user permissions, когато има логнат потребител.
        List<String> permissions = buildUserPermissions();
        Log.d(TAG, "createDocument: collection=" + collectionId
                + ", docId=" + documentId
                + ", permissions=" + permissions
                + ", dataKeys=" + data.keySet());
        executor.execute(() -> {
            try {
                Document<Map<String, Object>> doc = (Document<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.createDocument(
                                        databaseId,
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
                String errorMsg = extractErrorMessage(e);
                Log.e(TAG, "Create document failed in " + collectionId
                        + " (docId=" + documentId + "): " + errorMsg, e);
                // Подробен лог за грешка от Appwrite.
                Throwable cause = e.getCause();
                if (cause instanceof AppwriteException) {
                    AppwriteException ae = (AppwriteException) cause;
                    Log.e(TAG, "Appwrite error code=" + ae.getCode()
                            + ", type=" + ae.getType()
                            + ", message=" + ae.getMessage());
                }
                if (callback != null) callback.onError(errorMsg);
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
                                        databaseId,
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
        // Пускаме пак permissions, за да не изгубим достъп след update.
        List<String> permissions = buildUserPermissions();
        executor.execute(() -> {
            try {
                Document<Map<String, Object>> doc = (Document<Map<String, Object>>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, cont) -> {
                            try {
                                return databases.updateDocument(
                                        databaseId,
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
                Log.e(TAG, "Update document failed in " + collectionId + ": " + e.getMessage(), e);
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
                                        databaseId,
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


    public void syncVisit(VisitEntity visit, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", visit.getUserId());
        data.put("breweryId", visit.getBreweryId());
        data.put("cardId", visit.getCardId());
        data.put("visitTimestamp", toIso8601(visit.getVisitTimestamp()));
        data.put("stampAdded", visit.isStampAdded());
        String notes = visit.getNotes();
        data.put("notes", (notes != null && !notes.isEmpty()) ? notes : null);
        // Appwrite point очаква списък: [longitude, latitude].
        if (visit.getLatitude() != 0.0 || visit.getLongitude() != 0.0) {
            data.put("latitude", Arrays.asList(visit.getLongitude(), visit.getLatitude()));
            data.put("longitude", Arrays.asList(visit.getLongitude(), visit.getLatitude()));
        }

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
        String comment = rating.getComment();
        data.put("comment", (comment != null && !comment.isEmpty()) ? comment : null);
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
        data.put("addedAt", toIso8601(System.currentTimeMillis()));

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
        // badgeIcon е URL тип в Appwrite.
        String icon = badge.getBadgeIcon();
        if (icon != null && !icon.isEmpty() && !icon.startsWith("http")) {
            // Ако е custom идентификатор, правим placeholder URL.
            icon = "https://aletrail.app/badges/" + icon;
        }
        data.put("badgeIcon", icon != null && !icon.isEmpty() ? icon : "https://aletrail.app/badges/default");
        data.put("requiredCount", badge.getRequiredCount());
        data.put("earnedTimestamp", badge.getEarnedTimestamp());
        data.put("isEarned", badge.isEarned());
        // breweryId може да е null в схемата.
        data.put("breweryId", badge.getBreweryId() != null && !badge.getBreweryId().isEmpty()
                ? badge.getBreweryId() : null);

        // Стабилно docId, за да няма дублирани badge документи.
        String docId = badge.getUserId() + "_" + badge.getBadgeType();
        docId = docId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (docId.length() > 36) {
            docId = docId.substring(0, 36);
        }

        final String finalDocId = docId;

        // Първо update, после create ако не съществува.
        updateDocument(AppwriteConstants.COLLECTION_BADGES, finalDocId, data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                Log.d(TAG, "Badge updated in Appwrite: " + badge.getBadgeType());
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                // Ако doc липсва, създаваме го.
                createDocument(AppwriteConstants.COLLECTION_BADGES, finalDocId, data, new DocumentCallback() {
                    @Override
                    public void onSuccess(Document<Map<String, Object>> document) {
                        Log.d(TAG, "Badge created in Appwrite: " + badge.getBadgeType());
                        if (callback != null) callback.onSuccess();
                    }
                    @Override
                    public void onError(String msg) {
                        Log.e(TAG, "Badge sync FAILED for " + badge.getBadgeType()
                                + " (docId=" + finalDocId + "): " + msg
                                + " | dataKeys=" + data.keySet());
                        if (callback != null) callback.onError(msg);
                    }
                });
            }
        });
    }

    public void syncUserProfile(UserEntity user, SimpleCallback callback) {
        Log.d(TAG, "syncUserProfile called for userId=" + user.getUserId()
                + ", email=" + user.getEmail()
                + ", name=" + user.getDisplayName());
        Map<String, Object> data = new HashMap<>();
        // Основни полета, които трябва да съществуват в users колекцията.
        data.put("userId", user.getUserId());
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        // Не пращаме createdAt, защото Appwrite пази $createdAt.
        // Опционалните полета трябва да съществуват в схемата.
        data.put("authProvider", user.getAuthProvider() != null ? user.getAuthProvider() : "email");
        String imgUrl = user.getProfileImageUrl();
        data.put("profileImageUrl", (imgUrl != null && !imgUrl.isEmpty()) ? imgUrl : null);
        data.put("totalStamps", user.getTotalStamps());
        data.put("totalVisits", user.getTotalVisits());
        data.put("totalBreweriesVisited", user.getTotalBreweriesVisited());
        data.put("isPremium", user.isPremium());

        List<String> permissions = buildUserPermissions();
        Log.d(TAG, "syncUserProfile permissions: " + permissions);
        Log.d(TAG, "syncUserProfile data: " + data);
        Log.d(TAG, "syncUserProfile databaseId=" + databaseId
                + ", collectionId=" + AppwriteConstants.COLLECTION_USERS
                + ", documentId=" + user.getUserId());

        createDocument(AppwriteConstants.COLLECTION_USERS, user.getUserId(), data, new DocumentCallback() {
            @Override
            public void onSuccess(Document<Map<String, Object>> document) {
                Log.d(TAG, "syncUserProfile SUCCESS - document created: " + document.getId());
                if (callback != null) callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                Log.e(TAG, "syncUserProfile createDocument FAILED: " + message);
                if (message != null && message.contains("already exists")) {
                    Log.d(TAG, "syncUserProfile - document already exists, trying update...");
                    updateDocument(AppwriteConstants.COLLECTION_USERS, user.getUserId(), data,
                            new DocumentCallback() {
                                @Override
                                public void onSuccess(Document<Map<String, Object>> doc) {
                                    Log.d(TAG, "syncUserProfile UPDATE SUCCESS: " + doc.getId());
                                    if (callback != null) callback.onSuccess();
                                }
                                @Override
                                public void onError(String msg) {
                                    Log.e(TAG, "syncUserProfile UPDATE FAILED: " + msg);
                                    if (callback != null) callback.onError(msg);
                                }
                            });
                } else {
                    if (callback != null) callback.onError(message);
                }
            }
        });
    }

    // Правим permissions за текущия логнат user.
    private List<String> buildUserPermissions() {
        String userId = getSavedUserId();
        if (userId != null && !userId.startsWith("guest_")) {
            String userRole = "user:" + userId;
            // Използваме read/update/delete.
            return Arrays.asList(
                    "read(\"" + userRole + "\")",
                    "update(\"" + userRole + "\")",
                    "delete(\"" + userRole + "\")"
            );
        }
        // Ако няма логнат user, разчитаме на collection permissions.
        return new ArrayList<>();
    }

    // Конвертира epoch millis към ISO8601.
    private String toIso8601(long epochMillis) {
        if (epochMillis <= 0) {
            epochMillis = System.currentTimeMillis();
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(epochMillis));
    }

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

    // Проверка дали имейлът е верифициран.
    @SuppressWarnings("unchecked")
    public boolean isEmailVerified() {
        try {
            final boolean[] result = {false};
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

            executor.execute(() -> {
                try {
                    User<Map<String, Object>> user = (User<Map<String, Object>>) BuildersKt.runBlocking(
                            EmptyCoroutineContext.INSTANCE,
                            (scope, cont) -> {
                                try {
                                    return account.get(cont);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                    result[0] = user.getEmailVerification();
                } catch (Exception e) {
                    Log.e(TAG, "isEmailVerified error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "isEmailVerified exception: " + e.getMessage());
            return false;
        }
    }

    // Праща verify имейл към текущия профил.
    public void sendVerificationEmail(SimpleCallback callback) {
        executor.execute(() -> {
            try {
                BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, cont) -> {
                        try {
                            return account.createVerification(verifyUrl, cont);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "sendVerificationEmail failed: " + e.getMessage());
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }

    // Завършва verify процеса от deep link параметри.
    public void completeEmailVerification(String userId, String secret, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, cont) -> {
                        try {
                            return account.updateVerification(userId, secret, cont);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
                Log.d(TAG, "Email verification completed for user: " + userId);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "completeEmailVerification failed: " + e.getMessage());
                if (callback != null) callback.onError(extractErrorMessage(e));
            }
        });
    }
}

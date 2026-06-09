package com.example.aletrail;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LoyaltyCardRepositoryTest {

    private static final long LATCH_TIMEOUT_SEC = 3;

    private Database database;
    private LoyaltyCardRepository repository;
    private LoyaltyCardDAO loyaltyCardDao;
    private VisitDAO visitDao;
    private UserDAO userDao;
    private ExecutorService executorService;
    private String userId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, Database.class)
                .allowMainThreadQueries()
                .build();
        loyaltyCardDao = database.loyaltyCardDAO();
        visitDao = database.visitDAO();
        userDao = database.userDAO();
        executorService = Executors.newSingleThreadExecutor();
        repository = new LoyaltyCardRepository(
                database,
                executorService,
                new GamificationService(context),
                AppwriteService.getInstance(context)
        );

        userId = "guest_test_user";
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setDisplayName("Test User");
        userDao.insert(user);
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
        database.close();
    }

    @Test
    public void processStampFromQR_success_addsStamp() throws InterruptedException {
        LoyaltyCardEntity card = new LoyaltyCardEntity(userId, "brew_success", 5);
        long cardId = loyaltyCardDao.insert(card);

        StampResult result = executeStamp(userId, "brew_success");

        assertNotNull(result.success);
        assertNull(result.error);
        LoyaltyCardEntity updated = loyaltyCardDao.getCardForUserAndBrewerySync(userId, "brew_success");
        assertEquals(1, updated.getStamps());
        assertEquals((int) cardId, updated.getCardId());
    }

    @Test
    public void processStampFromQR_activeCooldown_returnsError() throws InterruptedException {
        LoyaltyCardEntity card = new LoyaltyCardEntity(userId, "brew_cooldown", 5);
        long cardId = loyaltyCardDao.insert(card);

        VisitEntity visit = new VisitEntity();
        visit.setUserId(userId);
        visit.setBreweryId("brew_cooldown");
        visit.setCardId((int) cardId);
        visit.setVisitTimestamp(System.currentTimeMillis() - 1000);
        visitDao.insert(visit);

        StampResult result = executeStamp(userId, "brew_cooldown");

        assertNull(result.success);
        assertNotNull(result.error);
        assertTrue(result.error.contains("cooldown"));
        LoyaltyCardEntity updated = loyaltyCardDao.getCardForUserAndBrewerySync(userId, "brew_cooldown");
        assertEquals(0, updated.getStamps());
    }

    @Test
    public void processStampFromQR_fullCard_returnsError() throws InterruptedException {
        LoyaltyCardEntity card = new LoyaltyCardEntity(userId, "brew_full", 2);
        card.setStamps(2);
        loyaltyCardDao.insert(card);

        StampResult result = executeStamp(userId, "brew_full");

        assertNull(result.success);
        assertNotNull(result.error);
        assertTrue(result.error.contains("пълна"));
        LoyaltyCardEntity updated = loyaltyCardDao.getCardForUserAndBrewerySync(userId, "brew_full");
        assertEquals(2, updated.getStamps());
    }

    @Test
    public void processStampFromQR_invalidFormat_returnsError() throws InterruptedException {
        StampResult result = executeStamp(userId, "INVALID_QR_FORMAT");

        assertNull(result.success);
        assertNotNull(result.error);
        assertTrue(result.error.contains("Нямаш loyalty карта"));
    }

    private StampResult executeStamp(String userId, String breweryId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        repository.processStampFromQR(userId, breweryId, 0.0, 0.0,
                new LoyaltyCardRepository.StampResultCallback() {
                    @Override
                    public void onSuccess(String breweryName) {
                        success.set(breweryName);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                        latch.countDown();
                    }
                });

        boolean completed = latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS);
        assertTrue("Callback timeout", completed);
        return new StampResult(success.get(), error.get());
    }

    private static class StampResult {
        final String success;
        final String error;

        StampResult(String success, String error) {
            this.success = success;
            this.error = error;
        }
    }
}


package com.example.aletrail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class LoyaltyCardDaoTest {

    private Database database;
    private LoyaltyCardDAO loyaltyCardDao;
    private VisitDAO visitDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, Database.class)
                .allowMainThreadQueries()
                .build();
        loyaltyCardDao = database.loyaltyCardDAO();
        visitDao = database.visitDAO();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test(expected = SQLiteConstraintException.class)
    public void insert_duplicateUserAndBrewery_throwsConstraintException() {
        LoyaltyCardEntity first = new LoyaltyCardEntity("u1", "b1", 10);
        loyaltyCardDao.insert(first);

        LoyaltyCardEntity duplicate = new LoyaltyCardEntity("u1", "b1", 10);
        loyaltyCardDao.insert(duplicate);
    }

    @Test
    public void addStamp_whenStampsAtMax_doesNotIncrease() {
        LoyaltyCardEntity card = new LoyaltyCardEntity("u1", "b1", 5);
        card.setStamps(5);
        long cardId = loyaltyCardDao.insert(card);

        loyaltyCardDao.addStamp((int) cardId);

        LoyaltyCardEntity updated = loyaltyCardDao.getCardForUserAndBrewerySync("u1", "b1");
        assertEquals(5, updated.getStamps());
    }

    @Test
    public void getLastVisitTimestampSync_whenNoVisits_returnsNull() {
        Long lastVisit = visitDao.getLastVisitTimestampSync("u1", "b1");
        assertNull(lastVisit);
    }
}


package com.example.aletrail;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationsActivity extends AppCompatActivity {

    private NotificationsAdapter adapter;
    private AppwriteService appwriteService;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        appwriteService = AppwriteService.getInstance(this);
        database = Database.getInstance(this);

        ImageButton backButton = findViewById(R.id.backButton);
        Button markAllReadButton = findViewById(R.id.markAllReadButton);
        RecyclerView notificationsList = findViewById(R.id.notificationsRecyclerView);

        backButton.setOnClickListener(v -> onBackPressed());

        adapter = new NotificationsAdapter(notification -> {
            new Thread(() -> {
                database.notificationDAO().markAsRead(notification.getNotificationId());
            }).start();
        });

        notificationsList.setLayoutManager(new LinearLayoutManager(this));
        notificationsList.setAdapter(adapter);

        String userId = appwriteService.getSavedUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty_fields, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        markAllReadButton.setOnClickListener(v -> {
            new Thread(() -> {
                database.notificationDAO().markAllAsRead(userId);
                runOnUiThread(() -> Toast.makeText(
                        NotificationsActivity.this,
                        R.string.notifications_all_marked_read,
                        Toast.LENGTH_SHORT
                ).show());
            }).start();
        });

        database.notificationDAO().getNotificationsForUser(userId).observe(this, adapter::setNotifications);
    }
}

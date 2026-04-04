package com.example.aletrail;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_history);

        // Скриваме горната системна лента.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ic = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ic.hide(WindowInsetsCompat.Type.statusBars());
        ic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        int cardId = getIntent().getIntExtra("cardId", -1);
        String breweryId = getIntent().getStringExtra("breweryId");

        TextView title = findViewById(R.id.historyTitle);
        TextView emptyText = findViewById(R.id.emptyText);
        RecyclerView recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Хардуерният back да ползва същата анимация.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        Database db = Database.getInstance(this);

        new Thread(() -> {
            // Пробваме да покажем реалното име на пивоварната.
            String breweryName = breweryId;
            try {
                BreweryEntity brewery = db.AleDAO().getAleByIdSync(breweryId);
                if (brewery != null && brewery.getName() != null) {
                    breweryName = brewery.getName();
                }
            } catch (Exception ignored) {}

            List<VisitEntity> visits = db.visitDAO().getVisitsForCardSync(cardId);
            final String finalName = breweryName;

            runOnUiThread(() -> {
                title.setText(getString(R.string.visit_history_title_with_name, finalName));

                if (visits == null || visits.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    return;
                }

                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy — HH:mm", Locale.getDefault());
                // Правим лесни за четене редове за списъка.
                String[] items = new String[visits.size()];
                for (int i = 0; i < visits.size(); i++) {
                    VisitEntity visit = visits.get(i);
                    String dateStr = sdf.format(new Date(visit.getVisitTimestamp()));
                    items[i] = "🍺  Stamp #" + (i + 1) + "\n" + dateStr;
                }

                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                        this, R.layout.item_visit_history, R.id.visitText, items);
                android.widget.ListView listView = new android.widget.ListView(this);
                listView.setAdapter(adapter);
                listView.setDivider(null);

                // Заменяме placeholder RecyclerView с прост ListView.
                recyclerView.setVisibility(View.GONE);
                ((android.widget.LinearLayout) recyclerView.getParent()).addView(listView);
            });
        }).start();
    }

}

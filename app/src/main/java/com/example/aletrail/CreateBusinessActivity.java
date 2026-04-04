package com.example.aletrail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Екран за добавяне на custom бизнес пивоварна + QR за печати.
public class CreateBusinessActivity extends AppCompatActivity {

    private TextInputEditText nameInput, typeInput, streetInput, cityInput,
            stateInput, postalInput, countryInput, phoneInput, websiteInput;
    private Button createButton;
    private Database database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_business);

        // Скриваме системната лента за по-чист екран.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars());
        insetsController.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        database = Database.getInstance(this);
        initViews();
    }

    private void initViews() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        nameInput = findViewById(R.id.businessName);
        typeInput = findViewById(R.id.businessType);
        streetInput = findViewById(R.id.businessStreet);
        cityInput = findViewById(R.id.businessCity);
        stateInput = findViewById(R.id.businessState);
        postalInput = findViewById(R.id.businessPostal);
        countryInput = findViewById(R.id.businessCountry);
        phoneInput = findViewById(R.id.businessPhone);
        websiteInput = findViewById(R.id.businessWebsite);
        createButton = findViewById(R.id.createBusinessButton);

        createButton.setOnClickListener(v -> createBusiness());
    }

    private void createBusiness() {
        String name = getText(nameInput);
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.business_empty_name, Toast.LENGTH_SHORT).show();
            return;
        }

        String type = getText(typeInput);
        if (type.isEmpty()) type = "micro";

        // Правим уникално ID за custom пивоварната.
        String breweryId = "custom_" + UUID.randomUUID().toString().substring(0, 12);

        BreweryEntity brewery = new BreweryEntity();
        brewery.setId(breweryId);
        brewery.setName(name);
        brewery.setBrewery_type(type);
        brewery.setStreet(getText(streetInput));
        brewery.setCity(getText(cityInput));
        brewery.setState(getText(stateInput));
        brewery.setPostal_code(getText(postalInput));
        brewery.setCountry(getText(countryInput));
        brewery.setPhone(getText(phoneInput));
        brewery.setWebsite_url(getText(websiteInput));

        executor.execute(() -> {
            database.AleDAO().insert(brewery);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.business_created, Toast.LENGTH_SHORT).show();
                showBusinessQR(brewery);
            });
        });
    }

    private void showBusinessQR(BreweryEntity brewery) {
        String qrValue = QRCodeService.generateBreweryStampQR(brewery.getId());
        Bitmap qrBitmap = QRCodeService.generateQRCodeBitmap(qrValue, 600, 600);

        if (qrBitmap != null) {
            ImageView qrImageView = new ImageView(this);
            qrImageView.setImageBitmap(qrBitmap);
            qrImageView.setPadding(32, 32, 32, 32);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.business_qr_title))
                    .setMessage(getString(R.string.business_qr_message, brewery.getName()))
                    .setView(qrImageView)
                    .setPositiveButton(R.string.dialog_close, (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            finish();
        }
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}

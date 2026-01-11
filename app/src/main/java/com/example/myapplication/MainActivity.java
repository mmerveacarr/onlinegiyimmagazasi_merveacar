package com.example.myapplication;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    private Spinner spProduct, spColor, spSize, spQty;
    private TextView tvUnitPrice, tvLineTotal;
    private Button btnAddToCart, btnGoCart, btnClearCart;

    // Data
    private final HashMap<String, Integer> priceMap = new HashMap<>();
    private final ArrayList<String> products = new ArrayList<>();
    private final ArrayList<String> colors = new ArrayList<>();
    private final ArrayList<String> sizes = new ArrayList<>();
    private final ArrayList<Integer> qtyList = new ArrayList<>();

    // Calculations
    private int unitPrice = 0;
    private int lineTotal = 0;

    // DB
    private CartDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        spProduct = findViewById(R.id.spProduct);
        spColor = findViewById(R.id.spColor);
        spSize = findViewById(R.id.spSize);
        spQty = findViewById(R.id.spQty);

        tvUnitPrice = findViewById(R.id.tvUnitPrice);
        tvLineTotal = findViewById(R.id.tvLineTotal);

        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnGoCart = findViewById(R.id.btnGoCart);
        btnClearCart = findViewById(R.id.btnClearCart);

        // DB
        dbHelper = new CartDbHelper(this);

        // Prepare lists & prices
        setupData();

        // Bind spinners
        bindSpinners();

        // Seçimler değişince anında hesapla
        AdapterView.OnItemSelectedListener recalcListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spProduct.setOnItemSelectedListener(recalcListener);
        spColor.setOnItemSelectedListener(recalcListener);
        spSize.setOnItemSelectedListener(recalcListener);
        spQty.setOnItemSelectedListener(recalcListener);

        // Buttons
        btnAddToCart.setOnClickListener(v -> addToCart());

        btnGoCart.setOnClickListener(v -> {
            // Şimdilik sepet bilgisini gösteriyoruz.
            // Bir sonraki adımda CartActivity yazınca buradan oraya geçeceğiz.
            int count = getCartCount();
            int total = getCartTotal();
            Toast.makeText(this,
                    "Sepette " + count + " ürün var. Toplam: " + total + " TL\n(Sepet ekranını bir sonraki adımda yapıyoruz)",
                    Toast.LENGTH_LONG).show();
        });

        btnClearCart.setOnClickListener(v -> confirmClearCart());

        // Initial calc
        calculate();
    }

    private void setupData() {
        // Ürünler + fiyatlar (senin verdiğin fiyatlar)
        products.clear();
        products.add("Ürün seçiniz...");
        products.addAll(Arrays.asList(
                "Gömlek", "Pantolon", "Kazak", "Etek", "Elbise", "Tişört", "Hırka"
        ));

        priceMap.clear();
        priceMap.put("Gömlek", 250);
        priceMap.put("Pantolon", 550);
        priceMap.put("Kazak", 400);
        priceMap.put("Etek", 300);
        priceMap.put("Elbise", 500);
        priceMap.put("Tişört", 100);
        priceMap.put("Hırka", 350);

        // Renkler
        colors.clear();
        colors.add("Renk seçiniz...");
        colors.addAll(Arrays.asList("Siyah", "Beyaz", "Kırmızı", "Mavi"));

        // Beden
        sizes.clear();
        sizes.add("Beden seçiniz...");
        sizes.addAll(Arrays.asList("S", "M", "L"));

        // Adet 1-10
        qtyList.clear();
        qtyList.add(0); // seçiniz gibi davranacak
        for (int i = 1; i <= 10; i++) qtyList.add(i);
    }

    private void bindSpinners() {
        ArrayAdapter<String> pAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, products);
        pAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProduct.setAdapter(pAdapter);

        ArrayAdapter<String> cAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colors);
        cAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spColor.setAdapter(cAdapter);

        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizes);
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSize.setAdapter(sAdapter);

        ArrayAdapter<Integer> qAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, qtyList);
        qAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQty.setAdapter(qAdapter);

        // Defaults
        spProduct.setSelection(0);
        spColor.setSelection(0);
        spSize.setSelection(0);
        spQty.setSelection(0);
    }

    private void calculate() {
        String product = safeString(spProduct.getSelectedItem());
        int qty = safeInt(spQty.getSelectedItem());

        unitPrice = priceMap.containsKey(product) ? priceMap.get(product) : 0;
        lineTotal = unitPrice * qty;

        tvUnitPrice.setText(String.format(Locale.getDefault(), "Birim Fiyat: %d TL", unitPrice));
        tvLineTotal.setText(String.format(Locale.getDefault(), "Satır Toplam: %d TL", lineTotal));
    }

    private void addToCart() {
        // Validasyon
        if (spProduct.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Lütfen ürün seç!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spColor.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Lütfen renk seç!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spSize.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Lütfen beden seç!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spQty.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Lütfen adet seç!", Toast.LENGTH_SHORT).show();
            return;
        }

        String product = safeString(spProduct.getSelectedItem());
        String color = safeString(spColor.getSelectedItem());
        String size = safeString(spSize.getSelectedItem());
        int qty = safeInt(spQty.getSelectedItem());

        // Recalc (garanti)
        unitPrice = priceMap.containsKey(product) ? priceMap.get(product) : 0;
        lineTotal = unitPrice * qty;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("product", product);
        cv.put("color", color);
        cv.put("size", size);
        cv.put("qty", qty);
        cv.put("unit_price", unitPrice);
        cv.put("line_total", lineTotal);

        long insertedId = db.insert("cart_items", null, cv);
        db.close();

        if (insertedId > 0) {
            Toast.makeText(this, "Sepete eklendi ✅ (+" + lineTotal + " TL)", Toast.LENGTH_SHORT).show();

            // Seçimleri sıfırla
            spProduct.setSelection(0);
            spColor.setSelection(0);
            spSize.setSelection(0);
            spQty.setSelection(0);
            calculate();
        } else {
            Toast.makeText(this, "Sepete eklenemedi ❌", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearCart() {
        new AlertDialog.Builder(this)
                .setTitle("Sepeti Temizle")
                .setMessage("Sepetteki tüm ürünler silinecek. Emin misin?")
                .setPositiveButton("Evet, temizle", (dialog, which) -> clearCart())
                .setNegativeButton("İptal", null)
                .show();
    }

    private void clearCart() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete("cart_items", null, null);
        db.close();

        Toast.makeText(this, "Sepet temizlendi ✅ (" + deleted + " ürün silindi)", Toast.LENGTH_SHORT).show();
    }

    private int getCartCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM cart_items", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    private int getCartTotal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(line_total) FROM cart_items", null);
        int total = 0;
        if (c.moveToFirst()) {
            if (!c.isNull(0)) total = c.getInt(0);
        }
        c.close();
        db.close();
        return total;
    }

    private String safeString(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }

    private int safeInt(Object o) {
        try {
            if (o == null) return 0;
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================
    // SQLite Helper (tek tablo: cart_items)
    // =========================
    static class CartDbHelper extends SQLiteOpenHelper {

        public CartDbHelper(MainActivity ctx) {
            super(ctx, "ShopDB.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cart_items (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "product TEXT," +
                            "color TEXT," +
                            "size TEXT," +
                            "qty INTEGER," +
                            "unit_price INTEGER," +
                            "line_total INTEGER" +
                            ")"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS cart_items");
            onCreate(db);
        }
    }
}

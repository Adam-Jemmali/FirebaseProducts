package com.example.firebase;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    DatabaseReference databaseProducts;
    RecyclerView recyclerView;
    List<Product> products;
    ProductAdapter adapter;
    EditText editTextName, editTextPrice;
    Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        databaseProducts = FirebaseDatabase.getInstance().getReference("products");

        editTextName = findViewById(R.id.editTextName);
        editTextPrice = findViewById(R.id.editTextPrice);
        addButton = findViewById(R.id.addButton);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        products = new ArrayList<>();
        adapter = new ProductAdapter(this, products);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(product -> showUpdateDialog(product));

        addButton.setOnClickListener(v -> addProduct());
    }

    private void showUpdateDialog(Product product) {
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.update_dialog, null);
        dialogBuilder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editUpdateName);
        EditText editPrice = dialogView.findViewById(R.id.editUpdatePrice);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        Button btnDelete=  dialogView.findViewById(R.id.btnDelete);

        editName.setText(product.getProductName());
        editPrice.setText(String.valueOf(product.getPrice()));

        dialogBuilder.setTitle("Update Product");
        android.app.AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        btnUpdate.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newPriceText = editPrice.getText().toString().trim();

            if (newName.isEmpty() || newPriceText.isEmpty()) {
                Toast.makeText(MainActivity.this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double newPrice = Double.parseDouble(newPriceText);
            updateProduct(product.getId(), newName, newPrice);
            alertDialog.dismiss();
        });
        btnDelete.setOnClickListener(v -> {
            deleteProduct(product.getId());
            alertDialog.dismiss();
        });
    }


    private void updateProduct(String id, String name, double price) {
        // Get a reference to the specific product in Firebase
        DatabaseReference dR = FirebaseDatabase.getInstance()
                .getReference("products").child(id);

        // Create a new Product object with the updated values
        Product product = new Product(id, name, price);

        // Set the new value in Firebase
        dR.setValue(product);

        // Show a quick confirmation
        Toast.makeText(getApplicationContext(), "Product Updated", Toast.LENGTH_LONG).show();
    }



    private void addProduct() {
        String name = editTextName.getText().toString().trim();
        String priceText = editTextPrice.getText().toString().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceText);
        String id = UUID.randomUUID().toString();

        Product product = new Product(id, name, price);
        databaseProducts.child(id).setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Product added!", Toast.LENGTH_SHORT).show();
                    editTextName.setText("");
                    editTextPrice.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void deleteProduct(String id) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("products").child(id);
        dR.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(MainActivity.this, "Product deleted", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        databaseProducts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                products.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Product product = postSnapshot.getValue(Product.class);
                    if (product != null) {
                        products.add(product);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

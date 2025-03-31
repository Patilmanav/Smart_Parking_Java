package com.example.smart_parking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_parking.Adapter.LocationAdapter;
import com.example.smart_parking.Model.ParkingLocation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private FirebaseFirestore db;
    private ListView locationsList;
    private List<ParkingLocation> locations;
    private LocationAdapter locationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        locations = new ArrayList<>();
        
        // Initialize views
        locationsList = findViewById(R.id.locationsList);
        FloatingActionButton addLocation = findViewById(R.id.addLocation);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Setup adapter
        locationAdapter = new LocationAdapter(this, locations);
        locationsList.setAdapter(locationAdapter);

        // Set long click listener for editing locations
        locationAdapter.setOnLocationLongClickListener(location -> showEditLocationDialog(location));

        // Load existing locations
        loadLocations();

        // Add location button click listener
        addLocation.setOnClickListener(v -> showAddLocationDialog());

        // Set click listener for logout button
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Clear user data
                MainActivity.log_username = null;
                MainActivity.log_password = null;
                
                // Return to login screen
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void loadLocations() {
        db.collection("locations")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                locations.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ParkingLocation location = document.toObject(ParkingLocation.class);
                    location.setLocationId(document.getId());
                    locations.add(location);
                }
                locationAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading locations", e);
                Toast.makeText(AdminActivity.this, "Error loading locations", Toast.LENGTH_SHORT).show();
            });
    }

    private void showAddLocationDialog() {
        showLocationDialog(null);
    }

    private void showEditLocationDialog(ParkingLocation location) {
        showLocationDialog(location);
    }

    private void showLocationDialog(ParkingLocation existingLocation) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_location, null);
        dialog.setView(dialogView);

        EditText nameInput = dialogView.findViewById(R.id.locationName);
        EditText addressInput = dialogView.findViewById(R.id.locationAddress);
        EditText slotsInput = dialogView.findViewById(R.id.totalSlots);
        EditText rateInput = dialogView.findViewById(R.id.hourlyRate);
        EditText mapsLinkInput = dialogView.findViewById(R.id.mapsLink);
        Button actionButton = dialogView.findViewById(R.id.addButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set dialog title and button text based on whether we're adding or editing
        String dialogTitle = existingLocation != null ? "Edit Location" : "Add Location";
        String buttonText = existingLocation != null ? "Update" : "Add Location";
        dialog.setTitle(dialogTitle);
        actionButton.setText(buttonText);

        // If editing, populate fields with existing data
        if (existingLocation != null) {
            nameInput.setText(existingLocation.getName());
            addressInput.setText(existingLocation.getAddress());
            slotsInput.setText(String.valueOf(existingLocation.getTotalSlots()));
            rateInput.setText(String.valueOf(existingLocation.getHourlyRate()));
            mapsLinkInput.setText(existingLocation.getMapsLink());
        }

        // Set hint for rate input to indicate INR
        rateInput.setHint("Hourly Rate (₹)");

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();

        actionButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String address = addressInput.getText().toString().trim();
            String slotsStr = slotsInput.getText().toString().trim();
            String rateStr = rateInput.getText().toString().trim();
            String mapsLink = mapsLinkInput.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty() || slotsStr.isEmpty() || rateStr.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int totalSlots = Integer.parseInt(slotsStr);
                double hourlyRate = Double.parseDouble(rateStr);

                ParkingLocation location = existingLocation != null ? existingLocation : 
                    new ParkingLocation(name, address, totalSlots, hourlyRate);
                
                location.setName(name);
                location.setAddress(address);
                location.setTotalSlots(totalSlots);
                location.setHourlyRate(hourlyRate);
                location.setMapsLink(mapsLink);

                if (existingLocation != null) {
                    // Update existing location
                    db.collection("locations")
                        .document(location.getLocationId())
                        .set(location)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminActivity.this, "Location updated successfully", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            loadLocations();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminActivity.this, "Error updating location", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Add new location
                    db.collection("locations")
                        .add(location)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(AdminActivity.this, "Location added successfully", Toast.LENGTH_SHORT).show();
                            alertDialog.dismiss();
                            loadLocations();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminActivity.this, "Error adding location", Toast.LENGTH_SHORT).show();
                        });
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for slots and rate", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());
    }

}
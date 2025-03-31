package com.example.smart_parking;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_parking.Adapter.VehicleAdapter;
import com.example.smart_parking.Model.UserProfile;
import com.example.smart_parking.Model.Vehicle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView nameText, emailText, phoneText;
    private ListView vehiclesList;
    private List<Vehicle> vehicles;
    private VehicleAdapter vehicleAdapter;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseFirestore.getInstance();
        username = MainActivity.log_username;
        vehicles = new ArrayList<>();

        // Initialize views
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        phoneText = findViewById(R.id.phoneText);
        vehiclesList = findViewById(R.id.vehiclesList);
        FloatingActionButton addVehicle = findViewById(R.id.addVehicle);

        // Setup adapter
        vehicleAdapter = new VehicleAdapter(this, vehicles);
        vehiclesList.setAdapter(vehicleAdapter);

        // Load user profile
        loadUserProfile();

        // Load vehicles
        loadVehicles();

        // Add vehicle button click listener
        addVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddVehicleDialog();
            }
        });
    }

    private void loadUserProfile() {
        db.collection("users")
            .document(username)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                    if (userProfile != null) {
                        nameText.setText(userProfile.getFirstName() + " " + userProfile.getLastName());
                        emailText.setText(userProfile.getEmail());
                        phoneText.setText(userProfile.getPhoneNumber());
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(UserProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadVehicles() {
        db.collection("users")
            .document(username)
            .collection("vehicles")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                vehicles.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Vehicle vehicle = document.toObject(Vehicle.class);
                    vehicle.setVehicleId(document.getId());
                    vehicles.add(vehicle);
                }
                vehicleAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(UserProfileActivity.this, "Error loading vehicles", Toast.LENGTH_SHORT).show();
            });
    }

    private void showAddVehicleDialog() {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.vehicle_registration, null);
        dialog.setView(dialogView);
        final android.app.AlertDialog Adialog = dialog.create();

        EditText vehicleName = dialogView.findViewById(R.id.vehicleName);
        EditText vehicleNumber = dialogView.findViewById(R.id.vehicleNumber);
        EditText vehicleType = dialogView.findViewById(R.id.vehicleType);
        Button addVehicle = dialogView.findViewById(R.id.addVehicle);

        addVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateVehicleFields(vehicleName, vehicleNumber, vehicleType)) {
                    Vehicle vehicle = new Vehicle(
                        vehicleName.getText().toString(),
                        vehicleNumber.getText().toString(),
                        vehicleType.getText().toString()
                    );

                    db.collection("users")
                        .document(username)
                        .collection("vehicles")
                        .add(vehicle)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(UserProfileActivity.this, "Vehicle added successfully", Toast.LENGTH_SHORT).show();
                            Adialog.dismiss();
                            loadVehicles();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserProfileActivity.this, "Failed to add vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            }
        });

        Adialog.show();
    }

    private boolean validateVehicleFields(EditText vehicleName, EditText vehicleNumber, EditText vehicleType) {
        if (vehicleName.getText().toString().trim().isEmpty()) {
            vehicleName.setError("Vehicle name is required");
            return false;
        }
        if (vehicleNumber.getText().toString().trim().isEmpty()) {
            vehicleNumber.setError("Vehicle number is required");
            return false;
        }
        if (vehicleType.getText().toString().trim().isEmpty()) {
            vehicleType.setError("Vehicle type is required");
            return false;
        }
        return true;
    }
} 
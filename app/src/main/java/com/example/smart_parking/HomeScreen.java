package com.example.smart_parking;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.*;
import android.view.View;
import android.graphics.Color;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeScreen extends AppCompatActivity {
    public String uname,upass,dataTime;
    Button s1,s2,s3,s4,s5,s6,sub_details;
    EditText v_name,v_number;
    EditText timePickerHours,timePickerMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);


        uname = MainActivity.log_username;
        upass = MainActivity.log_password;
        s1 = findViewById(R.id.button1);
        s2 = findViewById(R.id.button2);
        s3 = findViewById(R.id.button3);
        s4 = findViewById(R.id.button4);
        s5 = findViewById(R.id.button5);
        s6 = findViewById(R.id.button6);
        buttonClick();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List <Button> list = new ArrayList<Button>();
        list.add(s1);
        list.add(s2);
        list.add(s3);
        list.add(s4);
        list.add(s5);
        list.add(s6);
//        db.collection("parking_slots").count()

                        
        db.collection("parking_slots").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + document.getData());

                                for(int i=0;i<list.size();i++){
                                    if (list.get(i).getText().toString().trim().equals(document.getId())){
                                        if(Objects.equals(document.getString("booking_status"), "Booked")){
                                            list.get(i).setBackgroundColor(Color.RED);
                                            break;
                                        }
                                    }
                                }

                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    protected void getDetails(Button btn){
        Log.d("MyApp",btn.getText()+" Tapped");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.get_details, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();
        v_name = dialogView.findViewById(R.id.vehiclename);
        v_number = dialogView.findViewById(R.id.vehicleno);
        timePickerHours = dialogView.findViewById(R.id.etHours);
        timePickerMin = dialogView.findViewById(R.id.etMin);
        sub_details = dialogView.findViewById(R.id.detail_submit);

        sub_details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyApp","on Clicked");
                boolean isVNameNotEmpty = false,isVNoNotEmpty = false;

                if (v_name.getText().toString().trim().isEmpty()) {

                    Snackbar snackbar = Snackbar.make(view, "Please fill out these fields",
                            Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
                    snackbar.show();
                    v_name.setError("Vehicle Name should not be empty");
                } else {
                    //Here you can write the codes for checking username
                    isVNameNotEmpty = true;
                }
                if (v_number.getText().toString().trim().isEmpty()) {

                    Snackbar snackbar = Snackbar.make(view, "Please fill out these fields",
                            Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
                    snackbar.show();
                    v_number.setError("Vehicle Number should not be empty");
                } else {
                    //Here you can write the codes for checking username
                    isVNoNotEmpty = true;
                }

                if(isVNameNotEmpty && isVNoNotEmpty){
                    Adialog.dismiss();
                    Toast.makeText(HomeScreen.this,"Vehicle Details Captured",Toast.LENGTH_LONG).show();
                    Log.d("MyApp","Successful");
                    Log.d("MyApp","Vehicle Name = "+v_name.getText().toString());
                    Log.d("MyApp","Vehicle No = "+v_number.getText().toString());
                    Log.d("MyApp","Time = "+timePickerHours.getText().toString()+"Hours"+timePickerMin.getText().toString()+"Min");
                    btn.setBackgroundColor(Color.RED);

                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat;
                    simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss aaa");
                    dataTime = simpleDateFormat.format(calendar.getTime()).toString();
                    // Create a new user with a first and last name
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("username",uname);
                    slot.put("vehicle_name", v_name.getText().toString());
                    slot.put("vehicle_number", v_number.getText().toString());
                    slot.put("timeHours", timePickerHours.getText().toString());
                    slot.put("timeMin", timePickerMin.getText().toString());
                    slot.put("booking_status", "Booked");
                    slot.put("TimeStamp",dataTime);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    // Add a new document with a generated ID
                    db.collection("parking_slots")
                            .document(btn.getText().toString().trim())
                            .set(slot)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("Smart_Parking", "DocumentSnapshot added with ID: " + btn.getText().toString());

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("Smart_Parking", "Error adding document", e);
                                }
                            });

                }
            }
        });

        Adialog.show();


    }

    private void alreadyBooked(Button btn){
        Log.d("MyApp",btn.getText()+" Tapped");

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.already_booked, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();

        sub_details = dialogView.findViewById(R.id.detail_submit);

        sub_details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyApp","on Clicked");
                Adialog.dismiss();

            }
        });

        Adialog.show();

    }
    private void cancelBooking(Button btn){

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.cancel_booking, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();
        TextView u_name = dialogView.findViewById(R.id.u_name);
        TextView showTime = dialogView.findViewById(R.id.show_time);
        TextView Timestamp = dialogView.findViewById(R.id.timeStamp);

        Button checkout = dialogView.findViewById(R.id.checkout_button);
        Button close = dialogView.findViewById(R.id.cancel_button);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("parking_slots").document(btn.getText().toString()).get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){

                                    u_name.setText("USERNAME: "+task.getResult().getString("username").trim());
                                    Timestamp.setText("TimeStamp: "+task.getResult().getString("TimeStamp").trim());
                                    showTime.setText("Booked For: "+task.getResult().getString("timeHours").trim()+"Hours"+
                                            task.getResult().getString("timeMin").trim()+"Min");
                                }
                            }
                        });

        checkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btn.setBackgroundColor(Color.parseColor("#006600"));
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("booking_status","null");
                db.collection("parking_slots").document(btn.getText().toString()).set(data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("Update","Success");
                                Adialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("Update","Failed!!");

                            }
                        });
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyApp","on Clicked");
                Adialog.dismiss();

            }
        });

        Adialog.show();
    }

    private void checkBookingStatus(Button btn,View v){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("parking_slots").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean not_booked = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + btn.getText().toString().trim());

                                if (btn.getText().toString().trim().equals(document.getId())){
                                    if(Objects.equals(document.getString("booking_status"), "Booked")){
                                        Log.d("Username","Booked User "+document.getString("username") + uname);
                                        if(Objects.equals(document.getString("username"), uname)){
                                            cancelBooking(btn);
                                        }
                                        else {
                                            alreadyBooked(btn);
                                        }

                                        Log.d("TAG", "AlreadyBooked");

                                        not_booked = false;
                                        break;
                                    }
                                    else {
                                        not_booked = true;
                                    }

                                }
                                Log.d("not_booked", String.valueOf(not_booked));
                            }
                            if(not_booked){
                                Log.d("TAG", "NOT AlreadyBooked");

                                getDetails(btn);
                            }

                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MyApp","Failed to check");
                    }
                });
    }
    private void buttonClick(){
        s1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 1 Clicked");
                checkBookingStatus(s1,v);
            }
        });
        s2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 2 Clicked");
                checkBookingStatus(s2,v);
            }
        });
        s3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 3 Clicked");
                checkBookingStatus(s3,v);
            }
        });
        s4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 4 Clicked");
                checkBookingStatus(s4,v);
            }
        });
        s5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 5 Clicked");
                checkBookingStatus(s5,v);

            }
        });
        s6.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("MyApp","Slot 6 Clicked");
                checkBookingStatus(s6,v);
            }
        });

    }
}
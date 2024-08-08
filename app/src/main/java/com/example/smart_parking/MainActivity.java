package com.example.smart_parking;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    public static String log_username;
    public static String log_password;
    EditText username, password, reg_username, reg_password,
            reg_firstName, reg_lastName, reg_email, reg_confirmemail;
    Button login, signUp, reg_register;
    CheckBox rememberMe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        signUp = findViewById(R.id.signUp);
//        rememberMe = findViewById(R.id.rememberMe);


        ClickLogin();


        //SignUp's Button for showing registration page
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClickSignUp();
            }
        });


    }

    //This is method for doing operation of check login
    private void ClickLogin() {
        Map<String, Object> docDatalist = new HashMap<>();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("TAG", document.getId() + " => " + document.getString("password"));
                                docDatalist.put(document.getString("username"),document.getString("password"));

                                Log.d("list",String.format("%s = %s",docDatalist.size(), docDatalist));
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final boolean[] valid_uname = {false};
                final boolean[] valid_pass = {false};



                if (username.getText().toString().trim().isEmpty()) {

                    Snackbar snackbar = Snackbar.make(view, "Please fill out these fields",
                            Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
                    snackbar.show();
                    username.setError("Username should not be empty");
                } else {
                    //Here you can write the codes for checking username
                        Log.d("Else","hHellooo..."+ docDatalist.size());

                        for(Map.Entry m : docDatalist.entrySet()){
                            Log.d("Check uname",username.getText().toString().trim()+"=="+m.getKey()+"="+m.getValue());
                            if(username.getText().toString().trim().equals(m.getKey())){
                                Log.d("valid_uname", String.valueOf(valid_uname[0]));
                                valid_uname[0] = true;
                                Log.d("valid_uname", String.valueOf(valid_uname[0]));


                                break;

                            }
                        }

                }
                if (password.getText().toString().trim().isEmpty()) {
                    Snackbar snackbar = Snackbar.make(view, "Please fill out these fields",
                            Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(getResources().getColor(R.color.red));
                    snackbar.show();
                    password.setError("Password should not be empty");
                } else {
                    //Here you can write the codes for checking password
                    if(valid_uname[0]){
                        docDatalist.get(username.getText().toString().trim());
                        valid_pass[0] = true;
                        log_username = username.getText().toString();
                        log_password = password.getText().toString();


                    }
                }


                Log.d("MyApp", Arrays.toString(valid_uname)+" = "+valid_uname[0]);
                Log.d("MyApp", Arrays.toString(valid_pass) + " = " + valid_pass[0]);

                if(valid_uname[0] && valid_pass[0]){
                    Intent intent = new Intent(MainActivity.this, HomeScreen.class);
                    startActivity(intent);
                }

            }

        });

    }

    //The method for opening the registration page and another processes or checks for registering
    private void ClickSignUp() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register, null);
        dialog.setView(dialogView);
        final AlertDialog Adialog = dialog.create();
        reg_username = dialogView.findViewById(R.id.reg_username);
        reg_password = dialogView.findViewById(R.id.reg_password);
        reg_firstName = dialogView.findViewById(R.id.reg_firstName);
        reg_lastName = dialogView.findViewById(R.id.reg_lastName);
        reg_email = dialogView.findViewById(R.id.reg_contact);
        reg_confirmemail = dialogView.findViewById(R.id.reg_confirmcontact);
        reg_register = dialogView.findViewById(R.id.reg_register);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        reg_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean isValidUser = false;
                if (reg_username.getText().toString().trim().isEmpty()) {

                    reg_username.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    isValidUser = true;
                }
                if (reg_password.getText().toString().trim().isEmpty()) {
                    reg_password.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    isValidUser = true;
                }
                if (reg_firstName.getText().toString().trim().isEmpty()) {
                    reg_firstName.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    //Here you can write the codes for checking firstname
                    isValidUser = true;

                }
                if (reg_lastName.getText().toString().trim().isEmpty()) {
                    reg_lastName.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    isValidUser = true;
                }
                if (reg_email.getText().toString().trim().isEmpty()) {

                    reg_email.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    //Here you can write the codes for checking email
                    isValidUser = true;
                }
                if (reg_confirmemail.getText().toString().trim().isEmpty()) {

                    reg_confirmemail.setError("Please fill out this field");
                    isValidUser = false;
                } else {
                    //Here you can write the codes for checking confirmemail
                    isValidUser = true;
                }
                if(isValidUser){
                    Toast.makeText(MainActivity.this,"Successfully Registered",Toast.LENGTH_SHORT).show();
                    // Create a new user with a first and last name
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", reg_username.getText().toString());
                    user.put("password", reg_password.getText().toString());
                    user.put("first_name", reg_firstName.getText().toString());
                    user.put("last_name", reg_lastName.getText().toString());
                    user.put("contact", reg_email.getText().toString());


                    // Add a new document with a generated ID
                    db.collection("users")
                            .document(reg_username.getText().toString())
                            .set(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("Smart_Parking", "DocumentSnapshot added with ID: " + reg_username.getText().toString());

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


}

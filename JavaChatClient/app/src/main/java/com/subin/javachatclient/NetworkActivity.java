package com.subin.javachatclient;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.io.File;

public class NetworkActivity extends CommonActivity {

//    Defining strings to store protocol string, user name and user id received from previous activity
    String protocol;
    String userId;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

//        Storing the values received from previous Activity
        protocol = getIntent().getExtras().getString("protocol");
        userId = getIntent().getExtras().getString("userId");
        name = getIntent().getExtras().getString("name");

//        Showing message at bottom when user is logged in
        if (!name.equals("")){
            showMessage("Logged In as "+name);
        }

    }

//    Creating a method connectClicked to verify the inputs provided on IP Address field and port number field and navigating to main activity
    public void connectClicked(View view){
        EditText ip = findViewById(R.id.ipAddress);
        EditText portNum = findViewById(R.id.portNumber);

        if (ip.getText().toString().equals("")){
            showError("IP Address is Required.");
        }

        else if (portNum.getText().toString().equals("")){
            showError("Port Number is Required.");
        }

        else if (Integer.parseInt(portNum.getText().toString()) > 65535){
            showError("Invalid Port Number.");
        }

        else {
//            Proceeding to main activity with the selected protocol, user name, user id, IP Address and port numbers as extra information
            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
            mainActivity.putExtra("protocol", protocol);
            mainActivity.putExtra("port", portNum.getText().toString());
            mainActivity.putExtra("ip", ip.getText().toString());
            mainActivity.putExtra("userName", name);
            mainActivity.putExtra("userId", userId);
            startActivity(mainActivity);
        }
    }

//    Creating method loginToAnother to allow user to login with another account
    public void loginAnother(View view){
//        Deleting the existing local database file
        File dbFile = new File(getFilesDir()+"/userData.db");
        SQLiteDatabase.deleteDatabase(dbFile);

//        Proceeding to login activity
        Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
        loginActivity.putExtra("protocol",protocol);
        startActivity(loginActivity);
    }

//    Going back to opening activity when back button is pressed
    @Override
    public void onBackPressed() {
        Intent openingActivity = new Intent(getApplicationContext(), OpeningActivity.class);
        startActivity(openingActivity);
        finish();
    }
}

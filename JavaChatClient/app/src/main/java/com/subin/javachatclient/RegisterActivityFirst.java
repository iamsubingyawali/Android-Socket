package com.subin.javachatclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class RegisterActivityFirst extends CommonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_first);
    }

//    Creating method goRegisterSecond to verify the inputs on first activity to register
//    and go to the second activity for registration
    public void goRegisterSecond(View view){
//        Getting the EditText fields for firstname, lastname and email address
//        and verifying the inputs provided
        EditText firstName = findViewById(R.id.firstname);
        EditText lastName = findViewById(R.id.lastname);
        EditText email = findViewById(R.id.email);

        if (firstName.getText().toString().equals("")){
            showError("First Name Can't Be Empty.");
        }

        else if (lastName.getText().toString().equals("")){
            showError("Last Name Can't Be Empty.");
        }

        else if (email.getText().toString().equals("")){
            showError("Email Can't Be Empty.");
        }

        else{
//            Proceeding to second activity for registration where password is allowed to enter
            Intent regSecondActivity = new Intent(getApplicationContext(),RegisterActivitySecond.class);
            regSecondActivity.putExtra("firstName",firstName.getText().toString());
            regSecondActivity.putExtra("lastName",lastName.getText().toString());
            regSecondActivity.putExtra("email",email.getText().toString());
            startActivity(regSecondActivity);
        }
    }
}

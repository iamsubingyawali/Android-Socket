package com.subin.javachatclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class RegisterActivitySecond extends CommonActivity {

//    Defining strings to store user deatils such as firstname, lastname, emails(Recievd from previous activity)
//    and password
    String firstName;
    String lastName;
    String email;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_second);

//        Getting the firstName,lastName and email address value from previous activity and storing them
        firstName = getIntent().getExtras().getString("firstName");
        lastName = getIntent().getExtras().getString("lastName");
        email = getIntent().getExtras().getString("email");
    }

//    Creating a method registerClicked to be called when Register button is clicked
//    To register the user or to insert the user infoemation to the database
    public void registerClicked(View view){
//        Getting the inputs from the password and confirm password fields and verifying the inputs provided
        EditText pass = findViewById(R.id.password);
        EditText confPass = findViewById(R.id.confPassword);

        if (pass.getText().toString().equals("")){
            showError("Password Can't Be Empty.");
        }

        else if (confPass.getText().toString().equals("")){
            showError("You Must Confirm Your Password.");
        }

        else if (!pass.getText().toString().equals(confPass.getText().toString())){
            showError("Passwords Didn't Match.");
        }

        else{
            password = pass.getText().toString();

//            Creating a new thread to run the code to communicate with the server for registration
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        Creating a url path with the details of the user inside it as query strings
                        String serverPath = "http://"+dbIp+"/JavaChat/db.php?task=reg&firstname="+firstName+"&lastname="+lastName+"&email="+email+"&password="+password;
//                        Creating URL object using the path
                        URL pathUrl = new URL(serverPath);
//                        Creating UrlConnection object and opening the connection
                        URLConnection connection =pathUrl.openConnection();
//                        Typecasting the URLConnection object to HttpUrlConnection object
                        HttpURLConnection httpConnection = (HttpURLConnection) connection;

//                        Getting the response from the server if the server is reachable and working fine
                        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                            InputStream response =httpConnection.getInputStream();
                            InputStreamReader reader =new InputStreamReader(response);
                            BufferedReader buffer = new BufferedReader(reader);

                            String responseText = buffer.readLine();

//                            If the server responds with the status true
//                            proceeding to the login activity and showing the user registered message inside the login activity
                            if (responseText.equals("true")){
                                Intent loginActivity = new Intent(getApplicationContext(),LoginActivity.class);
                                loginActivity.putExtra("msg","User Registered.");
                                startActivity(loginActivity);
                            }
                            else{
                                showError("An Error Occurred.Try Again.");
                            }
                        }
                    }
                    catch(Exception e){
                        showError("An Error Occurred.Try Again.");
                    }
                }
//                Starting the thread
            }).start();
        }
    }
}

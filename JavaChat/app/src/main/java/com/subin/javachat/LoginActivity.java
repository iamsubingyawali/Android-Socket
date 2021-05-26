package com.subin.javachat;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class LoginActivity extends CommonActivity {

//  Creating strings to store protocol selected, email address of user and user password
    String protocol;
    String email;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        Getting protocol selected from the previous activity
        protocol = getIntent().getExtras().getString("protocol");
    }

//   Creating a function loginClicked for Logging in the user when login button is clicked
    public void loginClicked(View view){
//        Getting values from the text fields and verifying for valid values
        EditText emailField = findViewById(R.id.email);
        EditText pass = findViewById(R.id.password);

        email = emailField.getText().toString();
        password = pass.getText().toString();

        if (email.equals("")){
            showError("Email Can't Be Empty");
        }

        else if (password.equals("")){
            showError("Password Can't Be Empty");
        }

        else{
//            Creating a new thread to run the code to connect to the remote database inside it
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        Creating a URL string with the query string to verify the email and password provided using remote database server
                        String serverPath = "http://"+dbIp+"/JavaChat/db.php?task=login&email="+email+"&password="+password+"&type=server";
                        URL pathUrl = new URL(serverPath);
                        URLConnection connection =pathUrl.openConnection();
                        HttpURLConnection httpConnection = (HttpURLConnection) connection;

//                        Getting the user information from the database if provided credentials are correct and server responds with response code OK
                        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                            InputStream response =httpConnection.getInputStream();
                            InputStreamReader reader =new InputStreamReader(response);
                            BufferedReader buffer = new BufferedReader(reader);

                            String responseText = buffer.readLine();

                            JSONObject details = new JSONObject(responseText);
                            String status = details.get("status").toString();

                            if (status.equals("true")){
                                String id = details.get("id").toString();
                                String firstname = details.get("firstname").toString();
                                String lastname = details.get("lastname").toString();
                                String name = firstname+" "+lastname;

                                File dbFile = new File(getFilesDir()+"/userData.db");

//                                Creating a new local sqlite database file and storing the provided credentials to allow user to directly
//                                login to the app for future use of the app
                                if (!dbFile.exists()) {
                                    SQLiteDatabase localDatabase = SQLiteDatabase.openOrCreateDatabase(getFilesDir() + "/userData.db", null);
                                    String createQuery = "CREATE TABLE users(id text,firstname text,lastname text)";
                                    localDatabase.execSQL(createQuery);

                                    String insertQuery = "INSERT INTO users VALUES('"+id+"','"+firstname+"','"+lastname+"')";
                                    localDatabase.execSQL(insertQuery);
                                    localDatabase.close();

//                                    Proceeding to the network activity with the selected protocol, user name and user id
                                    Intent networkActivity = new Intent(getApplicationContext(), NetworkActivity.class);
                                    networkActivity.putExtra("protocol", protocol);
                                    networkActivity.putExtra("userId", id);
                                    networkActivity.putExtra("name", name);

                                    startActivity(networkActivity);
                                }
                                else{
                                    showError("Application Error.Try Reinstalling This App.");
                                }
                            }
                            else{
                                showError("Wrong Email Or Password.");
                            }
                        }
                    }
                    catch(Exception e){
                        showError("Connection Error Occurred.Try Again.");
                    }
                }
//                Running the thread
            }).start();
        }
    }

//    Going back to the openingActivity when back button is pressed
    @Override
    public void onBackPressed() {
        Intent openingActivity = new Intent(getApplicationContext(), OpeningActivity.class);
        startActivity(openingActivity);
        finish();
    }
}

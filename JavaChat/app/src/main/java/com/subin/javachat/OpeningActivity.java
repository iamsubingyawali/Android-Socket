package com.subin.javachat;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import java.io.File;

public class OpeningActivity extends CommonActivity {

//    Defining strings to store logged in user's id and name
    String name;
    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening);
    }

//    Creating function tcpClicked to call when clicked on TCP Button
    public void tcpClicked(View view){
        checkUser("TCP");
    }

//    Creating function udpClicked to call when clicked on UDP button
    public void udpClicked(View view){ checkUser("UDP"); }

//    Creating function checkUser to check if any user information already exists on the local database storage
    public void checkUser(String protocol){
//        Creating file object using database directory
        File dbFile = new File(getFilesDir()+"/userData.db");

//        Proceeding to login activity if no user info pre-exists on the device
        if (!dbFile.exists()){
            Intent loginActivity = new Intent(getApplicationContext(),LoginActivity.class);
//            Sending protocol selected to the login activity
            loginActivity.putExtra("protocol",protocol);
            startActivity(loginActivity);
        }
        else{
//            Getting user info of the stored user if database file exists on the device
            try {
                SQLiteDatabase localDatabase = SQLiteDatabase.openDatabase(getFilesDir()+"/userData.db",null,SQLiteDatabase.OPEN_READONLY);
                String fetchQuery = "SELECT * FROM users";
                Cursor cursor =localDatabase.rawQuery(fetchQuery,null);

                while (cursor.moveToNext()) {
                    id = cursor.getString(cursor.getColumnIndex("id"));
                    String firstname = cursor.getString(cursor.getColumnIndex("firstname"));
                    String lastname = cursor.getString(cursor.getColumnIndex("lastname"));
                    name = firstname+" "+lastname;
                }
                localDatabase.close();

//                Proceeding to network activity after user information is fetched from database
                Intent networkActivity = new Intent(getApplicationContext(), NetworkActivity.class);
                networkActivity.putExtra("protocol",protocol);
                networkActivity.putExtra("userId",id);
                networkActivity.putExtra("name",name);

                startActivity(networkActivity);
            }
            catch (Exception e){
//                Proceeding to login activity if any exception occurs
                Intent loginActivity = new Intent(getApplicationContext(),LoginActivity.class);
                loginActivity.putExtra("protocol",protocol);
                startActivity(loginActivity);
            }
        }
    }

//    Calling doubleBackToExit function from CommonActivity when back button is pressed
    @Override
    public void onBackPressed() {
        doubleBackToExit();
    }
}

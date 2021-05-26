package com.subin.javachat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class CommonActivity extends Activity {

//    Assigning server ip address to a variable for database operations
    String dbIp = "192.168.254.1";
//    Assigning backPressed variable to false at initial stage to exit the app on pressing back button twice
    boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

//    Creating function showError to show errors on activities if nay exception occurs
    public void showError(String errorText){
//        Getting context of app for the main layout
        View context = findViewById(R.id.mainLayout);
//        dislaying error using snackbar element
        Snackbar showError = Snackbar.make(context,errorText,Snackbar.LENGTH_LONG);
        View snackbarView = showError.getView();
        snackbarView.setBackgroundColor(Color.rgb(135,7,7));
        showError.show();
    }

    //    Creating function showMessage to show success message throughout the app
    public void showMessage(String msgText){
        View context = findViewById(R.id.mainLayout);
//          dislaying message using snackbar element
        Snackbar showMsg = Snackbar.make(context,msgText,Snackbar.LENGTH_LONG);
        View snackbarView = showMsg.getView();
        snackbarView.setBackgroundColor(Color.rgb(8, 99, 14));
        showMsg.show();
    }

    //    Creating push to database function to insert messages to the database
    //    Parameters: message = message to be inserted
    //    sender_id = user_id of the sender
    //    receiver_id = user_id of the receiver
    public void pushToDatabase(String message,String senderId,String receiverId){
        try {
//            Replacing all the spaces in messages with '%20' browser code
            String mMessage = message.replace(" ","%20");
//            Creating url path to send get request to the databse server
//            server code is handles by php
            String serverPath = "http://"+dbIp+"/JavaChat/db.php?task=msg&message="+mMessage+"&sender_id="+senderId+"&receiver_id="+receiverId;
            URL pathUrl = new URL(serverPath);
            URLConnection connection =pathUrl.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;

            if(httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK){
                Log.d("RESPONSE: ",httpConnection.getResponseCode()+"");
            }
        }
        catch(Exception e){
            Log.d("ERRORDB",e.getMessage());
        }
    }

    public void fetchFromDatabase(String sender_id, String receiver_id, final Context appContext, final LinearLayout chatLayout){
        try {
//            Creating url path to send GET request to the server
            String serverPath = "http://"+dbIp+"/JavaChat/db.php?task=fetch&sender_id="+sender_id+"&receiver_id="+receiver_id;
            URL pathUrl = new URL(serverPath);
            URLConnection connection =pathUrl.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;

//            Getting the response from the server if the response code is OK
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStream response =httpConnection.getInputStream();
                InputStreamReader reader =new InputStreamReader(response);
                BufferedReader buffer = new BufferedReader(reader);

//                Initializing a string result to stores response from the server
                String result = "";

                //    Appending response from the server to the result variable until the response becomes null
                do{
                    result+=buffer.readLine();
                }while (buffer.readLine() != null);

                if (!result.equals("") && result != null) {

//                    Creating json object from the received string sice the response sent by the server is a json object
                    JSONObject jsonObject = new JSONObject(result);
//                    Extracting json array inside the recived json
                    JSONArray jsonArray = (JSONArray) jsonObject.get("msg");
//                    Getting the length of the json array
                    int arrayLength = jsonArray.length();

//                    Looping through the json array to get all the messages and append to the activity
                    for (int i = 0; i < arrayLength; i++) {
                        JSONObject jsonMessage = (JSONObject) jsonArray.get(i);
//                        Getting message and sender_id from the json array
                        final String message = jsonMessage.get("message").toString();
                        String sender = jsonMessage.get("sender_id").toString();

//                        calling postMyMessage function to display the message sent by the server to the right side
                        if (sender.equals(sender_id)) {
//                            Running the method inside UI thread since the views can't be manipulated from other threads
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    postMyMessage(message, appContext, chatLayout);
                                }
                            });
//                        calling postReceivedMessage function to display the message sent by the client to the left side
                        } else {
//                            Running the method inside UI thread since the views can't be manipulated from other threads
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    postReceivedMessage(message, appContext, chatLayout);
                                }
                            });
                        }
                    }
                }
            }
        }
        catch(Exception e){
            Log.d("ERROR",e.getMessage());
        }
    }

//    Creating function postReceivedMessage to display received messages to the view
    public void postReceivedMessage(String msg, Context appContext, LinearLayout chatLayout){
//        Creating new Textview
        TextView msgBubble = new TextView(appContext);
//        Setting layout parameters for the new textview
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.bottomMargin = 20;
        layout.gravity = Gravity.LEFT;
//        Setting other properties for the textview
        msgBubble.setLayoutParams(layout);
        msgBubble.setText(msg);
        msgBubble.setBackgroundColor(Color.rgb(225,236,244));
        msgBubble.setTextColor(Color.BLACK);
        msgBubble.setElevation(1);
        msgBubble.setTextSize(TypedValue.COMPLEX_UNIT_DIP,16);
        msgBubble.setPadding(10,6,10,6);
        msgBubble.setMaxWidth(300);
//        Adding new textview to the main chat window
        chatLayout.addView(msgBubble);
    }

    //    Creating function postMyMessage to display sent messages to the view
    public void postMyMessage(String msg, Context appContext, LinearLayout chatLayout){
//        Creating new Textview
        TextView msgBubble = new TextView(appContext);
//        Setting layout parameters for the new textview
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.bottomMargin = 20;
        layout.gravity = Gravity.RIGHT;
//        Setting other properties for the textview
        msgBubble.setLayoutParams(layout);
        msgBubble.setText(msg);
        msgBubble.setBackgroundResource(R.drawable.gradient);
        msgBubble.setTextColor(Color.WHITE);
        msgBubble.setElevation(1);
        msgBubble.setTextSize(TypedValue.COMPLEX_UNIT_DIP,16);
        msgBubble.setPadding(10,6,10,6);
        msgBubble.setMaxWidth(300);
//        Adding new textview to the main chat window
        chatLayout.addView(msgBubble);
    }

//    Creating function doubleBackToExit to record the number of back presses and exit the application when it is pressed twice
    public void doubleBackToExit(){
        if (backPressed) {
//            Closing the app and killing the app process
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        else{
//            Showing the toast notification when back button is pressed once
            backPressed = true;
            Toast.makeText(getApplicationContext(),"Press Back Once Again To Exit.",Toast.LENGTH_SHORT).show();

//            Running timer to reset the number of back button presses after 1.5s
            Timer exitTimer = new Timer();
            exitTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    backPressed = false;
                }
            },1500);
        }
    }
}

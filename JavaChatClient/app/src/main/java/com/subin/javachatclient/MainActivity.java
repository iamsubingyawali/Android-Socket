package com.subin.javachatclient;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends CommonActivity {

//    Defining strings to store received protocol and port number
    String protocol;
    String ipAddress;
    String portNum;
    EditText msgBox;
//    Defining strings to store userId and username
    public static String user;
    public static String userId;
//    Defining thread object clientThread
    public static Thread clientThread;
//    Creating objects of two classes TcpCommunication and UdpCommunications to perform chat using TCP and UDP
    TcpCommunication tcpCommunication;
    UdpCommunication udpCommunication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Getting protocol info, username, userId, Ip address and port number from the previous activity
        protocol = getIntent().getExtras().getString("protocol");
        user = getIntent().getExtras().getString("userName");
        userId = getIntent().getExtras().getString("userId");
        ipAddress = getIntent().getExtras().getString("ip");
        portNum = getIntent().getExtras().getString("port");
        msgBox = findViewById(R.id.msgBox);

//        Getting the views on the activity to modify or get the typed contents
        TextView protocolInfo = findViewById(R.id.protocol);
        RelativeLayout loading = findViewById(R.id.loading);
        LinearLayout bottom = findViewById(R.id.bottomLayout);
        LinearLayout chatLayout = findViewById(R.id.chatLayout);
        TextView userField = findViewById(R.id.user);

//        Creating objects of two classes TcpCommunication and UdpCommunication to perform chat using TCP and UDP
//        Constructor Arguments: msgBox = EditText to type message to be sent
//        IpAddress: Ip address of the server to connect
//        portNum: Opened Port Number of the server to connect
//        loading: Loading progress shown on the activity before the connection is successful
//        userField: TextView to show username of the connected server on the top of the activity
//        chatLayout: Main layout at middle to show the sent and received chats using textviews
//        this: Context of the activity
        tcpCommunication = new TcpCommunication(msgBox,ipAddress,portNum,loading,bottom,userField,chatLayout,this);
        udpCommunication = new UdpCommunication(msgBox,ipAddress,portNum,loading,bottom,userField,chatLayout,this);

//        Running object of respective communication classes on the basis of selected protocol inside a thread
        if (protocol.equals("TCP")){
            MainActivity.clientThread = new Thread(tcpCommunication);
            protocolInfo.setText("TCP Connection");
        }
        else{
            MainActivity.clientThread = new Thread(udpCommunication);
            protocolInfo.setText("UDP Connection");
        }
        MainActivity.clientThread.start();
    }

//    Creating method logout Clicked to logout of the app when logout button at the top right cornor is pressed
    public void logoutClicked(View view){
//        Deleting the pre-existing local database file and going back to login activity
        File dbFile = new File(getFilesDir()+"/userData.db");
        SQLiteDatabase.deleteDatabase(dbFile);

        Intent loginActivity = new Intent(getApplicationContext(),LoginActivity.class);
        loginActivity.putExtra("protocol",protocol);
        startActivity(loginActivity);
    }

//    Creating method sendClicked to send the message when send button at the bottom right corner is pressed after writing some texts inside message box
    public void sendClicked(View view){
        // Starting synchronized block when send button is clicked to call notifyAll method by the thread to
        // make the thread wake up from the wait state called inside the run method of the thread for communication
        synchronized (MainActivity.clientThread) {
            MainActivity.clientThread.notifyAll();
        }
    }

//    Calling doubleBackToExit method from CommonActivity when backbutton is clicked
    @Override
    public void onBackPressed() {
        doubleBackToExit();
    }
}

//Creating class for communication using TCP inside a thread
class TcpCommunication extends CommonActivity implements Runnable{
    String ipAddress;
    String portNum;
    EditText msgBox;
    String serverName;
    String serverId;
    RelativeLayout loading;
    LinearLayout bottom;
    TextView userField;
    LinearLayout chatLayout;
    Context appContext;
    boolean isAlive= true;
    int request=0;

    public TcpCommunication(){ } // Default Constructor

//    Parameterized Constructor
//        Constructor Parameters: msgBox = EditText to type message to be sent
//        IpAddress: Ip address of the server to connect
//        portNum: Opened Port Number of the server to connect
//        loading: Loading progress shown on the activity before the connection is successful
//        userField: TextView to show username of the connected server on the top of the activity
//        chatLayout: Main Linear layout at middle to show the sent and received chats using textviews
//        appContext: Context of the activity
    public TcpCommunication(EditText msgBox,String ipAddress,String portNum,RelativeLayout loading,LinearLayout bottom,TextView userField,LinearLayout chatLayout,Context appContext){
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.msgBox = msgBox;
        this.loading = loading;
        this.bottom = bottom;
        this.userField = userField;
        this.chatLayout = chatLayout;
        this.appContext = appContext;
    }

    @Override
    public void run() {
        try {
//            Defining the object of the socket through which the communication occurs
            Socket clientSocket = null;
//            Defining input stream object to read the input stream
            InputStreamReader inputReader = null;
//            Defining bufferedReader object to read the data from the buffer
            BufferedReader bufferReader = null;
//            Defining PrintWriter object to write the data to the socket
            PrintWriter inputWriter = null;

//            Starting while loop to continuous communication
            while (isAlive){
                if(request > 0) {
//                    starting synchronized block stop execution of thread until the thread is notified to continue
                    synchronized (MainActivity.clientThread) {
                        MainActivity.clientThread.wait();
                    }
                }
//                ****SENDING****
//                Creating socket object using IP Address and port number for the communication over it
                clientSocket = new Socket(ipAddress,Integer.parseInt(portNum));
//                Creating OutputStream object to send data to the output stream over socket
                OutputStream output = clientSocket.getOutputStream();
//                PrintWriter object to write data to the socket
                inputWriter = new PrintWriter(output,true);

//                Getting the typed message from the message box and sending the data
//                if it is not the first message
                if(request > 0) {
                    final String clientMessage = msgBox.getText().toString();
                    inputWriter.println(clientMessage);
//                    Running code to manipulate view inside UI Thread since other threads can't modify views
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            msgBox.getText().clear();
                            postMyMessage(clientMessage,appContext,chatLayout);
                        }
                    });
//                    Inserting sent messages to the remote database
                    pushToDatabase(clientMessage,MainActivity.userId,serverId);
                }
//                If the message to be sent is first message, the server receives JSON object of the user info as first message
//                Sending JSON object with logged in client user info in it
                else{
                    JSONObject user = new JSONObject();
                    user.put("userName",MainActivity.user);
                    user.put("userId",MainActivity.userId);
                    inputWriter.println(user);
                }

//                ****RECEIVING****
//                InputStreamReader object to create input stream to receive data from socket
                inputReader = new InputStreamReader(clientSocket.getInputStream());
//                BufferedReader object to store input stream data into buffer
                bufferReader = new BufferedReader(inputReader);
//                Creating string to store message to be received
                final String serverMessage;

                if ((serverMessage = bufferReader.readLine()) != null){
//                Displaying the message on the chat window using postReceivedMessage method
//                If the received message is not the first message for the client
                    if (request > 0) {
//                        Running method inside UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postReceivedMessage(serverMessage,appContext,chatLayout);
                            }
                        });
                    }
//                    If the received message is first message for the client, the server sends a JSON with its details as first message
//                    Extracting the JSON object and getting the server details
                    else{
                        JSONObject server = new JSONObject(serverMessage);
                        serverName = server.getString("serverName");
                        serverId = server.getString("serverId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Setting the name of the server at the top of the activity
                                userField.setText(serverName);
//                                Setting loading animation visibility to invisible and showing message box to type messages
                                bottom.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.INVISIBLE);
                            }
                        });
//                        Fetching the previous conversations between current user and the server if any exist
                        fetchFromDatabase(MainActivity.userId,serverId,appContext,chatLayout);
                    }
                }
                request+=1;
            }
//            Closing all the resources
            inputWriter.close();
            inputReader.close();
            bufferReader.close();
            clientSocket.close();

        } catch (Exception e) {
            Log.d("ERROR",e.getMessage());
        }
    }
}


//Class for communication using UDP inside a thread
class UdpCommunication extends CommonActivity implements Runnable{
    String ipAddress;
    String portNum;
    EditText msgBox;
    String serverName;
    String serverId;
    RelativeLayout loading;
    LinearLayout bottom;
    TextView userField;
    LinearLayout chatLayout;
    Context appContext;
    boolean isAlive= true;
    int request=0;

    public UdpCommunication(){ } // Default Constructor

//    Parameterized Constructor
//        Constructor Parameters: msgBox = EditText to type message to be sent
//        IpAddress: Ip address of the server to connect
//        portNum: Opened Port Number of the server to connect
//        loading: Loading progress shown on the activity before the connection is successful
//        userField: TextView to show username of the connected server on the top of the activity
//        chatLayout: Main Linear layout at middle to show the sent and received chats using textviews
//        appContext: Context of the activity
    public UdpCommunication(EditText msgBox,String ipAddress,String portNum,RelativeLayout loading,LinearLayout bottom,TextView userField,LinearLayout chatLayout,Context appContext){
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.msgBox = msgBox;
        this.loading = loading;
        this.bottom = bottom;
        this.userField = userField;
        this.chatLayout = chatLayout;
        this.appContext = appContext;
    }

    @Override
    public void run() {
        try {
//            Creating DatagramSocket object to create socket for the communication
            DatagramSocket clientSocket = new DatagramSocket();

//            ****SENDING****
//            Starting while loop for continuous communication
            while (isAlive){
//                Starting synchronized thread to stop the execution of the thread until the thread is notified to continue
                if(request > 0) {
                    synchronized (MainActivity.clientThread) {
                        MainActivity.clientThread.wait();
                    }
                }

//                Defining an array of bytes to store the sent message as bytes
                byte[] bufferBytes;
//                Creating InetAddress object using IP address provided
                InetAddress ip = InetAddress.getByName(ipAddress);

//                Getting the typed message from the message box and sending the data
//                if it is not the first message
                if(request > 0) {
                    final String clientMessage = msgBox.getText().toString();
                    bufferBytes = clientMessage.getBytes();
//                    Crating DatagramPacket object and sending the data
                    DatagramPacket send = new DatagramPacket(bufferBytes,bufferBytes.length,ip,Integer.parseInt(portNum));
                    clientSocket.send(send);
//                    Displaying the sent messages on the chat window
//                    Running the methods to modify view inside the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            msgBox.getText().clear();
                            postMyMessage(clientMessage,appContext,chatLayout);
                        }
                    });
//                    Inserting the sent messages to the remote database
                    pushToDatabase(clientMessage,MainActivity.userId,serverId);
                }
//                If the message to be sent is first message, creating a JSON object with the details of the current user
//                and sending the data since server receives JSON object as the first message by the client
                else{
                    JSONObject user = new JSONObject();
                    user.put("userName",MainActivity.user);
                    user.put("userId",MainActivity.userId);
                    bufferBytes = user.toString().getBytes();
                    DatagramPacket send = new DatagramPacket(bufferBytes,bufferBytes.length,ip,Integer.parseInt(portNum));
                    clientSocket.send(send);
                }

//                ****RECEIVING****
//                Creating DatagramPacket object to receive the packet data from the socket
                DatagramPacket receive = new DatagramPacket(bufferBytes,bufferBytes.length);
//                Receiving the packet
                clientSocket.receive(receive);
//                Creating string to store the received message
                final String serverMessage;

//                Getting the received message and displaying on the chat window if the message is not the first message by the server
                if ((serverMessage = new String(receive.getData())) != null){
                    if (request > 0) {
//                        Running postReceivedMessage method to display received message on the chat window
//                        Running the method inside UI thread since other threads can't modify views
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postReceivedMessage(serverMessage,appContext,chatLayout);
                            }
                        });
                    }
//                    If the received message is the first message by the server, the server sends a JSON object loaded with its details in it
//                    Extracting the server details from the received JSON object
                    else{
                        JSONObject server = new JSONObject(serverMessage);
                        serverName = server.getString("server");
                        serverId = server.getString("serverId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Displaying the name of the server at the top of the main activity
                                userField.setText(serverName);
//                                Hiding the loading animation and showing the message box to type message
                                bottom.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.INVISIBLE);
                            }
                        });
//                        Fetching the previous conversations between the server and the current client from the database, if any exist
                        fetchFromDatabase(MainActivity.userId,serverId,appContext,chatLayout);
                    }
                }
                request+=1;
            }
//            closing the DatagramSocket
            clientSocket.close();

        } catch (Exception e) {
            Log.d("ERROR",e.getMessage());
        }
    }
}
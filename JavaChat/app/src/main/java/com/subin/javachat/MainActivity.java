package com.subin.javachat;

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
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends CommonActivity {

//    Defining strings to store received protocol and port number
    String protocol;
    String portNum;
    EditText msgBox;
//    Defining strings to store serverId and name
    public static String serverId;
    public static String server;
//    Defining thread object ServerThread
    public static Thread serverThread;
//    Creating objects of two classes TcpCommunication and UdpCommunications to perform chat using TCP and UDP
    TcpCommunication tcpCommunication;
    UdpCommunication udpCommunication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Getting protocol info, servername, serverId and port number from the previous activity
        protocol = getIntent().getExtras().getString("protocol");
        portNum = getIntent().getExtras().getString("portNum");
        serverId = getIntent().getExtras().getString("serverId");
        server = getIntent().getExtras().getString("serverName");
        msgBox = findViewById(R.id.msgBox);

//        Getting the views on the activity to modify or get the typed contents
        TextView protocolInfo = findViewById(R.id.protocol);
        RelativeLayout loading = findViewById(R.id.loading);
        LinearLayout bottom = findViewById(R.id.bottomLayout);
        LinearLayout chatLayout = findViewById(R.id.chatLayout);
        TextView userField = findViewById(R.id.user);

//        Creating objects of two classes TcpCommunication and UdpCommunication to perform chat using TCP and UDP
//        Constructor Arguments: msgBox = EditText to type message to be sent
//        portNum: Opened Port Number of the server
//        loading: Loading progress shown on the activity before the connection is successful
//        userField: TextView to show username of the user connected on the top of the activity
//        chatLayout: Main layout at middle to show the sent and received chats using textviews
//        this: Context of the activity
        tcpCommunication = new TcpCommunication(msgBox,portNum,loading,bottom,userField,chatLayout,this);
        udpCommunication = new UdpCommunication(msgBox,portNum,loading,bottom,userField,chatLayout,this);

//        Running object of respective communication classes on the basis of selected protocol inside a thread
        if (protocol.equals("TCP")){
            MainActivity.serverThread = new Thread(tcpCommunication);
            protocolInfo.setText("TCP Connection");
        }
        else{
            MainActivity.serverThread = new Thread(udpCommunication);
            protocolInfo.setText("UDP Connection");
        }
        MainActivity.serverThread.start();
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
        synchronized (MainActivity.serverThread) {
            MainActivity.serverThread.notifyAll();
        }
    }

//    Calling doubleBackToExit method from CommonActivity when back button is clicked
    @Override
    public void onBackPressed() {
        doubleBackToExit();
    }
}

//Class to create TCP connection to run inside a thread
class TcpCommunication extends CommonActivity implements Runnable{
    EditText msgBox;
    String portNum;
    String userId;
    String userName;
    RelativeLayout loading;
    LinearLayout bottom;
    TextView userField;
    LinearLayout chatLayout;
    Context appContext;
    boolean isAlive = true;
    int response=0;

    public TcpCommunication(){ } // Default Constructor

//    Parameterized Constructor
//        Constructor Parameters: msgBox = EditText to type message to be sent
//        portNum: Opened Port Number of the server
//        loading: Loading progress shown on the activity before the connection is successful
//        bottom: the bottom part of the chat interface with input field and send button
//        userField: TextView to show username of the connected user on the top of the activity
//        chatLayout: Main Linear layout at middle to show the sent and received chats using textviews
//        appContext: Context of the activity
    public TcpCommunication(EditText msgBox,String portNum,RelativeLayout loading,LinearLayout bottom,TextView userField,LinearLayout chatLayout,Context appContext){
       this.msgBox = msgBox;
       this.portNum = portNum;
       this.loading = loading;
       this.bottom = bottom;    
       this.userField = userField;
       this.chatLayout = chatLayout;
       this.appContext = appContext;
    }

    @Override
    public void run() {
        try {
//            Opening a specified port for the communication by the server
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(portNum));
//            Defining the object of the socket through which the communication occurs
            Socket clientSocket = null;
//            Defining input stream object to read the input stream
            InputStreamReader inputReader = null;
//            Defining bufferedReader object to read the data from the buffer
            BufferedReader bufferReader = null;
//            Defining PrintWriter object to write the data to the socket
            PrintWriter inputWriter = null;

//            Starting while loop to continuously listen for the requests from the client
            while (isAlive){
//              ****RECEIVING****
//                Accepting communication requests on the defined socket
                clientSocket = serverSocket.accept();
//                Getting the input stream
                inputReader = new InputStreamReader(clientSocket.getInputStream());
//                Loding the buffer with the input stream data
                bufferReader = new BufferedReader(inputReader);
//                Defining string to store the received message from the client
                final String clientMessage;

//                Displaying the received message to the chat window
//                if it is not the initial message
                if (response > 0) {
                    if ((clientMessage = bufferReader.readLine()) != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postReceivedMessage(clientMessage,appContext,chatLayout);
                            }
                        });
                    }

//                    Starting synchronized block to stop the execution of the thread program until it is notified
                    synchronized (MainActivity.serverThread) {
                        MainActivity.serverThread.wait();
                    }
                }
//                Initially when communication is started the client sends json object with the details of the client user into it
//                Extracting the info from the json object
                else{
                    if ((clientMessage = bufferReader.readLine()) != null) {

                        JSONObject user = new JSONObject(clientMessage);
                        userName = user.getString("userName");
                        userId = user.getString("userId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Displaying the userName of the user at the top
                                userField.setText(userName);
//                                Hiding loading animation and showing view to type and send message
                                bottom.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.INVISIBLE);
                            }
                        });
//                        Loading conversations between the connected user and the server from the remote database if any exist
                        fetchFromDatabase(MainActivity.serverId,userId,appContext,chatLayout);
                    }
                }
//                ****SENDING****
//                Creating an outputStream object to send the data
                OutputStream output = clientSocket.getOutputStream();
//                loading the printWriter object with output stream
                inputWriter = new PrintWriter(output,true);

//                Getting the typed message from the editText and sending if it is not the first message
                if (response > 0) {
                    final String serverMessage = msgBox.getText().toString();
                    inputWriter.println(serverMessage);
//                    Running the methods inside UI thread since views can't be modified by other threads
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Clearing editText box
                            msgBox.getText().clear();
                            postMyMessage(serverMessage,appContext,chatLayout);
                        }
                    });
//                    Inserting the sent messages to the database using pushToDatabase method
                    pushToDatabase(serverMessage,MainActivity.serverId,userId);
                }
//                Initially when server is sending the message, it sends a json object with its details in it
                else{
                    JSONObject server = new JSONObject();
                    server.put("serverName",MainActivity.server);
                    server.put("serverId",MainActivity.serverId);
                    inputWriter.println(server);
                }
                response+=1;
            }
//            Closing all the resources
            inputWriter.close();
            inputReader.close();
            bufferReader.close();
            clientSocket.close();
            serverSocket.close();

        } catch (Exception e) {
            Log.d("ERROR",e.getMessage());
        }
    }
}


//Class to perform UDP communication inside a thread
class UdpCommunication extends CommonActivity implements Runnable{
    EditText msgBox;
    String portNum;
    String userId;
    String userName;
    RelativeLayout loading;
    LinearLayout bottom;
    TextView userField;
    LinearLayout chatLayout;
    Context appContext;
    boolean isAlive = true;
    int response=0;

    public UdpCommunication(){ } // Default Constructor

//    Parameterized Constructor
//        Constructor Parameters: msgBox = EditText to type message to be sent
//        portNum: Opened Port Number of the server
//        loading: Loading progress shown on the activity before the connection is successful
//        bottom: the bottom linear layout for message input and send button
//        userField: TextView to show username of the connected user on the top of the activity
//        chatLayout: Main Linear layout at middle to show the sent and received chats using textviews
//        appContext: Context of the activity
    public UdpCommunication(EditText msgBox,String portNum,RelativeLayout loading,LinearLayout bottom,TextView userField,LinearLayout chatLayout,Context appContext){
        this.msgBox = msgBox;
        this.portNum = portNum;
        this.loading = loading;
        this.bottom = bottom;
        this.userField = userField;
        this.chatLayout = chatLayout;
        this.appContext = appContext;
    }

    @Override
    public void run() {
        try {
//            Creating DatagramSocket object to create a socket for the communication
            DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(portNum));

//            Starting while to continuously accept the communication requests
            while (isAlive){
//                ****RECEIVING****
//                Creating bytes array of 1024 bytes to store bytes of message
                byte[] bufferBytes = new byte[1024];
//                Creating the DatagramPacket object to receive the data packet
                DatagramPacket datagramPacket = new DatagramPacket(bufferBytes,bufferBytes.length);
//                Receiving the data packet from the socket
                serverSocket.receive(datagramPacket);
//                Defining string to store the message sent by the client
                final String clientMessage;

//                Displaying the received message to the chat window
//                if it is not the initial message
                if (response > 0) {
                    if ((clientMessage = new String(datagramPacket.getData())) != null) {
                        final String clientMsg = new String(datagramPacket.getData(),datagramPacket.getOffset(),datagramPacket.getLength());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postReceivedMessage(clientMsg,appContext,chatLayout);
                            }
                        });
                    }

//                    Starting synchronized block to stop the execution of the thread program until it is notified
                    synchronized (MainActivity.serverThread) {
                        MainActivity.serverThread.wait();
                    }
                }
//                Initially when communication is started the client sends json object with the details of the client user into it
//                Extracting the info from the json object
                else{
                    if ((clientMessage = new String(datagramPacket.getData())) != null) {

                        JSONObject user = new JSONObject(clientMessage);
                        userName = user.getString("userName");
                        userId = user.getString("userId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Displaying the userName of the user at the top
                                userField.setText(userName);
//                                Hiding loading animation and showing view to type and send message
                                bottom.setVisibility(View.VISIBLE);
                                loading.setVisibility(View.INVISIBLE);
                            }
                        });
//                        Fetching the conversation between the server and the user connected from the database and displaying if any exist
                        fetchFromDatabase(MainActivity.serverId,userId,appContext,chatLayout);
                    }
                }

//                ****SENDING****
//                Getting ip address of the client from the socket
                InetAddress clientIp = datagramPacket.getAddress();
//                Getting open port of the client from the socket
                int port = datagramPacket.getPort();

//                Getting the typed message from the message box and sending to the client
//                if it is not the first message
                if (response > 0) {
                    final String serverMessage = msgBox.getText().toString();
//                    Extracting bytes from the message typed
                    bufferBytes = serverMessage.getBytes();
//                    Creating DatagramPacket to be sent over the socket
                    DatagramPacket send = new DatagramPacket(bufferBytes,bufferBytes.length,clientIp,port);
                    serverSocket.send(send);
//                    Displaying the message and clearing the message box
//                    Running methods inside UI thread since views can't be modified by other threads
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            msgBox.getText().clear();
                            postMyMessage(serverMessage,appContext,chatLayout);
                        }
                    });
//                    Inserting sent messages to the database using pushToDatabase method
                    pushToDatabase(serverMessage,MainActivity.serverId,userId);
                }
//                Creating JSON object by loading server details into it to be sent over the socket as the first message from the server
                else{
                    JSONObject server = new JSONObject();
                    server.put("server",MainActivity.server);
                    server.put("serverId",MainActivity.serverId);
//                    Getting data bytes from the String json object
                    bufferBytes = server.toString().getBytes();
//                    Creating datagram packet and sending the data
                    DatagramPacket send = new DatagramPacket(bufferBytes,bufferBytes.length,clientIp,port);
                    serverSocket.send(send);
                }
                response+=1;
            }
//            closing Datagram Socket
            serverSocket.close();

        } catch (Exception e) {
            Log.d("ERROR",e.getMessage());
        }
    }
}
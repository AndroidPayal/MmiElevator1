package com.radioknit.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Tag_MainActivity";
    private static final boolean D = true;
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private static StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private static BluetoothChatService mChatService = null;
    //private static BluetoothChatService connector;
    BluetoothDevice device;
    int tryBTConnect=0;
    static StringBuffer msgAppend=new StringBuffer();
    private Menu menu;
    private MainActivity mContext;


    TextView txtDate;
    TextView txtTime;

    Button sendmsgb;
    EditText editText_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createObj();
        generateId();

        sendmsgb = findViewById(R.id.sendmsg);
        editText_msg = findViewById(R.id.edit_msg);
        sendmsgb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage((editText_msg.getText().toString()+"").getBytes());
            }
        });


    //    registerEvent();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        BufferedReader input = null;
        File file = null;
        try {
            file = new File(getFilesDir(), "MyFile"); // Pass getFilesDir() and "MyFile" to read file

            input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            device = mBluetoothAdapter.getRemoteDevice(buffer.toString());
            try{
                mChatService.connect(device);
            }
            catch (Exception e){
                tryBTConnect=tryBTConnect+1;
                //mChatService.connect(device);
                //Toast.makeText(this,"First time clicked",Toast.LENGTH_SHORT).show();

            }

            // Log.d(TAG, buffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

     //   myHandlerChk.postDelayed(checkDataContinue, 0);


    }

    private void createObj() {
        mContext = MainActivity.this;
    }


    private void generateId() {
        txtDate = findViewById(R.id.tvDate);
        txtTime = findViewById(R.id.tvTime);

        tryBTConnect=0;
    }



  @Override
    public void onStart() {
        super.onStart();
       /* if (D)
            Log.e(TAG, "++ ON START ++");*/

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null)
                setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
       /* if (D)
            Log.e(TAG, "+ ON RESUME +");*/

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity
        // returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        //  Log.d(TAG, "setupChat()");


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }
    @Override
    public synchronized void onPause() {
        super.onPause();
        /*if (D)
            Log.e(TAG, "- ON PAUSE -");*/
    }

    @Override
    public void onStop() {
        super.onStop();
        /*if (D)
            Log.e(TAG, "-- ON STOP --");*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null)
            mChatService.stop();
       /* if (D)
            Log.e(TAG, "--- ON DESTROY ---");*/
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(resId);
        actionBar.show();
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(subTitle);
        actionBar.show();
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    //final ActionBar actionBar = getSupportActionBar();
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            /*setStatus(getString(R.string.title_connected_to,
                                    mConnectedDeviceName));*/
                            /*setStatus(getString(R.string.title_connected_to,
                                    mConnectedDeviceName));*/
                            // menu.getItem(0).setIcon(ContextCompat.getDrawable(mContext, R.drawable.grn_bt));
                            setStatus("Connected to "+ mConnectedDeviceName);

                            //actionBar.setSubtitle("Connected to "+ mConnectedDeviceName);
                           /* //for AutoConnect
                            btConnect=0;
                            btConnect=btConnect+1;*/

                            // mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus("title_connecting");
                            //actionBar.setSubtitle(getString(R.string.title_connecting));
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                           /* Toast.makeText(getApplicationContext(),
                                    "Listening Mode",
                                    Toast.LENGTH_SHORT).show();
*/
                            if(tryBTConnect==1){
                                mChatService.connect(device);
                                tryBTConnect=0;
                                return;
                            }
                            /*if(btConnect==1){
                                mChatService.connect(device);
                            }*/
                        case BluetoothChatService.STATE_NONE:
                            /*try {
                                menu.getItem(0).setIcon(ContextCompat.getDrawable(mContext, R.drawable.red_bt));
                            }
                            catch (Exception e){
                                //
                            }*/
                            setStatus("title_not_connected");
                            //actionBar.setSubtitle(getString(R.string.title_not_connected));

                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Toast.makeText(mContext, "ME: "+writeMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "handleMessage: me: "+writeMessage);

                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    /*byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    txtFpi.setText(readMessage);
                   *//* mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
                            + readMessage);*/
                    //Log.d(TAG,"message rx" );

                   // final String readMessage = (String) msg.obj;

                    String readMessage = (String) msg.obj;
                    if (readMessage != null) {
                        Log.d(TAG,"readMessage got msg=" + readMessage);
                        Toast.makeText(mContext, "msg="+readMessage, Toast.LENGTH_SHORT).show();
                          //  appendLog1(readMessage, false, false, needClean);
                        readMessage="";
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + mConnectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };




    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if (D)
            Log.d(TAG, "onActivityResult " + resultCode);*/
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                   /* Toast.makeText(getApplicationContext(),
                            "Connection Request",
                            Toast.LENGTH_SHORT).show();*/
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    //  Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "not_enabled_Bluetooth",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(
                DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }


    /**
     * Sends a message.
     *
     * @param message
     *            A string of text to send.
     */
    public static void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            /*Toast.makeText(this.getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();*/

            return;
        }

        // Check that there's actually something to send
        if (message.length> 0) {
            msgAppend.setLength(0);
            // Get the message bytes and tell the BluetoothConnectionRoom to write
            //byte[] send = message.getBytes();
            mChatService.write(message);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.device_control_activity, menu);
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        this.menu = menu;
        return true;
    }
    // ============================================================================


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                mChatService.stop();
                Intent serverIntent = null;
                serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;

            case R.id.logout:
                //createDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

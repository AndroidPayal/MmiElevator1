package com.radioknit.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity1 extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static String TAG = "Tag_MainActivity";

    public static final int BLUETOOTH_ACTION_REQUEST_ENABLE_CODE = 1;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 2;
    public static boolean BLUETOOTH_SUPPORT_ON_DEVICE = false;
    public static String EXTRA_DEVICE_ADDRESS = "device_Address";

    ArrayAdapter<String> arrayAdapterNewDevices;
    ArrayAdapter<String> arrayAdapterPairedDevices;
    ArrayList<BluetoothDevice> arrayBluetoothDevices = new ArrayList<>();

    BluetoothAdapter bluetoothAdapter;
    TextView text_bt_status;
    Button button_bt_discovery,button_bt_on, button_listen_as_server, button_create_as_client, button_sendmsg;
    ListView listView_discovered;
    CommonChatApplication chatApplication;
    private static CommonChatApplication mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        findViews();
        UiInitialization();

     //  chatApplication.initiateClient();


        button_bt_on.setOnClickListener(this);
        button_bt_discovery.setOnClickListener(this);
        listView_discovered.setOnItemClickListener(this);
        button_listen_as_server.setOnClickListener(this);
        button_create_as_client.setOnClickListener(this);
        button_sendmsg.setOnClickListener(this);


    }

    private void UiInitialization() {
        text_bt_status.setText("Bluetooth Off");
        arrayAdapterNewDevices=new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_expandable_list_item_1);
        arrayAdapterPairedDevices=new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_expandable_list_item_1);

        listView_discovered.setAdapter(arrayAdapterNewDevices);
    }

    private void findViews() {
        text_bt_status = findViewById(R.id.text_bt_status);
        button_bt_discovery = findViewById(R.id.button_bt_discover);
        button_bt_on=findViewById(R.id.button_bt_on);
        listView_discovered=findViewById(R.id.listView_discovered);
        button_listen_as_server=findViewById(R.id.button_listen_as_server);
        button_create_as_client = findViewById(R.id.button_create_as_client);
        button_sendmsg = findViewById(R.id.button_sendmsg);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.button_bt_on:
                if (BLUETOOTH_SUPPORT_ON_DEVICE)
                    if (!bluetoothAdapter.isEnabled()){
                        Intent intentBtOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intentBtOn, BLUETOOTH_ACTION_REQUEST_ENABLE_CODE);
                    }//else{//BT already on}
                text_bt_status.setText("Bluetooth on");
                break;


            case R.id.button_bt_discover:
                if (bluetoothAdapter.isDiscovering())
                    bluetoothAdapter.cancelDiscovery();
                else
                {
                    arrayAdapterNewDevices.clear();
                    arrayBluetoothDevices.clear();
                    bluetoothAdapter.startDiscovery();

                    //register action broadcasts
                    registerBtDiscoveryBroadcast();

                    //get already paired devices
                    getAlreadyPairedDevices();
                }
                listView_discovered.setAdapter(arrayAdapterNewDevices);
                break;

            case R.id.button_listen_as_server:
                chatApplication.initiateServer();
                break;
            case  R.id.button_create_as_client:
                break;
            case R.id.button_sendmsg:
                String msgToSend = "Hello Payal msg is here";

                //initializing not needed bcz it is initialized by server client classes itself
                //chatApplication.initiateChat(msgToSend);
                chatApplication.sendMsg(msgToSend.getBytes());
                Toast.makeText(this, "msg sent", Toast.LENGTH_SHORT).show();

                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String itemvalue = (String) adapterView.getItemAtPosition(i);
        String mac = itemvalue.substring(itemvalue.length() - 17);//last 17 chars

        bluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: device clicked : "+arrayBluetoothDevices.get(i).getName());
        chatApplication.initiateClient(bluetoothAdapter.getRemoteDevice(mac));//

       // text_bt_status.setText("connecting");

/*        Intent intent=new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, mac);
        setResult(Activity.RESULT_OK , intent);*/

     //   finish();
    }



    public void setupConnectionAndChat(){
         chatApplication = new CommonChatApplication(getApplicationContext(), mHandler);
    }


    //The Handler that gets msg back from CommonChatApplication
    private Handler mHandler =new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            handlerCases(msg);
            return true;
        }
    });

    private void handlerCases(Message msg) {
        switch (msg.what){
            case CommonChatApplication.STATE_NONE:
                text_bt_status.setText(text_bt_status.getText().toString() + "-noneState");
                break;
            case CommonChatApplication.STATE_CONNECTED:
                text_bt_status.setText("connected");
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
                break;
            case CommonChatApplication.STATE_CONNECTING:
                text_bt_status.setText("connecting");
                Toast.makeText(this, "connecting", Toast.LENGTH_SHORT).show();
                break;
            case CommonChatApplication.STATE_CONNECTION_FAILED:
                text_bt_status.setText("connection failed");
                Toast.makeText(this, "connection failed", Toast.LENGTH_SHORT).show();
                break;
            case CommonChatApplication.STATE_LISTEN:
                text_bt_status.setText("listning connection");
                Toast.makeText(this, "listning for connection", Toast.LENGTH_SHORT).show();
                break;
            case CommonChatApplication.STATE_MSG_RECEIVED:
               /* text_bt_status.setText("received msg");
                Toast.makeText(this, "Received msg", Toast.LENGTH_SHORT).show();
*/
                byte[] readBuff = (byte[]) msg.obj;
                String tmpMsg = new String(readBuff , 0, msg.arg1);
                text_bt_status.setText(tmpMsg);
                Toast.makeText(this, tmpMsg, Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        //initiallize BT adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //check device supports BT or not
        checkSupportBTonDevice();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            askLocationPermission();
        }

        //calling commonChatApplication class
        setupConnectionAndChat();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    private void askLocationPermission() {

        //Prompt the user once explanation has been shown
        ActivityCompat.requestPermissions(MainActivity1.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);
    }

    private void checkSupportBTonDevice() {
        if (bluetoothAdapter==null){
            BLUETOOTH_SUPPORT_ON_DEVICE=false;
            Toast.makeText(this, "Bluetooth not supported in device!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: Bluetooth not supported in device");
        }else{
            BLUETOOTH_SUPPORT_ON_DEVICE=true;
            Toast.makeText(this, "Yahoo!Device have Bluetooth support", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: Yahoo!Device have Bluetooth support");
        }
    }


    private void getAlreadyPairedDevices() {
        //get previously paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d(TAG, "UiInitialization: count="+pairedDevices.size());
        for (BluetoothDevice device : pairedDevices) {
            arrayAdapterNewDevices.add("Paired: " + device.getName() + "\n" + device.getAddress());
            arrayBluetoothDevices.add(device);
        }
        Log.d(TAG, "getAlreadyPairedDevices: bluetoothArraySize : " +arrayBluetoothDevices.size());
    }

    private void registerBtDiscoveryBroadcast() {
        IntentFilter intent_found= new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastDiscoveryReceiver,intent_found);

        IntentFilter intent_end_discover = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastDiscoveryReceiver, intent_end_discover);
    }

/*
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);

        switch (requestCode){
            case BLUETOOTH_ACTION_REQUEST_ENABLE_CODE:
                Log.d(TAG, "startActivityForResult: BT enabled");
                Toast.makeText(this, "Turning On Bluetooth", Toast.LENGTH_SHORT).show();
                break;
        }
    }
*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case BLUETOOTH_ACTION_REQUEST_ENABLE_CODE:
                Log.d(TAG, "startActivityForResult: BT enabled");
                Toast.makeText(this, "Turning On Bluetooth", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    BroadcastReceiver broadcastDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action= intent.getAction();
            handleOperationOfthisBroadcast(action,context,intent);
        }
    };

    private void handleOperationOfthisBroadcast(String action, Context context, Intent intent) {
        switch (action){
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState()!=BluetoothDevice.BOND_BONDED){
                    arrayAdapterNewDevices.add(device.getName() + "\n" + device.getAddress());
                    arrayBluetoothDevices.add(device);
                }

                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                if (arrayAdapterNewDevices.getCount()==0){
                    Toast.makeText(context, "No Device Found ", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastDiscoveryReceiver);
    }

   }

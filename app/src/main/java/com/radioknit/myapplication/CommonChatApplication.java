package com.radioknit.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class CommonChatApplication {
    public static String TAG = "Tag_CommonChatApplication";
    // Unique UUID for this application
    private static final UUID UNIQUE_UUID = UUID
            .fromString("0001101-0000-1000-8000-00805f9b34fb");

    // INSECURE "8ce255c0-200a-11e0-ac64-0800200c9a66"
    // SECURE   "fa87c0d0-afac-11de-8a39-0800200c9a66"
    //c08f1b0c-86d6-4ed0-bd01-e3a0e40e816f
    // SPP "0001101-0000-1000-8000-00805F9B34FB"
    /*Hint: If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you are connecting to an Android peer then please generate your own unique UUID.*/
    public static String NAME = "BluetoothChatApp";//normaly used app name

    private BluetoothAdapter bluetoothAdapter;
    private int mState;
    Context context;
    Handler handler;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    public static final int STATE_CONNECTION_FAILED = 4; // connection failed to remote device
    public static final int STATE_MSG_RECEIVED = 5; // msg received from remote device

    AcceptThreadServerClass acceptThread;
    ConnectThreadClientClass connectThread;
    ConnectedThreadClass connectedThread;


    public CommonChatApplication(Context context, Handler handler){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE ;
        this.context = context;
        this.handler = handler;
    }


    public synchronized void initiateServer(){
        if (connectThread!=null){
            connectThread.close();
            connectThread = null;
        }

        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread= null;
        }
        if (acceptThread == null) {
            acceptThread = new AcceptThreadServerClass();
            acceptThread.start();
        }

        Message msg = Message.obtain();
        msg.what = STATE_LISTEN;
        handler.sendMessage(msg);
        mState = STATE_LISTEN;

    }

    public synchronized void initiateClient(BluetoothDevice device){
        if (acceptThread!=null){
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread= null;
        }
        //if (connectThread == null) {
            connectThread = new ConnectThreadClientClass(device);
            connectThread.start();
        //}

        mState = STATE_CONNECTING;
        Message msg = Message.obtain();
        msg.what = STATE_CONNECTING;
        handler.sendMessage(msg);
    }

    public synchronized void initiateChat(BluetoothSocket socket) {
        if (connectThread!=null){
            connectThread.close();
            connectThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThreadClass(socket);
        connectedThread.start();

        sendMsg("this is msg2".getBytes());

    }
    public synchronized void sendMsg(byte[] msg){
        connectedThread.write(msg);
    }


    //class to send and receive message after successful BT connection
    private class ConnectedThreadClass extends Thread{
        BluetoothSocket bluetoothSocket;
        InputStream inputStream;
        OutputStream outputStream;

        public ConnectedThreadClass(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn =bluetoothSocket.getInputStream();
                tmpOut =bluetoothSocket.getOutputStream();
            }catch (IOException e){e.printStackTrace();}

            inputStream = tmpIn ;
            outputStream = tmpOut ;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            //infinite loop bcz we are always listening to new msg
            while (true){
                try {
                    bytes = inputStream.read(buffer);

                    /*Message msg = Message.obtain();
                    msg.what = STATE_MSG_RECEIVED;*/
                    handler.obtainMessage(STATE_MSG_RECEIVED, bytes, -1 , buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }






    //server class to listen/accept bluetooth request
    private class AcceptThreadServerClass extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThreadServerClass(){
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME , UNIQUE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThreadServerClass: listen() Failed : " + e );
            }
            bluetoothServerSocket = tmp ;
        }

        public void run() {

            setName("AcceptThread");
            BluetoothSocket bluetoothSocket = null;

            while(mState !=STATE_CONNECTED){//bluetoothSocket == null){// while (mState !=STATE_CONNECTED){

                try {
                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTING;
                    handler.sendMessage(msg);
                    mState = STATE_CONNECTING;

                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "run: acceptThreadException : " + e );

                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(msg);
                    mState = STATE_CONNECTION_FAILED;


                    break;
                }

                //if connection request is accepted
                if (bluetoothSocket !=null){

                   initiateChat( bluetoothSocket);

                    Message msg = Message.obtain();
                    msg.what = STATE_CONNECTED;
                    handler.sendMessage(msg);

                    Log.d(TAG, "run: Server Thread: A connnection has been accepted");
                    mState= STATE_CONNECTED;
                    break;
                }


            }
            /*try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "run: serverThread : cant close server socket");
            }*/
        }

        public void cancel(){
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() server failed" );
            }
        }

    }





    //client class to request bluetooth connectivity
    private class ConnectThreadClientClass extends Thread{
        private BluetoothSocket bluetoothSocket;
        private BluetoothDevice bluetoothDevice;

        public ConnectThreadClientClass(BluetoothDevice device){
            bluetoothDevice = device ;
            BluetoothSocket tmp= null;
            try{
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(UNIQUE_UUID);
            }catch (Exception e){
                Log.e(TAG, "ConnectThreadClientClass: connectionRequest() Failed : "+e );
            }
            bluetoothSocket = tmp ;
        }

        //connect() must called in saperate thread
        public void run() {
            setName("ConnectThread");

           // bluetoothAdapter.cancelDiscovery();
            try {
                    if (bluetoothSocket != null) {

                        bluetoothSocket.connect();
                        Log.d(TAG, "run: connected to device");

                        Message msg = Message.obtain();
                        msg.what = STATE_CONNECTED;
                        handler.sendMessage(msg);
                        initiateChat(bluetoothSocket);

                        mState = STATE_CONNECTED;


                    }else{
                        Log.d(TAG, "Connect Thread ,run: socket is null");
                    }


            } catch (IOException e) { //try the fallback

                    try {

                        Log.e("","trying fallback...");

                        bluetoothSocket =(BluetoothSocket) bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bluetoothDevice,1);
                        bluetoothSocket.connect();

                        Log.e("","Connected");

                        Message msg = Message.obtain();
                        msg.what = STATE_CONNECTED;
                        handler.sendMessage(msg);
                        mState = STATE_CONNECTED;

                        initiateChat(bluetoothSocket);



                        } catch (Exception e1) {//ioexcept

                       // bluetoothSocket.close();

                        Message msg = Message.obtain();
                        msg.what = STATE_CONNECTION_FAILED;
                        handler.sendMessage(msg);
                        mState = STATE_CONNECTION_FAILED;

                            Log.e(TAG, "run: connectionSocketError", e);
                        } /*catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    }*/
                    //fallback ends here


               /* Message msg = Message.obtain();
                msg.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(msg);

                mState = STATE_CONNECTION_FAILED;
                Log.e(TAG, "run: connectionThreadError", e);*/


                }
        }

        public void close(){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close: socketCloseError" + e );
            }
        }

    }


}

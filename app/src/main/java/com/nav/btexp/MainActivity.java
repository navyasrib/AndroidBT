package com.nav.btexp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Runnable {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final String TAG = "TAG";
    private BluetoothAdapter bluetooth;
    private Set<BluetoothDevice> pairedDevices;
    private ListView listView;
    private ArrayAdapter<DeviceItem> mAdapter = null;
    private ArrayList<String> mDeviceList = new ArrayList<String>();
    private UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog mBluetoothConnectProgressDialog;
    private BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;
//    private BluetoothSocket fallbackSocket;
    private DataOutputStream os;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        listView = (ListView) findViewById(R.id.listView);
    }

    public void turnOnBt(View view) {
        if (!bluetooth.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void getAvailableDevices(View view) {
//        bluetooth.startDiscovery();

//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter);
//        listPairedDevices();
        Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
    }

//    private void listPairedDevices() {
//        pairedDevices = bluetooth.getBondedDevices();
//        ArrayList<String> devicesList = new ArrayList<String>();
//        for (BluetoothDevice bt : pairedDevices) {
//            devicesList.add(bt.getName());
//        }
//        Toast.makeText(getApplicationContext(), "Showing available devices", Toast.LENGTH_SHORT).show();
//        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devicesList);
//        listView.setAdapter(adapter);
//    }

    public void onActivityResult(int mRequestCode, int mResultCode, Intent mDataIntent) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);

        switch (mRequestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (mResultCode == Activity.RESULT_OK) {
                    String mDeviceAddress = mDataIntent.getExtras().getString("DeviceAddress");
                    Log.v(TAG, "Coming incoming address " + mDeviceAddress);
                    mBluetoothDevice = bluetooth.getRemoteDevice(mDeviceAddress);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this, "Connecting...", mBluetoothDevice.getName() + " : " + mBluetoothDevice.getAddress(), true, false);
                    Thread mBlutoothConnectThread = new Thread(this);
                    mBlutoothConnectThread.start();
                }
                break;
        }
    }

    public void run() {
        if (connectToDevice()) {
            mHandler.sendEmptyMessage(0);
            sendMessageToConnectedDevice();
        } else {
            Log.d(TAG, "device not connected");
        }
//        try {
//            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
//            Class<?> clazz = mBluetoothSocket.getRemoteDevice().getClass();
//            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
//
//            Method m = clazz.getMethod("createRfcommSocket", paramTypes);
//            Object[] params = new Object[]{Integer.valueOf(1)};
//
//            fallbackSocket = (BluetoothSocket) m.invoke(mBluetoothSocket.getRemoteDevice(), params);
//            fallbackSocket.connect();
//
//            mHandler.sendEmptyMessage(0);
//            os = new DataOutputStream(fallbackSocket.getOutputStream());
//            new clientSock().start();
//        } catch (Exception e) {
//            e.printStackTrace();
//            closeSocket(mBluetoothSocket);
//            Log.e("BLUETOOTH", e.getMessage());
//        }
//        try {
//            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
//            bluetooth.cancelDiscovery();
//            Class<? extends BluetoothDevice> clazz = mBluetoothSocket.getRemoteDevice().getClass();
//            Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
//            Method m = clazz.getMethod("createRfcommSocket", paramTypes);
//            Object[] params = new Object[] {Integer.valueOf(1)};
//            fallbackSocket = (BluetoothSocket) m.invoke(mBluetoothSocket.getRemoteDevice(), params);
//            mBluetoothSocket.connect();
//        mHandler.sendEmptyMessage(0);
//            sendStringToConnectedDevice();
//        } catch (IOException eConnectException) {
//            Log.d(TAG, "CouldNotConnectToSocket", eConnectException);
//            closeSocket(mBluetoothSocket);
//            return;
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
    }

    private boolean connectToDevice() {
        bluetooth.cancelDiscovery();
        if (mBluetoothSocket!= null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BluetoothDevice device = bluetooth.getRemoteDevice(mBluetoothDevice.getAddress());
            Method m = device.getClass().getMethod("createRfcommSocket", int.class);
            mBluetoothSocket = (BluetoothSocket) m.invoke(device, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mBluetoothSocket == null)
            return false;
        try {
            mBluetoothSocket.connect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                bluetooth.cancelDiscovery();
                Class<? extends BluetoothDevice> clazz = mBluetoothSocket.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                mBluetoothSocket = (BluetoothSocket) m.invoke(mBluetoothSocket.getRemoteDevice(), params);
                mBluetoothSocket.connect();
            } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e1) {
                e1.printStackTrace();
            }
            closeSocket(mBluetoothSocket);
        }
        return false;
    }

    private void sendMessageToConnectedDevice() {
        try {
            os = new DataOutputStream(mBluetoothSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        new clientSock().start();
    }

    public class clientSock extends Thread {
        public void run() {
            try {
                os.writeBytes("anything you want"); // anything you want
                os.flush();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private void sendStringToConnectedDevice() {
//        try {
////            InputStream iStream = fallbackSocket.getInputStream();
////            OutputStream oStream = fallbackSocket.getOutputStream();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void closeSocket(BluetoothSocket nOpenSocket) {
        try {
            nOpenSocket.close();
            Log.d(TAG, "SocketClosed");
        } catch (IOException ex) {
            Log.d(TAG, "CouldNotCloseSocket");
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mBluetoothConnectProgressDialog.dismiss();
            Toast.makeText(MainActivity.this, "DeviceConnected", Toast.LENGTH_LONG).show();
        }
    };
}

//    @Override
//    protected void onDestroy() {
//        unregisterReceiver(mReceiver);
//        super.onDestroy();
//    }

//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent
//                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                mDeviceList.add(device.getName() + "\n" + device.getAddress());
//                Log.i("BT", device.getName() + "\n" + device.getAddress());
//                listView.setAdapter(new ArrayAdapter<String>(context,
//                        android.R.layout.simple_list_item_1, mDeviceList));
//            }
//        }
//    };


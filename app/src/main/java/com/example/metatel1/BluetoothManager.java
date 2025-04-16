package com.example.metatel1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static BluetoothManager instance;
    private static final String TAG = "BluetoothManager";
    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private volatile boolean isReading = false;
    private Thread readThread;
    private BluetoothDataCallback dataCallback;
    private ConnectionStateListener connectionStateListener;


    private BluetoothManager(Context context) {
        this.context = context.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothManager getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothManager(context);
        }
        return instance;
    }

    public void setCallback(BluetoothDataCallback callback) {
        this.dataCallback = callback;
    }

    public boolean connectToDevice(String deviceName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not supported or not enabled");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.e(TAG, "Bluetooth permissions not granted");
                return false;
            }
        }

        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            Log.d(TAG, "Already connected");
            return true;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothSocket.connect();
                    inputStream = bluetoothSocket.getInputStream();
                    Log.d(TAG, "Connected to " + deviceName);
                    notifyConnectionStateChanged(true);
                    startReading();
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed", e);
                    notifyConnectionStateChanged(false);
                    return false;
                }
            }
        }

        Log.e(TAG, "Device with name " + deviceName + " not found");
        return false;
    }

    private void startReading() {
        if (inputStream == null) {
            Log.e(TAG, "Input stream is null");
            return;
        }

        isReading = true;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isReading) {
                try {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        String data = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received: " + data);

                        BluetoothDataCallback callback = dataCallback;
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onDataReceived(data));
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in reading thread", e);
                    notifyConnectionStateChanged(false);
                    break;
                }
            }

            Log.d(TAG, "Reading thread stopped.");
        });

        readThread.setDaemon(true);
        readThread.start();
    }

    public void stopReading() {
        isReading = false;
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
    }

    public void closeConnection() {
        stopReading();
        try {
            if (inputStream != null) inputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Log.d(TAG, "Connection closed");
            notifyConnectionStateChanged(false);
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection", e);
        }
    }

    public void setConnectionStateListener(ConnectionStateListener listener) {
        this.connectionStateListener = listener;
    }

    private void notifyConnectionStateChanged(boolean isConnected) {
        if (connectionStateListener != null) {
            connectionStateListener.onConnectionStateChanged(isConnected);
        }
    }

    public interface BluetoothDataCallback {
        void onDataReceived(String data);
    }
    public interface ConnectionStateListener {
        void onConnectionStateChanged(boolean isConnected);
    }
}

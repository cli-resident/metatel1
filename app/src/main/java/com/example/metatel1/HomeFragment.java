package com.example.metatel1;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.Manifest;
import android.widget.TextView;


public class HomeFragment extends Fragment implements BluetoothManager.ConnectionStateListener{
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    private final String[] bluetoothPermissions = new String[] {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
    };
    boolean connected = false;
    private boolean connectionAttempted = false;
    TextView connection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_fragment, container, false);
        Button BTButton = root.findViewById(R.id.BTButton);
        Button EnterButton = root.findViewById(R.id.EnterButton);
        connection = root.findViewById(R.id.connection);
        BluetoothManager manager = BluetoothManager.getInstance(getContext());
        manager.setConnectionStateListener(this);
        BTButton.setOnClickListener(v -> {
            connectionAttempted = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(bluetoothPermissions, REQUEST_BLUETOOTH_PERMISSIONS);
                    return;
                }
            }
            manager.closeConnection();
            manager.connectToDevice("METATEL");
        });


        EnterButton.setOnClickListener(v -> {
            if(connected){
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_homeFragment_to_mapFragment);}
            else{
                connection.setText("Подключитесь перед использованием!");
            }
        });
        Button menuButton = root.findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_settings) {
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.action_homeFragment_to_settingsFragment);
                    return true;
                }
                return false;
            });
            popup.show();
        });


        return root;
    }

    @Override
    public void onConnectionStateChanged(boolean isConnected) {
        connected = isConnected;
        connection.setText(isConnected ? "Подключено" : "Не подключено");
        if(connected){
            connection.setText("Подключено");
            connection.setTextColor(getResources().getColor(R.color.green));

        }else {
            connection.setText("Не подключено");
            connection.setTextColor(getResources().getColor(R.color.red));
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (connectionAttempted) {
            BluetoothManager manager = BluetoothManager.getInstance(getContext());
            connected = manager.isConnected();
            if (connected) {
                connection.setText("Подключено");
                connection.setTextColor(getResources().getColor(R.color.green));
            } else {
                connection.setText("Не подключено");
                connection.setTextColor(getResources().getColor(R.color.red));
            }
        }
    }
}
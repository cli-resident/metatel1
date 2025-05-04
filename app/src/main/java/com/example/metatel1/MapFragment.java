package com.example.metatel1;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.InputStream;
import java.util.*;

public class MapFragment extends Fragment implements BluetoothManager.ConnectionStateListener {

    private static final int MODE_SET_SCALE = 0;
    private static final int MODE_SET_CENTER = 1;
    private static final int MODE_SET_FIRE = 2;
    private static final int MODE_SET_AIM = 3;
    private int currentMode = MODE_SET_CENTER;

    private final List<PointF> scalePoints = new ArrayList<>();
    private final List<PointF> firePoints = new ArrayList<>();
    private PointF centerPoint = null;
    private PointF aimPoint = null;

    private double scale = 0;
    private double angle = 45;
    private double azimuth = 0;
    private double speed = 10;
    private double speedFix = 0;
    private double azimuthFix = 0;
    private float radiusPixels = 0;
    private Shell selectedShell = null;

    private MapView mapView;
    private EditText angleInput, azimuthInput, distanceInput;
    private TextView scaleLabel, modeLabel, deltaLabel, speedValue, connectionStatus, deltaLabelspeed;
    private SeekBar speedSeekBar;
    private ImageView cursor;

    private static final int REQUEST_IMAGE = 1001;
    private BluetoothManager manager;

    private final BluetoothManager.BluetoothDataCallback callback = data -> {
        processSerial(data);
        updateRadiusDisplay();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.map_fragment, container, false);
        manager = BluetoothManager.getInstance(requireContext());
        manager.setCallback(callback);
        manager.setConnectionStateListener(this);
        cursor = root.findViewById(R.id.cursor);
        cursor.setVisibility(View.GONE);
        mapView = root.findViewById(R.id.mapView);
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    cursor.setVisibility(View.VISIBLE);
                    float cursorX = event.getX() - cursor.getWidth();
                    float cursorY = event.getY();
                    cursor.setX(cursorX);
                    cursor.setY(cursorY);
                    break;
                case MotionEvent.ACTION_UP:
                    float pointX = event.getX()-cursor.getWidth();
                    float pointY = event.getY()-cursor.getHeight();
                    handleMapTouch(pointX, pointY);
                    cursor.setVisibility(View.GONE);
                    break;
            }
            return true;
        });


        scaleLabel = root.findViewById(R.id.scaleLabel);
        modeLabel = root.findViewById(R.id.modeLabel);
        deltaLabel = root.findViewById(R.id.deltaLabel);
        speedSeekBar = root.findViewById(R.id.speedSeekBar);
        speedValue = root.findViewById(R.id.speedValue);
        deltaLabelspeed = root.findViewById(R.id.deltaLabelspeed);

        connectionStatus = root.findViewById(R.id.connectionStatus);
        connectionStatus.setText("Подключено");
        connectionStatus.setTextColor(getResources().getColor(R.color.green));


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // запрос фотки
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE); // обработка получения фотки \/


        root.findViewById(R.id.resetBtn).setOnClickListener(v -> resetAll());

        root.findViewById(R.id.scaleModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_SCALE;
            modeLabel.setText("Режим: Выбор масштаба");
            scalePoints.clear();
            mapView.setScalePoints(scalePoints);
        });

        root.findViewById(R.id.centerModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_CENTER;
            modeLabel.setText("Режим: Выбор точки стрельбы");
        });

        root.findViewById(R.id.fireModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_FIRE;
            modeLabel.setText("Режим: Выбор точки прилета");
        });
        root.findViewById(R.id.aimModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_AIM;
            modeLabel.setText("Режим: Выбор цели");
        });

        root.findViewById(R.id.shellbutton).setOnClickListener(v -> {
            List<Shell> shells = Shell.loadAll(requireContext());
            if (shells == null || shells.isEmpty()) return;

            PopupMenu popup = new PopupMenu(requireContext(), v);

            for (int i = 0; i < shells.size(); i++) {
                Shell shell = shells.get(i);
                popup.getMenu().add(0, i, i, shell.getName());
            }

            popup.setOnMenuItemClickListener(item -> {
                selectedShell = shells.get(item.getItemId());
                mapView.setShellRadius((float) (selectedShell.getRadius()/scale));
                speed = selectedShell.getSpeed();
                speedSeekBar.setProgress((int)speed);
                Toast.makeText(requireContext(), selectedShell.getName()+selectedShell.getRadius(), Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress + speedFix;
                speedValue.setText("Скорость,м/с:" + speed);
                updateRadiusDisplay();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return root;
    }

    private void updateRadiusDisplay() { // обновление радиуса
        double range = (speed * speed * Math.sin(Math.toRadians(2 * angle))) / 9.81;
        radiusPixels = (float) (scale > 0 ? range / scale : range);
        mapView.setRadius(radiusPixels);
        mapView.setAzimuth(azimuth, azimuthFix);
        mapView.invalidate();
    }

    private void resetAll() { // сброс параметров
        scale = 0;
        speedFix = 0;
        azimuthFix = 0;
        centerPoint = null;
        firePoints.clear();
        scalePoints.clear();
        mapView.setFirePoints(firePoints);
        mapView.setCenterPoint(null);
        mapView.setScalePoints(scalePoints);
        mapView.setAimPoint(null);
        mapView.setShellRadius(0);
        mapView.setRadius(0);
        mapView.clearCalibrationLines();
        mapView.invalidate();
        scaleLabel.setText("Масштаб: Не задан");
        deltaLabel.setText("Поправка адимута: 0.00°");
        deltaLabelspeed.setText("Поправка скорости: 0.00");
    }

    private void handleMapTouch(float x, float y) { // обработка касания и передача/обработка MapView
        PointF point = new PointF(x, y);

        if (currentMode == MODE_SET_SCALE) {
            scalePoints.add(point);
            mapView.setScalePoints(scalePoints);
            if (scalePoints.size() == 2) {
                showDistanceDialog(scalePoints.get(0), scalePoints.get(1));
            }
        } else if (currentMode == MODE_SET_CENTER) {
            centerPoint = point;
            mapView.setCenterPoint(centerPoint);
            updateRadiusDisplay();
        } else if (currentMode == MODE_SET_FIRE && centerPoint != null) {
            firePoints.add(point);
            mapView.setFirePoints(firePoints);

            double dx = point.x - centerPoint.x;
            double dy = point.y - centerPoint.y;
            double distance = Math.hypot(dx, dy);

            double actualSpeed = speed / Math.sqrt(radiusPixels / distance);
            String formatted = String.format(Locale.US, "%.2f", actualSpeed - speed);
            speedFix = Double.parseDouble(formatted);
            speed = speed + speedFix;


            double baseAz = Math.toRadians(azimuth);
            double vx = dx, vy = dy;
            double len = Math.hypot(vx, vy);
            double dirX = Math.sin(baseAz), dirY = -Math.cos(baseAz);
            double dot = vx * dirX + vy * dirY;
            double angleBetween = Math.acos(dot / len);
            double cross = vx * dirY - vy * dirX;
            azimuthFix = Math.toDegrees(cross < 0 ? angleBetween : -angleBetween);

            deltaLabel.setText(String.format("Поправка азимута: %.2f°", azimuthFix));
            deltaLabelspeed.setText(String.format("Поправка скорости: %.2f", speedFix));
            updateRadiusDisplay();
        }
        else if (currentMode == MODE_SET_AIM) {
            aimPoint = point;
            mapView.setAimPoint(aimPoint);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE) return;

        if (resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try (InputStream stream = requireContext().getContentResolver().openInputStream(uri)) {
                Bitmap src = BitmapFactory.decodeStream(stream);
                if (src == null) return;
                try (InputStream exifSt = requireContext().getContentResolver().openInputStream(uri)) {
                    int orient = new ExifInterface(exifSt).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    src = rotateBitmapIfRequired(src, orient);}
                final Bitmap bmp = src;
                mapView.post(() -> {
                    mapView.setMapBitmap(bmp);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
            resetAll();
        } else {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_mapFragment_to_homeFragment);
        }
    }

    private Bitmap rotateBitmapIfRequired(Bitmap bitmap, int orientation) { // поворот по exif
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
    private void processSerial(String line) {       // парс данных с блютус серийника
        if (line == null || line.isEmpty()) return;

        String[] parts = line.split(",");
        for (String p : parts) {
            p = p.trim();

            if (p.startsWith("H:")) {
                try {
                    azimuth = Double.parseDouble(p.substring(2));
                } catch (NumberFormatException ignored) {
                }
            } else if (p.startsWith("P:")) {
                try {
                    angle = -1*Double.parseDouble(p.substring(2));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @Override
    public void onConnectionStateChanged(boolean isConnected) {
        connectionStatus = requireView().findViewById(R.id.connectionStatus);
        if (isConnected) {
            connectionStatus.setText("Подключено");
            connectionStatus.setTextColor(getResources().getColor(R.color.green));
        } else {
            connectionStatus.setText("Не подключено");
            connectionStatus.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private void showDistanceDialog(PointF p1, PointF p2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Введите расстояние (в метрах)");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = input.getText().toString();
                if (!value.isEmpty()) {
                    double distM = Double.parseDouble(value);
                    double distPx = Math.hypot(p2.x - p1.x, p2.y - p1.y);
                    scale = distM / distPx;
                    mapView.setRealScale((float)scale);
                    scaleLabel.setText(String.format("Масштаб: %.2f m/px", scale));
                    mapView.addCalibrationLine(p1, p2);
                    scalePoints.clear();
                    mapView.setScalePoints(scalePoints);
                }
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

}


package com.example.metatel1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.InputStream;
import java.util.*;

public class MapFragment extends Fragment {

    private static final int MODE_SET_SCALE = 0;
    private static final int MODE_SET_CENTER = 1;
    private static final int MODE_SET_FIRE = 2;
    private int currentMode = MODE_SET_SCALE;

    private final List<PointF> scalePoints = new ArrayList<>();
    private final List<PointF> firePoints = new ArrayList<>();
    private PointF centerPoint = null;

    private double scale = 0;
    private double angle = 45;
    private double azimuth = 0;
    private double speed = 10;
    private double speedFix = 0;
    private double azimuthFix = 0;
    private float radiusPixels = 0;

    private MapView mapView;
    private EditText angleInput, azimuthInput, distanceInput;
    private TextView scaleLabel, modeLabel, deltaLabel,speedValue;
    private SeekBar speedSeekBar;

    private static final int REQUEST_IMAGE = 1001;
    private BluetoothManager manager;

    private final BluetoothManager.BluetoothDataCallback callback = data -> {
        Log.d("MapFragment", "Получено: " + data);
        processSerial(data);
        updateRadiusDisplay();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.map_fragment, container, false);
        manager = BluetoothManager.getInstance(requireContext());
        manager.setCallback(callback);
        // Bind UI
        mapView = root.findViewById(R.id.mapView);
        mapView.setOnMapTouchListener(this::handleMapTouch);

        scaleLabel = root.findViewById(R.id.scaleLabel);
        modeLabel = root.findViewById(R.id.modeLabel);
        deltaLabel = root.findViewById(R.id.deltaLabel);
        angleInput = root.findViewById(R.id.angleInput);
        azimuthInput = root.findViewById(R.id.azimuthInput);
        distanceInput = root.findViewById(R.id.distanceInput);
        speedSeekBar = root.findViewById(R.id.speedSeekBar);
        speedValue = root.findViewById(R.id.speedValue);


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // запрос фотки
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE); // обработка получения фотки \/


        root.findViewById(R.id.resetBtn).setOnClickListener(v -> resetAll());

        root.findViewById(R.id.scaleModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_SCALE;
            modeLabel.setText("Mode: Set Scale");
            scalePoints.clear();
            mapView.setScalePoints(scalePoints);
        });

        root.findViewById(R.id.centerModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_CENTER;
            modeLabel.setText("Mode: Set Center");
        });

        root.findViewById(R.id.fireModeBtn).setOnClickListener(v -> {
            currentMode = MODE_SET_FIRE;
            modeLabel.setText("Mode: Set Fire Point");
        });

        angleInput.setOnFocusChangeListener((v, f) -> {
            if (!f) try {
                angle = Double.parseDouble(angleInput.getText().toString());
                updateRadiusDisplay();
            } catch (Exception ignored) {}
        });

        azimuthInput.setOnFocusChangeListener((v, f) -> {
            if (!f) try {
                azimuth = Double.parseDouble(azimuthInput.getText().toString());
                updateRadiusDisplay();
            } catch (Exception ignored) {}
        });

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress + speedFix;
                speedValue.setText("Speed: " + speed);
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
        mapView.setRadius(0);

        // Удаляем линии калибровки
        mapView.clearCalibrationLines();

        mapView.invalidate();

        scaleLabel.setText("Scale: Not set");
        deltaLabel.setText("delta azimuth: 0.00°, delta speed: 0.00");
    }

    private void handleMapTouch(float x, float y) { // обработка касания и передача/обработка MapView
        PointF point = new PointF(x, y);

        if (currentMode == MODE_SET_SCALE && !(distanceInput.getText().toString().isEmpty())) {
            scalePoints.add(point);
            mapView.setScalePoints(scalePoints);
            if (scalePoints.size() == 2) {
                PointF p1 = scalePoints.get(0), p2 = scalePoints.get(1);
                double distPx = Math.hypot(p2.x - p1.x, p2.y - p1.y);
                double distM = Double.parseDouble(distanceInput.getText().toString());
                scale = distM / distPx;
                scaleLabel.setText(String.format("Scale: %.2f m/px", scale));
                mapView.addCalibrationLine(p1, p2);
                scalePoints.clear();
                mapView.setScalePoints(scalePoints);
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


            double baseAz = Math.toRadians(azimuth);
            double vx = dx, vy = dy;
            double len = Math.hypot(vx, vy);
            double dirX = Math.sin(baseAz), dirY = -Math.cos(baseAz);
            double dot = vx * dirX + vy * dirY;
            double angleBetween = Math.acos(dot / len);
            double cross = vx * dirY - vy * dirX;
            azimuthFix = Math.toDegrees(cross < 0 ? angleBetween : -angleBetween);

            deltaLabel.setText(String.format("delta azimuth: %.2f°, delta speed: %.2f", azimuthFix, speedFix));
            updateRadiusDisplay();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // получение и обработка фотки
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try (InputStream stream = getActivity().getContentResolver().openInputStream(uri)) {
                    Bitmap bmp = BitmapFactory.decodeStream(stream);
                    if (bmp != null) {
                        InputStream exifStream = getActivity().getContentResolver().openInputStream(uri); // проверка ориентации через exif (костыль)
                        ExifInterface exif = new ExifInterface(exifStream);
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        exifStream.close();
                        bmp = rotateBitmapIfRequired(bmp, orientation); // поворот если чето не так

                        int mapViewWidth = mapView.getWidth(); // масштаб под View
                        int mapViewHeight = mapView.getHeight();
                        float imageAspect = (float) bmp.getWidth() / bmp.getHeight();
                        float mapViewAspect = (float) mapViewWidth / mapViewHeight;
                        int targetWidth, targetHeight;
                        if (imageAspect > mapViewAspect) {
                            targetWidth = mapViewWidth;
                            targetHeight = (int) (mapViewWidth / imageAspect);
                        } else {
                            targetHeight = mapViewHeight;
                            targetWidth = (int) (mapViewHeight * imageAspect);
                        }
                        bmp = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true);
                        mapView.setMapBitmap(bmp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                resetAll();
            } else {
                NavController navController = Navigation.findNavController(requireView()); // кидаем обратно если не выбрали фото
                navController.navigate(R.id.action_mapFragment_to_homeFragment);
            }
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
}


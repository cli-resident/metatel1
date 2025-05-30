package com.example.metatel1;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MapView extends View {
    private Bitmap mapBitmap;
    private final List<PointF> scalePoints = new ArrayList<>();
    private final List<PointF[]> calibrationLines = new ArrayList<>();
    private PointF centerPoint = null;

    private PointF aimPoint = null;
    private final List<PointF> firePoints = new ArrayList<>();
    private float radiusPixels = 0f;
    private float shellRadiusPixels = 0f;
    private double azimuth = 0.0;
    private double azimuthFix = 0.0;
    public float scaleMetersPerPixel = 0f;

    double totalAzimuth;
    double rad;
    float x2;
    float y2;
    float center_x;
    float center_y;
    float scale;

    // Отрисовка маркеров с уменьшением их размера
    private Paint paintRed, paintBlue, paintGreen, paintYellow, paintCyan;
    Bitmap mark2Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mark2);
    Bitmap mark1Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mark1);
    // Уменьшаем масштаб изображения
    int newmark2Width = mark2Bitmap.getWidth() / 2; // Новая ширина (например, в 2 раза меньше)
    int newmark2Height = mark2Bitmap.getHeight() / 2; // Новая высота (например, в 2 раза меньше)
    Bitmap scaledMark2Bitmap = Bitmap.createScaledBitmap(mark2Bitmap, newmark2Width, newmark2Height, true);
    float mark2X = newmark2Width / 2f; // Центрирование по горизонтали
    int newmark1Width = mark1Bitmap.getWidth() / 2; // Новая ширина (например, в 2 раза меньше)
    int newmark1Height = mark1Bitmap.getHeight() / 2; // Новая высота (например, в 2 раза меньше)
    Bitmap scaledMark1Bitmap = Bitmap.createScaledBitmap(mark1Bitmap, newmark1Width, newmark1Height, true);
    float mark1X = newmark1Width / 2f; // Центрирование по горизонтали

    public interface OnMapTouchListener {
        void onMapTouched(float x, float y);
    }

    private OnMapTouchListener touchListener;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    // Назначение цветовых атрибутов линиям
    private void init() {
        paintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(3f);
        paintRed.setStyle(Paint.Style.STROKE);

        paintBlue = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBlue.setColor(Color.BLUE);
        paintBlue.setStrokeWidth(3f);
        paintBlue.setStyle(Paint.Style.STROKE);

        paintGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGreen.setColor(Color.GREEN);
        paintGreen.setStrokeWidth(3f);
        paintGreen.setStyle(Paint.Style.STROKE);

        paintYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3f);
        paintYellow.setStyle(Paint.Style.STROKE);

        paintCyan = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCyan.setColor(Color.CYAN);
        paintCyan.setStrokeWidth(3f);
        paintCyan.setStyle(Paint.Style.STROKE);
    }

    public void setMapBitmap(Bitmap bitmap) {
        this.mapBitmap = bitmap;
        invalidate();
    }

    public void setScalePoints(List<PointF> points) {
        scalePoints.clear();
        scalePoints.addAll(points);
        invalidate();
    }
    public void setAimPoint(PointF aimPoint) {
        this.aimPoint = aimPoint;
        invalidate();
    }

    public void addCalibrationLine(PointF p1, PointF p2) {
        calibrationLines.add(new PointF[]{p1, p2});
        invalidate();
    }

    public void clearCalibrationLines() {
        calibrationLines.clear();
        invalidate();
    }

    public void setRealScale(float metersPerPixel) {
        this.scaleMetersPerPixel = metersPerPixel;
        invalidate();
    }

    public void setCenterPoint(PointF center) {
        this.centerPoint = center;
        invalidate();
    }

    public void setFirePoints(List<PointF> points) {
        firePoints.clear();
        firePoints.addAll(points);
        invalidate();
    }

    public void setRadius(float radius) {
        this.radiusPixels = radius;
        invalidate();
    }

    public void setAzimuth(double azimuth, double azimuthFix) {
        this.azimuth = azimuth;
        this.azimuthFix = azimuthFix;
        invalidate();
    }
    public void setShellRadius(float radius) {
        this.shellRadiusPixels = radius;
        invalidate();
    }

    public void setOnMapTouchListener(OnMapTouchListener listener) {
        this.touchListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);

        if (mapBitmap != null) {
            float vw = getWidth();
            float vh = getHeight();
            float bw = mapBitmap.getWidth();
            float bh = mapBitmap.getHeight();
            float scale = Math.min(vw / bw, vh / bh);
            float dw = bw * scale;
            float dh = bh * scale;
            float left = (vw - dw) / 2f;
            float top = (vh - dh) / 2f;
            RectF dst = new RectF(left, top, left + dw, top + dh);
            canvas.drawBitmap(mapBitmap, null, dst, null);
        }

        // установка отрезка для вычисления масштаба
        for (PointF[] line : calibrationLines) {
            PointF p1 = line[0], p2 = line[1];
            canvas.drawCircle(p1.x, p1.y, 5f, paintRed);
            canvas.drawCircle(p2.x, p2.y, 5f, paintRed);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintYellow);
        }
        // отрисовка окружности с центром в точке стрельбы и радиусом, соответствующим скорости выстрела и углу наклона
        for (PointF p : scalePoints) canvas.drawCircle(p.x, p.y, 5f, paintRed);
        if (centerPoint != null) {
            totalAzimuth = azimuth + azimuthFix;
            rad = Math.toRadians(totalAzimuth);
            x2 = (float) (centerPoint.x + radiusPixels * Math.sin(rad));
            y2 = (float) (centerPoint.y - radiusPixels * Math.cos(rad));
            center_x = centerPoint.x;
            center_y = centerPoint.y;
            canvas.drawBitmap(scaledMark1Bitmap, centerPoint.x - mark1X, centerPoint.y, null);
            canvas.drawCircle(centerPoint.x, centerPoint.y, radiusPixels, paintRed);
            canvas.drawLine(centerPoint.x, centerPoint.y, x2, y2, paintBlue);
        }

        for (PointF p : firePoints) //указываем место, куда попали
            canvas.drawCircle(p.x, p.y, 5f, paintCyan);
        // проверка попадания в окружность, соответствующую радиусу разлета, установленному в характеристиках снаряда
        if (shellRadiusPixels != 0f) {
            Paint radiusPaint = paintBlue;
            if (aimPoint != null) {
                float distance = (float) Math.sqrt(Math.pow(aimPoint.x - x2, 2) + Math.pow(aimPoint.y - y2, 2));
                if (distance <= shellRadiusPixels) {
                    radiusPaint = paintGreen;
                }
            }
            canvas.drawCircle(x2, y2, shellRadiusPixels, radiusPaint);
        }

        if (aimPoint != null) {
            canvas.drawCircle(aimPoint.x, aimPoint.y, 10f, paintBlue);
            canvas.drawBitmap(scaledMark2Bitmap, aimPoint.x - mark2X, aimPoint.y, null);
        }
// отрисовка горизонтальной линейки масштаба
        if (scaleMetersPerPixel > 0) {
            Paint scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scalePaint.setColor(Color.MAGENTA);
            scalePaint.setStrokeWidth(2f);
            scalePaint.setTextSize(30f);
            scalePaint.setStyle(Paint.Style.FILL);

            int segmentsX = 5;
            int segmentsY = 7;
            float scaleBarLengthPixels = 1000f;
            float segmentLengthPixelsX = scaleBarLengthPixels / segmentsX;
            float segmentLengthPixelsY = scaleBarLengthPixels / segmentsY;
            float segmentLengthMetersX = segmentLengthPixelsX * scaleMetersPerPixel;
            float segmentLengthMetersY = segmentLengthPixelsY * scaleMetersPerPixel;
            float startY_line = getHeight() - 70f;
            float startX_line = 20f;

            float centerX = center_x; // Используем center_x для центральной точки
            float centerY = center_y; // Используем center_y для центральной точки

            // Рисуем центральную отметку X
            canvas.drawLine(centerX, startY_line - 10f, centerX, startY_line + 10f, scalePaint);
            canvas.drawText("0 м", centerX, startY_line - 20f, scalePaint);

            // Рисуем центральную отметку Y
            canvas.drawLine(startX_line - 10f, centerY,startX_line + 10f, centerY, scalePaint);
            canvas.drawText("0 м", startX_line + 5f, centerY - 15f, scalePaint);

            // Рисуем сегменты слева от центра
            float currentX = centerX - segmentLengthPixelsX;
            int segmentCountX = 1;
            while (currentX > 0) {
                canvas.drawLine(currentX, startY_line - 10f, currentX, startY_line + 10f, scalePaint);
                String scaleText = String.format("%.0f м", -segmentCountX * segmentLengthMetersX);
                canvas.drawText(scaleText, currentX, startY_line - 20f, scalePaint);
                currentX -= segmentLengthPixelsX;
                segmentCountX++;
            }
            // Рисуем сегменты сверху от центра
            float currentY = centerY - segmentLengthPixelsY;
            int segmentCountY = 1;
            while (currentY > 0) {
                canvas.drawLine(startX_line - 5f, currentY, startX_line + 5f, currentY, scalePaint);
                String scaleText = String.format("%.0f м", segmentCountY * segmentLengthMetersY);
                canvas.drawText(scaleText, startX_line + 10f, currentY, scalePaint);
                currentY -= segmentLengthPixelsY;
                segmentCountY++;
            }

            // Рисуем сегменты справа от центра
            currentX = centerX + segmentLengthPixelsX;
            segmentCountX = 1;
            while (currentX < getWidth()) {
                canvas.drawLine(currentX, startY_line - 10f, currentX, startY_line + 10f, scalePaint);
                String scaleText = String.format("%.0f м", segmentCountX * segmentLengthMetersX);
                canvas.drawText(scaleText, currentX, startY_line - 20f, scalePaint);
                currentX += segmentLengthPixelsX;
                segmentCountX++;
            }

            canvas.drawLine(0, startY_line, getWidth(), startY_line, scalePaint);
            canvas.drawLine(startX_line, 0, startX_line, getHeight(), scalePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && touchListener != null) {
            touchListener.onMapTouched(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

}

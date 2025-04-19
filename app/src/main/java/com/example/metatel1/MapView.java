package com.example.metatel1;


import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
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
    double totalAzimuth;
    double rad;
    float x2;
    float y2;

    private Paint paintRed, paintBlue, paintGreen;

    public interface OnMapTouchListener {
        void onMapTouched(float x, float y);
    }

    private OnMapTouchListener touchListener;

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

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
            float top  = (vh - dh) / 2f;
            RectF dst = new RectF(left, top, left + dw, top + dh);
            canvas.drawBitmap(mapBitmap, null, dst, null);
        }


        for (PointF[] line : calibrationLines) {
            PointF p1 = line[0], p2 = line[1];
            canvas.drawCircle(p1.x, p1.y, 5f, paintRed);
            canvas.drawCircle(p2.x, p2.y, 5f, paintRed);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintRed);
        }

        for (PointF p : scalePoints) canvas.drawCircle(p.x, p.y, 5f, paintRed);
        if (centerPoint != null) {
            totalAzimuth = azimuth + azimuthFix;
            rad = Math.toRadians(totalAzimuth);
            x2 = (float) (centerPoint.x + radiusPixels * Math.sin(rad));
            y2 = (float) (centerPoint.y - radiusPixels * Math.cos(rad));
            canvas.drawCircle(centerPoint.x, centerPoint.y, radiusPixels, paintRed);
            canvas.drawLine(centerPoint.x, centerPoint.y, x2, y2, paintBlue);
        }

        for (PointF p : firePoints)
            canvas.drawCircle(p.x, p.y, 5f, paintBlue);
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

package kr.co.conwallet;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private float scale = 1f;
    private float lastX, lastY;
    private boolean dragging;

    public ZoomImageView(Context c) {
        super(c);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newScale = Math.max(1f, Math.min(6f, scale * factor));
                factor = newScale / scale; scale = newScale;
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                setImageMatrix(matrix); return true;
            }
        });
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(w,h,oldw,oldh); post(this::fitCenter); }
    @Override public void setImageDrawable(Drawable drawable) { super.setImageDrawable(drawable); post(this::fitCenter); }

    private void fitCenter() {
        Drawable d = getDrawable(); if (d == null || getWidth()==0 || getHeight()==0) return;
        float sx = getWidth() / (float) d.getIntrinsicWidth(); float sy = getHeight() / (float) d.getIntrinsicHeight();
        float s = Math.min(sx, sy); float dx = (getWidth() - d.getIntrinsicWidth()*s)/2f; float dy = (getHeight()-d.getIntrinsicHeight()*s)/2f;
        matrix.reset(); matrix.postScale(s,s); matrix.postTranslate(dx,dy); scale=1f; setImageMatrix(matrix);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: lastX=e.getX(); lastY=e.getY(); dragging=true; break;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress() && scale > 1f) {
                    float dx=e.getX()-lastX, dy=e.getY()-lastY; matrix.postTranslate(dx,dy); setImageMatrix(matrix);
                }
                lastX=e.getX(); lastY=e.getY(); break;
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL: dragging=false; break;
        }
        return true;
    }
}

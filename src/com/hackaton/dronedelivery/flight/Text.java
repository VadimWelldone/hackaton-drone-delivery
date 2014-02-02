package com.hackaton.dronedelivery.flight;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import javax.microedition.khronos.opengles.GL10;

public class Text extends GLSprite {
    private String mText;
    private Paint mPaint;

    private boolean mIsNeedUpdateTexture;

    private Resources mResources;

    public Text(Context context) {
        mResources = context.getResources();
        mPaint = new Paint();
        mPaint.setColor(mResources.getColor(android.R.color.primary_text_dark));
        mPaint.setTextSize(24);
        mPaint.setAntiAlias(true);
        mPaint.setSubpixelText(true);
        mText = new String();
        initSprite();
        mIsNeedUpdateTexture = false;
    }
    private void initSprite() {
        Bitmap bitmap = createBitmapToRender();
        updateTexture(bitmap);
        bitmap.recycle();
    }
    private Bitmap createBitmapToRender() {
        float width = 1;
        float height = 1;

        if (mText.length() > 0) {
            width = mPaint.measureText(mText);
            height = mPaint.getTextSize();
        }
        Bitmap bitmap = Bitmap.createBitmap(Math.round(width), Math.round(height), Bitmap.Config.ARGB_4444);
        bitmap.eraseColor(0x00000000);
        bitmap.setDensity(mResources.getDisplayMetrics().densityDpi);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(mText, 0, height - mPaint.getFontMetrics().bottom, mPaint);
        return bitmap;
    }
    @Override
    public void onDraw(GL10 gl, float x, float y) {
        if (mIsNeedUpdateTexture) {
            Bitmap bitmap = createBitmapToRender();
            updateTexture(bitmap);
            mIsNeedUpdateTexture = false;
        }
        super.onDraw(gl, x, y);
    }
    public void setTextSize(int size) {
        if (mPaint.getTextSize() != size) {
            mPaint.setTextSize(size);
            invalidate();
        }
    }
    public void setTextColor(int color) {
        if (color != mPaint.getColor()) {
            mPaint.setColor(color);
            invalidate();
        }
    }
    public void setText(String value) {
        if (!mText.equals(value)) {
            mText = value;
            invalidate();
        }
    }
    public void setBold(boolean bold) {
        if (bold) {
            mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        } else {
            mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        }
    }
    private void invalidate() {
        mIsNeedUpdateTexture = true;
    }
    public void setTypeface(Typeface tf) {
        mPaint.setTypeface(tf);
        invalidate();
    }
}

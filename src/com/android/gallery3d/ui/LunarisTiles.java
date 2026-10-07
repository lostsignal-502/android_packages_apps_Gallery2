/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;

public final class LunarisTiles {

    private static final int SHAPE_SIZE = 128;

    private LunarisTiles() {
    }

    public static float photoCorner(Context context) {
        return context.getResources().getFraction(R.fraction.lunaris_photo_corner, 1, 1);
    }

    public static float albumCorner(Context context) {
        return context.getResources().getFraction(R.fraction.lunaris_album_corner, 1, 1);
    }

    public static ThreadPool.Job<Bitmap> rounded(ThreadPool.Job<Bitmap> job, float corner) {
        return jc -> {
            Bitmap bitmap = job.run(jc);
            if (bitmap == null || jc.isCancelled()) return bitmap;
            final Bitmap rounded = round(bitmap, corner);
            if (rounded != bitmap) GalleryBitmapPool.getInstance().put(bitmap);
            return rounded;
        };
    }

    public static Bitmap round(Bitmap source, float corner) {
        if (corner <= 0f || source.getConfig() == Bitmap.Config.HARDWARE) return source;
        final int w = source.getWidth();
        final int h = source.getHeight();
        final Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        final float r = Math.min(w, h) * corner;
        new Canvas(out).drawRoundRect(0, 0, w, h, r, r, paint);
        return out;
    }

    public static BitmapTexture shape(int color, float corner) {
        final Bitmap out = Bitmap.createBitmap(SHAPE_SIZE, SHAPE_SIZE, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        final float r = SHAPE_SIZE * corner;
        new Canvas(out).drawRoundRect(0, 0, SHAPE_SIZE, SHAPE_SIZE, r, r, paint);
        return new BitmapTexture(out);
    }

    public static BitmapTexture checkBadge(Context context, boolean checked) {
        final int size = context.getResources().getDimensionPixelSize(
                R.dimen.lunaris_check_badge_size);
        final Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(out);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float c = size / 2f;
        final float ring = size * 0.08f;
        if (checked) {
            paint.setColor(Color.WHITE);
            canvas.drawCircle(c, c, c, paint);
            paint.setColor(context.getColor(R.color.lunaris_primary));
            canvas.drawCircle(c, c, c - ring, paint);
            final Path check = new Path();
            check.moveTo(size * 0.29f, size * 0.52f);
            check.lineTo(size * 0.44f, size * 0.66f);
            check.lineTo(size * 0.72f, size * 0.37f);
            paint.setColor(context.getColor(R.color.lunaris_on_primary));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.1f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(check, paint);
        } else {
            paint.setColor(0x33000000);
            canvas.drawCircle(c, c, c - ring, paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ring);
            canvas.drawCircle(c, c, c - ring / 2f, paint);
        }
        return new BitmapTexture(out);
    }
}

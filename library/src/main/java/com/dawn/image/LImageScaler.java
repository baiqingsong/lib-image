package com.dawn.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片缩放工具（纯Java实现，不依赖FFmpeg）。
 *
 * <p>特点：
 * <ul>
 *     <li>通过 inSampleSize 先下采样解码，避免 OOM。</li>
 *     <li>再用 Canvas + FILTER_BITMAP_FLAG 做一次高质量缩放到目标尺寸。</li>
 *     <li>按输出后缀选择格式；PNG 像素无损，JPEG 用 quality=100 尽量保真。</li>
 *     <li>若输出是 JPEG，尽量复制 EXIF，并把方向设为 NORMAL。</li>
 * </ul>
 */
@SuppressWarnings("unused")
public final class LImageScaler {

    private LImageScaler() {
    }

    /**
     * 将图片缩放到指定尺寸并写入文件。
     *
     * @param inputPath    原图路径
     * @param outputPath   输出路径
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     */
    public static void scaleToFile(@NonNull String inputPath,
                                   @NonNull String outputPath,
                                   int targetWidth,
                                   int targetHeight) throws IOException {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("targetWidth/targetHeight must be > 0");
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputPath, bounds);
        int srcW = bounds.outWidth;
        int srcH = bounds.outHeight;
        if (srcW <= 0 || srcH <= 0) {
            throw new IOException("Failed to decode image bounds: " + inputPath);
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = calculateInSampleSize(srcW, srcH, targetWidth, targetHeight);
        Bitmap decoded = BitmapFactory.decodeFile(inputPath, opts);
        if (decoded == null) {
            throw new IOException("BitmapFactory.decodeFile returned null: " + inputPath);
        }

        Bitmap scaled = null;
        try {
            if (decoded.getWidth() == targetWidth && decoded.getHeight() == targetHeight) {
                scaled = decoded;
                decoded = null;
            } else {
                scaled = createScaledBitmapHighQuality(decoded, targetWidth, targetHeight);
            }

            Bitmap.CompressFormat format = LImageFormat.chooseOutputFormat(outputPath, inputPath);
            writeBitmap(scaled, outputPath, format, 100);

            if (format == Bitmap.CompressFormat.JPEG) {
                copyExifBestEffort(inputPath, outputPath);
            }
        } finally {
            if (decoded != null && !decoded.isRecycled()) decoded.recycle();
            if (scaled != null && !scaled.isRecycled()) scaled.recycle();
        }
    }

    /**
     * 高质量缩放Bitmap到指定尺寸（内存中操作）。
     *
     * @param src      源Bitmap
     * @param dstW     目标宽度
     * @param dstH     目标高度
     * @return 缩放后的Bitmap
     */
    public static Bitmap scaleHighQuality(@NonNull Bitmap src, int dstW, int dstH) {
        return createScaledBitmapHighQuality(src, dstW, dstH);
    }

    private static Bitmap createScaledBitmapHighQuality(@NonNull Bitmap src, int dstW, int dstH) {
        Bitmap dst = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(src, null, new Rect(0, 0, dstW, dstH), p);
        return dst;
    }

    private static void writeBitmap(@NonNull Bitmap bitmap,
                                    @NonNull String outputPath,
                                    @NonNull Bitmap.CompressFormat format,
                                    int quality) throws IOException {
        File outFile = new File(outputPath);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (outFile.exists()) {
            outFile.delete();
        }

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            boolean ok = bitmap.compress(format, quality, fos);
            fos.flush();
            if (!ok) {
                throw new IOException("Bitmap.compress returned false");
            }
        }
    }

    private static int calculateInSampleSize(int srcW, int srcH, int reqW, int reqH) {
        int inSampleSize = 1;
        if (srcH > reqH || srcW > reqW) {
            final int halfHeight = srcH / 2;
            final int halfWidth = srcW / 2;
            while ((halfHeight / inSampleSize) >= reqH && (halfWidth / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static void copyExifBestEffort(@NonNull String inputPath, @NonNull String outputPath) {
        try {
            ExifInterface in = new ExifInterface(inputPath);
            ExifInterface out = new ExifInterface(outputPath);

            String[] tags = new String[]{
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP
            };

            for (String tag : tags) {
                String val = in.getAttribute(tag);
                if (val != null) out.setAttribute(tag, val);
            }

            out.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            out.saveAttributes();
        } catch (Exception ignore) {
        }
    }
}

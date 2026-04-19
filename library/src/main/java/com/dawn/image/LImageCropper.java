package com.dawn.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片裁剪工具（纯Java实现，不依赖FFmpeg）。
 *
 * <p>使用 BitmapRegionDecoder 进行内存高效的区域解码裁剪。</p>
 *
 * <p>说明：
 * <ul>
 *     <li>PNG：裁剪后用 PNG 写回，像素无损（但会重新编码，文件体积可能变化）。</li>
 *     <li>JPEG：用区域解码 + JPEG(quality=100) 兜底，尽量减小损失，并尽量保留 EXIF。</li>
 *     <li>WebP：统一走 Bitmap 重编码。</li>
 * </ul>
 */
@SuppressWarnings("unused")
public final class LImageCropper {

    private LImageCropper() {
    }

    /**
     * 按"边距"裁剪并写入文件。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param top        上边距（像素）
     * @param bottom     下边距（像素）
     * @param left       左边距（像素）
     * @param right      右边距（像素）
     */
    public static void cropToFile(@NonNull String inputPath,
                                  @NonNull String outputPath,
                                  int top,
                                  int bottom,
                                  int left,
                                  int right) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputPath, bounds);

        int srcW = bounds.outWidth;
        int srcH = bounds.outHeight;
        if (srcW <= 0 || srcH <= 0) {
            throw new IOException("Failed to decode image bounds: " + inputPath);
        }

        Rect cropRect = new Rect(
                clamp(left, 0, srcW),
                clamp(top, 0, srcH),
                clamp(srcW - right, 0, srcW),
                clamp(srcH - bottom, 0, srcH)
        );
        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            throw new IOException("Invalid crop rect: " + cropRect);
        }

        int exifOrientation = readExifOrientationSafe(inputPath);

        Bitmap cropped = null;
        try {
            cropped = decodeRegion(inputPath, cropRect);
            cropped = applyExifOrientationIfNeeded(cropped, exifOrientation);

            Bitmap.CompressFormat format = LImageFormat.chooseOutputFormat(outputPath, inputPath);
            writeBitmap(cropped, outputPath, format, 100);

            if (format == Bitmap.CompressFormat.JPEG) {
                copyExif(inputPath, outputPath, true);
            }
        } finally {
            if (cropped != null && !cropped.isRecycled()) {
                cropped.recycle();
            }
        }
    }

    /**
     * 按矩形区域裁剪并写入文件。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param x          起始X坐标
     * @param y          起始Y坐标
     * @param width      裁剪宽度
     * @param height     裁剪高度
     */
    public static void cropRectToFile(@NonNull String inputPath,
                                      @NonNull String outputPath,
                                      int x, int y,
                                      int width, int height) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputPath, bounds);

        int srcW = bounds.outWidth;
        int srcH = bounds.outHeight;
        if (srcW <= 0 || srcH <= 0) {
            throw new IOException("Failed to decode image bounds: " + inputPath);
        }

        Rect cropRect = new Rect(
                clamp(x, 0, srcW),
                clamp(y, 0, srcH),
                clamp(x + width, 0, srcW),
                clamp(y + height, 0, srcH)
        );
        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            throw new IOException("Invalid crop rect: " + cropRect);
        }

        int exifOrientation = readExifOrientationSafe(inputPath);

        Bitmap cropped = null;
        try {
            cropped = decodeRegion(inputPath, cropRect);
            cropped = applyExifOrientationIfNeeded(cropped, exifOrientation);

            Bitmap.CompressFormat format = LImageFormat.chooseOutputFormat(outputPath, inputPath);
            writeBitmap(cropped, outputPath, format, 100);

            if (format == Bitmap.CompressFormat.JPEG) {
                copyExif(inputPath, outputPath, true);
            }
        } finally {
            if (cropped != null && !cropped.isRecycled()) {
                cropped.recycle();
            }
        }
    }

    private static Bitmap decodeRegion(@NonNull String inputPath, @NonNull Rect cropRect) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(inputPath))) {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap region = decoder.decodeRegion(cropRect, options);
            decoder.recycle();
            if (region == null) {
                throw new IOException("decodeRegion returned null");
            }
            return region;
        }
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

    private static int readExifOrientationSafe(@NonNull String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignore) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static Bitmap applyExifOrientationIfNeeded(@NonNull Bitmap bitmap, int exifOrientation) {
        Matrix m = new Matrix();
        boolean transform = true;
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                m.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                m.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                m.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                m.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                m.postRotate(90);
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                m.postRotate(270);
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                transform = false;
                break;
        }

        if (!transform) return bitmap;

        Bitmap out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        if (out != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return out;
    }

    private static void copyExif(@NonNull String inputPath, @NonNull String outputPath, boolean resetOrientation) {
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
                    ExifInterface.TAG_ISO_SPEED_RATINGS,
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
                @Nullable String val = in.getAttribute(tag);
                if (val != null) {
                    out.setAttribute(tag, val);
                }
            }

            if (resetOrientation) {
                out.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            }

            out.saveAttributes();
        } catch (Exception ignore) {
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

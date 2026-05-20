package com.dawn.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

/**
 * 图片有效性校验工具类
 * <p>
 * 提供三种校验级别：完整解码校验、极速下采样校验、文件头+最小解码校验。
 * </p>
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class LImageValidator {

    private static final String[] SUPPORTED_MIME_TYPES = {
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp"
    };

    /**
     * 完整校验图片是否有效（可解码）
     *
     * @param filePath 图片文件路径
     * @return true 表示图片有效可解码
     */
    public static boolean isImageValid(@NonNull String filePath) {
        if (TextUtils.isEmpty(filePath)) return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        if (options.outWidth <= 0 || options.outHeight <= 0
                || TextUtils.isEmpty(options.outMimeType)) {
            return false;
        }
        if (!isMimeTypeSupported(options.outMimeType)) return false;
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options);
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap != null) {
                bitmap.recycle();
                return true;
            }
        } catch (OutOfMemoryError | Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * 极速校验：以极小目标尺寸进行下采样解码，速度优先
     *
     * @param filePath 图片文件路径
     * @return true 表示图片有效
     */
    public static boolean isImageValidFast(@NonNull String filePath) {
        if (TextUtils.isEmpty(filePath)) return false;
        final BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || TextUtils.isEmpty(bounds.outMimeType)) {
            return false;
        }
        if (!isMimeTypeSupported(bounds.outMimeType)) return false;
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = calculateSampleSizeToTarget(bounds.outWidth, bounds.outHeight, 64);
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inScaled = false;
        opts.inMutable = false;
        try {
            Bitmap bm = BitmapFactory.decodeFile(filePath, opts);
            if (bm != null) {
                boolean ok = bm.getWidth() > 0 && bm.getHeight() > 0;
                bm.recycle();
                return ok;
            }
        } catch (OutOfMemoryError | Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * 快速轻量校验：文件头签名检查 + 最小解码验证
     *
     * @param filePath 图片文件路径
     * @return true 表示图片有效
     */
    public static boolean isImageValidQuick(String filePath) {
        String detectedType = quickHeaderCheck(filePath);
        if (detectedType == null) return false;
        return minimalDecodeCheck(filePath, detectedType);
    }

    // ==================== 私有辅助方法 ====================

    private static boolean isMimeTypeSupported(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) return false;
        for (String t : SUPPORTED_MIME_TYPES) {
            if (t.equalsIgnoreCase(mimeType)) return true;
        }
        return false;
    }

    private static int calculateSampleSize(BitmapFactory.Options options) {
        final int maxDimension = Math.max(options.outWidth, options.outHeight);
        return maxDimension > 2048 ? maxDimension / 1024 : 1;
    }

    private static int calculateSampleSizeToTarget(int srcW, int srcH, int targetMax) {
        if (srcW <= 0 || srcH <= 0 || targetMax <= 0) return 1;
        int maxDim = Math.max(srcW, srcH);
        int sample = 1;
        while ((maxDim / sample) > targetMax) {
            sample <<= 1;
        }
        return Math.max(sample, 1);
    }

    private static final Pair<String, byte[]>[] IMAGE_HEADERS = new Pair[]{
            new Pair<>("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            new Pair<>("image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
            new Pair<>("image/webp", new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}),
            new Pair<>("image/gif", new byte[]{'G', 'I', 'F', '8'})
    };

    private static String quickHeaderCheck(String filePath) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            for (Pair<String, byte[]> header : IMAGE_HEADERS) {
                byte[] signature = header.second;
                byte[] buffer = new byte[signature.length];
                if (fis.read(buffer) != signature.length) continue;
                boolean match = true;
                for (int i = 0; i < signature.length; i++) {
                    if ("image/webp".equals(header.first) && i >= 4 && i <= 7) continue;
                    if (buffer[i] != signature[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) return header.first;
                fis.getChannel().position(0);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static boolean minimalDecodeCheck(String filePath, String mimeType) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) return false;
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateMaximalSampleSize(options);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap != null) {
                bitmap.recycle();
                return true;
            }
        } catch (Exception | OutOfMemoryError e) {
            // ignore
        }
        return false;
    }

    private static int calculateMaximalSampleSize(BitmapFactory.Options options) {
        return Math.max(options.outWidth, options.outHeight);
    }
}

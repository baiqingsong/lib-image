package com.dawn.image;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

/**
 * 图片格式工具，根据文件后缀选择输出格式。
 */
final class LImageFormat {

    private LImageFormat() {
    }

    /**
     * 根据输出路径后缀选择压缩格式，若后缀不明确则参考输入路径。
     *
     * @param outputPath 输出路径
     * @param inputPath  输入路径
     * @return 压缩格式
     */
    static Bitmap.CompressFormat chooseOutputFormat(@NonNull String outputPath, @NonNull String inputPath) {
        String outLower = outputPath.toLowerCase();
        if (outLower.endsWith(".jpg") || outLower.endsWith(".jpeg")) {
            return Bitmap.CompressFormat.JPEG;
        }
        if (outLower.endsWith(".webp")) {
            return Bitmap.CompressFormat.WEBP;
        }
        if (outLower.endsWith(".png")) {
            return Bitmap.CompressFormat.PNG;
        }

        String inLower = inputPath.toLowerCase();
        if (inLower.endsWith(".jpg") || inLower.endsWith(".jpeg")) {
            return Bitmap.CompressFormat.JPEG;
        }
        if (inLower.endsWith(".webp")) {
            return Bitmap.CompressFormat.WEBP;
        }
        return Bitmap.CompressFormat.PNG;
    }
}

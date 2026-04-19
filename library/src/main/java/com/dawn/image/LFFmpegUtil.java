package com.dawn.image;

import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

/**
 * FFmpeg 图片处理工具类。
 *
 * <p>依赖 mobile-ffmpeg-full 库，提供基于 FFmpeg 的图片裁剪、缩放、压缩等操作。
 * 相比纯 Java 的 Bitmap 操作，FFmpeg 处理速度较慢但效果更好，特别适合高质量图片处理。</p>
 *
 * <p>需要在 build.gradle 中添加依赖：
 * <pre>implementation 'com.arthenica:mobile-ffmpeg-full:4.4'</pre>
 * </p>
 */
@SuppressWarnings("unused")
public final class LFFmpegUtil {

    private static final String TAG = "LFFmpegUtil";

    private LFFmpegUtil() {
    }

    /**
     * 使用 FFmpeg 裁剪图片（按边距裁剪）。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param top        上边距（像素）
     * @param bottom     下边距（像素）
     * @param left       左边距（像素）
     * @param right      右边距（像素）
     * @return 是否裁剪成功
     */
    public static boolean cropImage(String inputPath, String outputPath,
                                    int top, int bottom, int left, int right) {
        String command = String.format("-y -i \"%s\" -vf crop=in_w-%d-%d:in_h-%d-%d:%d:%d \"%s\"",
                inputPath, left, right, top, bottom, left, top, outputPath);
        return executeCommand(command);
    }

    /**
     * 使用 FFmpeg 缩放图片到指定尺寸。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param width      目标宽度
     * @param height     目标高度
     * @return 是否缩放成功
     */
    public static boolean scaleImage(String inputPath, String outputPath, int width, int height) {
        String command = String.format("-y -i \"%s\" -vf scale=%d:%d -q:v 2 \"%s\"",
                inputPath, width, height, outputPath);
        return executeCommand(command);
    }

    /**
     * 使用 FFmpeg 压缩图片（指定质量）。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param quality    压缩质量（1-31，值越小质量越高）
     * @return 是否压缩成功
     */
    public static boolean compressImage(String inputPath, String outputPath, int quality) {
        String command = String.format("-y -i \"%s\" -q:v %d \"%s\"",
                inputPath, quality, outputPath);
        return executeCommand(command);
    }

    /**
     * 使用 FFmpeg 合并多张图片为网格布局。
     *
     * <p>示例：2x2 网格合并4张图片</p>
     *
     * @param inputPaths 输入图片路径数组
     * @param outputPath 输出路径
     * @param columns    列数
     * @param tileWidth  每个格子的宽度
     * @param tileHeight 每个格子的高度
     * @return 是否合并成功
     */
    public static boolean mergeGrid(String[] inputPaths, String outputPath,
                                    int columns, int tileWidth, int tileHeight) {
        if (inputPaths == null || inputPaths.length == 0) return false;

        int rows = (int) Math.ceil((double) inputPaths.length / columns);
        int canvasWidth = columns * tileWidth;
        int canvasHeight = rows * tileHeight;

        StringBuilder command = new StringBuilder();
        command.append("-y");

        // 添加所有输入
        for (String path : inputPaths) {
            command.append(" -i \"").append(path).append("\"");
        }

        // 构建 filter_complex
        command.append(" -filter_complex \"");

        // 缩放每个输入
        for (int i = 0; i < inputPaths.length; i++) {
            command.append("[").append(i).append(":v]scale=")
                    .append(tileWidth).append(":").append(tileHeight)
                    .append("[s").append(i).append("];");
        }

        // 创建画布
        command.append("color=c=white:s=").append(canvasWidth).append("x").append(canvasHeight)
                .append("[bg];");

        // 逐个叠加
        String lastOutput = "bg";
        for (int i = 0; i < inputPaths.length; i++) {
            int col = i % columns;
            int row = i / columns;
            int x = col * tileWidth;
            int y = row * tileHeight;
            String currentOutput = (i == inputPaths.length - 1) ? "out" : "tmp" + i;
            command.append("[").append(lastOutput).append("][s").append(i).append("]")
                    .append("overlay=").append(x).append(":").append(y)
                    .append("[").append(currentOutput).append("];");
            lastOutput = currentOutput;
        }

        // 移除最后的分号
        command.setLength(command.length() - 1);

        command.append("\" -map \"[out]\" -frames:v 1 \"").append(outputPath).append("\"");

        return executeCommand(command.toString());
    }

    /**
     * 使用 FFmpeg 将图片叠加到背景图上。
     *
     * @param backgroundPath 背景图路径
     * @param overlayPath    叠加图路径
     * @param outputPath     输出路径
     * @param x              叠加位置X坐标
     * @param y              叠加位置Y坐标
     * @return 是否叠加成功
     */
    public static boolean overlay(String backgroundPath, String overlayPath, String outputPath,
                                  int x, int y) {
        String command = String.format("-y -i \"%s\" -i \"%s\" -filter_complex \"overlay=%d:%d\" \"%s\"",
                backgroundPath, overlayPath, x, y, outputPath);
        return executeCommand(command);
    }

    /**
     * 使用 FFmpeg 添加白色边距（适合打印）。
     *
     * @param inputPath  原图路径
     * @param outputPath 输出路径
     * @param left       左边距
     * @param top        上边距
     * @param right      右边距
     * @param bottom     下边距
     * @return 是否成功
     */
    public static boolean addPadding(String inputPath, String outputPath,
                                     int left, int top, int right, int bottom) {
        String command = String.format(
                "-y -i \"%s\" -vf \"pad=iw+%d+%d:ih+%d+%d:%d:%d:white\" \"%s\"",
                inputPath, left, right, top, bottom, left, top, outputPath);
        return executeCommand(command);
    }

    /**
     * 执行自定义 FFmpeg 命令。
     *
     * @param command FFmpeg 命令（不含 "ffmpeg" 前缀）
     * @return 是否执行成功
     */
    public static boolean executeCommand(String command) {
        try {
            Log.d(TAG, "FFmpeg command: " + command);
            int rc = FFmpeg.execute(command);
            if (rc == Config.RETURN_CODE_SUCCESS) {
                return true;
            } else if (rc == Config.RETURN_CODE_CANCEL) {
                Log.w(TAG, "FFmpeg command cancelled");
            } else {
                Log.e(TAG, "FFmpeg command failed with rc=" + rc);
            }
        } catch (Exception e) {
            Log.e(TAG, "FFmpeg command error", e);
        }
        return false;
    }

    /**
     * 取消正在执行的 FFmpeg 命令。
     */
    public static void cancel() {
        FFmpeg.cancel();
    }
}

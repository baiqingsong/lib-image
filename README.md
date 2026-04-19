# lib-image

Android 图片处理工具库

## 引用

Step 1. Add the JitPack repository to your build file

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies {
    implementation 'com.github.baiqingsong:lib-image:Tag'
}
```

## 类说明

`com.dawn.image.LImageUtil` 图片工具类

### 缩放

| 方法 | 说明 |
|------|------|
| `scaleBitmap(Bitmap, float)` | 按比例缩放图片 |
| `scaleBitmap(Bitmap, int, int)` | 按指定宽高缩放图片 |

### 旋转 / 翻转

| 方法 | 说明 |
|------|------|
| `rotateBitmap(Bitmap, float)` | 旋转图片 |
| `flipHorizontal(Bitmap)` | 水平翻转图片 |
| `flipVertical(Bitmap)` | 垂直翻转图片 |

### 裁剪

| 方法 | 说明 |
|------|------|
| `toCircle(Bitmap)` | 裁剪为圆形 |
| `toRoundCorner(Bitmap, float)` | 裁剪为圆角 |
| `cropping(Bitmap, int, int)` | 按宽高比居中裁剪 |
| `cropping(Bitmap, int, int, int)` | 按宽高比居中裁剪（限制最大宽度） |
| `cropFace(Bitmap, int, int, int, int)` | 裁剪人脸区域 |

### 转换

| 方法 | 说明 |
|------|------|
| `bitmapToBytes(Bitmap, CompressFormat, int)` | Bitmap 转 byte 数组 |
| `bytesToBitmap(byte[])` | byte 数组转 Bitmap |
| `bitmapToBase64(Bitmap)` | Bitmap 转 Base64（JPEG，质量100） |
| `bitmapToBase64(Bitmap, int)` | Bitmap 转 Base64（JPEG，指定质量） |
| `bitmapToBase64(Bitmap, CompressFormat, int)` | Bitmap 转 Base64（指定格式和质量） |
| `base64ToBitmap(String)` | Base64 转 Bitmap |
| `bitmapToByteArray(Bitmap, int)` | Bitmap 转 byte 数组（JPEG） |
| `byteArrayToBitmap(byte[])` | byte 数组转 Bitmap |

### 加载 / 保存

| 方法 | 说明 |
|------|------|
| `loadBitmap(String)` | 从文件加载 Bitmap |
| `loadBitmap(String, int, int)` | 从文件加载 Bitmap（采样，避免 OOM） |
| `decodeSampledBitmap(String, int, int)` | 从文件按目标尺寸采样加载 |
| `decodeSampledBitmapFromResource(Context, int, int, int)` | 从资源按目标尺寸采样加载 |
| `saveBitmap(Bitmap, String)` | 保存到文件（默认质量90） |
| `saveBitmap(Bitmap, String, int)` | 保存到文件（指定质量） |
| `saveBitmap(Bitmap, File, CompressFormat, int)` | 保存到文件（指定格式和质量） |

### 合并

| 方法 | 说明 |
|------|------|
| `mergeHorizontal(Bitmap, Bitmap)` | 水平拼接两张图片 |
| `mergeVertical(Bitmap, Bitmap)` | 垂直拼接两张图片 |

### DPI PNG 保存（适合打印）

| 方法 | 说明 |
|------|------|
| `savePng300Dpi(Bitmap, File)` | 保存 PNG 并写入 300dpi 元数据 |
| `savePng300Dpi(Bitmap, String)` | 保存 PNG 并写入 300dpi 元数据 |
| `savePngWithDpi(Bitmap, File, int)` | 保存 PNG 并写入指定 DPI 元数据 |

### 其他

| 方法 | 说明 |
|------|------|
| `getImageSize(String)` | 获取图片宽高（不加载到内存） |
| `recycleBitmap(Bitmap)` | 安全回收 Bitmap |

---

`com.dawn.image.LImageCropper` 图片裁剪工具（纯Java，不依赖FFmpeg）

使用 BitmapRegionDecoder 进行内存高效的区域解码裁剪，自动处理 EXIF 旋转，支持 JPEG/PNG/WebP。

| 方法 | 说明 |
|------|------|
| `cropToFile(String, String, int, int, int, int)` | 按边距裁剪（top, bottom, left, right） |
| `cropRectToFile(String, String, int, int, int, int)` | 按矩形区域裁剪（x, y, width, height） |

---

`com.dawn.image.LImageScaler` 图片缩放工具（纯Java，不依赖FFmpeg）

通过 inSampleSize 预采样 + Canvas 高质量缩放，自动保留 EXIF 信息。

| 方法 | 说明 |
|------|------|
| `scaleToFile(String, String, int, int)` | 缩放图片到指定尺寸并写入文件 |
| `scaleHighQuality(Bitmap, int, int)` | 内存中高质量缩放 Bitmap |

---

`com.dawn.image.LFFmpegUtil` FFmpeg 图片处理工具

依赖 `mobile-ffmpeg-full`，处理速度较慢但效果更好，适合高质量图片处理场景。

| 方法 | 说明 |
|------|------|
| `cropImage(String, String, int, int, int, int)` | FFmpeg 裁剪（按边距） |
| `scaleImage(String, String, int, int)` | FFmpeg 缩放到指定尺寸 |
| `compressImage(String, String, int)` | FFmpeg 压缩（指定质量） |
| `mergeGrid(String[], String, int, int, int)` | FFmpeg 网格合并多张图片 |
| `overlay(String, String, String, int, int)` | FFmpeg 图片叠加到背景 |
| `addPadding(String, String, int, int, int, int)` | FFmpeg 添加白色边距 |
| `executeCommand(String)` | 执行自定义 FFmpeg 命令 |
| `cancel()` | 取消正在执行的命令 |

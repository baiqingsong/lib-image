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

### 其他

| 方法 | 说明 |
|------|------|
| `getImageSize(String)` | 获取图片宽高（不加载到内存） |
| `recycleBitmap(Bitmap)` | 安全回收 Bitmap |

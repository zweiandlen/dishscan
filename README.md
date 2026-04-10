# 小碗菜识别

纯离线的 Android 菜品识别应用，基于 YOLOv8n + EfficientNet-B0 双模型进行目标检测与特征提取。

## 功能特性

- 实时摄像头菜品检测与识别
- 无需网络，纯离线运行
- 支持添加新菜品（拍照提取特征向量存入数据库）
- 菜品营养信息管理

## 项目架构

```
app/src/main/java/com/dishrecognition/
├── data/ml/              # ML模型 (ObjectDetectionModel, FeatureExtractorModel)
├── data/local/           # Room数据库 (DishEntity, NutritionEntity)
├── domain/usecase/       # 业务逻辑 (RecognitionUseCase, AddDishUseCase)
└── ui/                   # Jetpack Compose界面
```

## 模型说明

| 模型 | 文件 | 用途 |
|------|------|------|
| YOLOv8n | `assets/models/yolov8n.tflite` | 目标检测 |
| EfficientNet-B0 | `assets/models/efficientnetb0_feature.tflite` | 特征提取 |

### 工作流程

```
摄像头帧 → YOLOv8n检测(菜品+餐具) → 裁剪菜品区域 → EfficientNet-B0提取特征 → 向量匹配
```

### 目标检测标签

`dish`, `small_bowl`, `medium_bowl`, `large_bowl`, `small_plate`, `medium_plate`, `large_plate`, `long_plate`

### 识别参数

- 特征向量维度: 1280 (FloatArray)
- 向量匹配阈值: 0.75 (余弦相似度)

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# 环境要求
export ANDROID_HOME=/opt/android-sdk
java 17+
```

## 添加新菜品

无需重新训练模型！通过拍照提取特征向量存入数据库即可识别。

## 技术栈

- Kotlin + Jetpack Compose
- TensorFlow Lite
- Room Database
- Hilt 依赖注入
- CameraX

# Android 小碗菜识别项目

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# 环境要求
export ANDROID_HOME=/opt/android-sdk
java 17+
```

## 项目架构

```
app/src/main/java/com/dishrecognition/
├── data/ml/              # ML模型 (ObjectDetectionModel, FeatureExtractorModel)
├── data/local/           # Room数据库 (DishEntity, NutritionEntity)
├── domain/usecase/       # 业务逻辑 (RecognitionUseCase, AddDishUseCase)
└── ui/                   # Jetpack Compose界面
```

## 关键模型

| 模型 | 文件 | 用途 |
|------|------|------|
| YOLOv8n | `assets/models/yolov8n.tflite` | 目标检测 |
| EfficientNet-B0 | `assets/models/efficientnetb0_feature.tflite` | 特征提取 |

**模型来源**: 见 `.monkeycode/docs/model-guide.md`

## 工作流程

```
摄像头帧 → YOLOv8n检测(菜品+餐具) → 裁剪菜品区域 → EfficientNet-B0提取特征 → 向量匹配
```

## 添加新菜品

无需重新训练模型！通过拍照提取特征向量存入数据库即可识别。

## 重要约束

- 纯离线运行，无需网络权限
- 目标检测标签: dish, small_bowl, medium_bowl, large_bowl, small_plate, medium_plate, large_plate, long_plate
- 特征向量维度: 1280 (FloatArray)
- 向量匹配阈值: 0.75 (余弦相似度)

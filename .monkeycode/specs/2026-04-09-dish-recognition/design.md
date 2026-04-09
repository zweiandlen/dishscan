# 小碗菜识别系统技术设计

需求名称：dish-recognition
更新日期：2026-04-09

## 1. 概述

本项目是一款 Android 端小碗菜识别应用，主要功能包括：
- 实时识别餐桌上的菜品与餐具
- 通过多角度拍照添加新菜品到个人菜谱库
- 基于餐具类型估算菜品重量并计算营养成分

### 技术约束

| 约束项 | 选择 |
|--------|------|
| 模型部署 | 本地离线（TensorFlow Lite） |
| 营养数据库 | 自建本地 SQLite |
| 重量估算 | 基于餐具类型映射表 |
| 网络要求 | 无需网络，纯离线运行 |

---

## 2. 系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Android App                          │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                                 │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  CameraScreen│ │  DishListScreen│ │  NutritionScreen│ │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer                                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ RecognitionUseCase│ │ AddDishUseCase │ │ NutritionUseCase│ │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ TFLiteModelRepo│ │ DishRepository │ │ NutritionRepository│ │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
├─────────────────────────────────────────────────────────────┤
│  ML/CV Layer                                                 │
│  ┌─────────────────────┐ ┌─────────────────────┐           │
│  │ ObjectDetectionModel│ │ DishClassifierModel │           │
│  │   (YOLOv5s-TFLite)  │ │  (MobileNetV3-TFLite)│           │
│  └─────────────────────┘ └─────────────────────┘           │
├─────────────────────────────────────────────────────────────┤
│  Local Database (Room)                                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │   DishEntity │ │NutritionEntity│ │ TablewareMapping │ │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系

```mermaid
graph TD
    A[UI Layer] --> B[Domain Layer]
    B --> C[Data Layer]
    C --> D[(Local DB)]
    C --> E[ML Models]
    E --> F[TFLite Interpreter]
```

---

## 3. 组件与接口

### 3.1 核心组件

| 组件 | 职责 | 输入 | 输出 |
|------|------|------|------|
| `ObjectDetectionModel` | 实时检测餐具和菜品 | Camera Frame | DetectionResult[List[BoundingBox]] |
| `DishClassifierModel` | 菜品分类/识别 | Cropped Image | ClassificationResult |
| `NutritionRepository` | 营养成分查询 | dish_id | NutritionInfo |
| `WeightEstimator` | 基于餐具估算重量 | TablewareType | EstimatedWeight |
| `DishRepository` | 菜品CRUD | DishEntity | Long(dishId) |

### 3.2 主要接口

```kotlin
interface RecognitionUseCase {
    suspend fun recognizeDish(frame: ByteArray): Flow<RecognitionResult>
}

interface AddDishUseCase {
    suspend fun addNewDish(images: List<ByteArray>, name: String): Long
}

interface NutritionUseCase {
    suspend fun getNutritionInfo(dishId: Long, tablewareType: TablewareType): NutritionInfo
}
```

### 3.3 屏幕导航

```
MainScreen (BottomNavigation)
├── CameraScreen (实时识别)
│   └── RecognitionOverlay (识别结果叠加层)
├── DishListScreen (菜品列表)
│   └── AddDishScreen (添加新菜品 - 多角度拍照)
└── NutritionScreen (营养统计)
```

---

## 4. 数据模型

### 4.1 数据库 Schema (Room)

```kotlin
@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val featureVector: FloatArray,  // 1280维特征向量
    val imagePaths: List<String>,   // 多角度图片路径
    val createdAt: Long,
    val updatedAt: Long
) {
    override equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DishEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "nutrition")
data class NutritionEntity(
    @PrimaryKey val dishId: Long,
    val calories: Float,           // 热量 (kcal/100g)
    val fat: Float,                 // 脂肪 (g/100g)
    val protein: Float,            // 蛋白质 (g/100g)
    val carbohydrate: Float,       // 碳水化合物 (g/100g)
    val vitamin: Float,            // 维生素 (mg/100g)
    val sodium: Float              // 钠 (mg/100g)
)

@Entity(tableName = "tableware_mapping")
data class TablewareMappingEntity(
    @PrimaryKey val tablewareType: String,  // "small_bowl", "plate", etc.
    val defaultWeight: Float  // 默认重量 (g)
)
```

### 4.2 营养成分单位

| 成分 | 单位 | 说明 |
|------|------|------|
| 热量 | kcal | 千卡 |
| 脂肪 | g | 克 |
| 蛋白质 | g | 克 |
| 碳水化合物 | g | 克 |
| 维生素 | mg | 毫克 |
| 钠 | mg | 毫克 |

### 4.3 餐具类型映射

| 餐具类型 | 估算重量 | 适用场景 |
|----------|----------|----------|
| 小碗 | 150g | 汤类、小份菜 |
| 中碗 | 250g | 主食、米饭 |
| 大碗 | 400g | 面条、炖菜 |
| 小盘 | 100g | 凉菜、小食 |
| 中盘 | 200g | 炒菜 |
| 大盘 | 350g | 整鱼、排骨 |
| 长盘 | 300g | 饺子、包子 |

---

## 5. ML 模型设计

### 5.1 目标检测模型

| 项目 | 方案 |
|------|------|
| 模型 | YOLOv5s (converted to TFLite) |
| 输入 | 640x640 RGB Image |
| 输出 | BoundingBox + Class + Confidence |
| 类别 | small_bowl, medium_bowl, large_bowl, small_plate, medium_plate, large_plate, long_plate |
| 推理时间 | < 50ms (Snapdragon 865) |

### 5.2 特征向量提取模型

| 项目 | 方案 |
|------|------|
| 基础模型 | MobileNetV3-Large (预训练 ImageNet) |
| 输入 | 224x224 RGB Image |
| 输出 | 1280维特征向量 (1280-dim Float32) |
| 提取层 | GlobalAveragePooling 后的特征层 |
| 用途 | 用户自定义菜品的特征存储与相似度匹配 |

### 5.3 向量检索机制

| 项目 | 方案 |
|------|------|
| 相似度度量 | 余弦相似度 (Cosine Similarity) |
| 匹配阈值 | 0.75 (可调整) |
| 检索方式 | 暴力遍历 + 预过滤 |
| 向量存储 | Room BLOB 字段存储 FloatArray |

### 5.4 模型文件结构

```
assets/
├── models/
│   ├── yolov5s.tflite                # 目标检测模型 (~30MB)
│   └── mobilenetv3_feature.tflite   # 特征提取模型 (~20MB)
├── labels/
│   └── detection_labels.txt          # 检测标签
```

---

## 6. 核心流程

### 6.1 实时识别流程

```mermaid
sequenceDiagram
    participant Camera
    participant Detector
    participant FeatureExtractor
    participant VectorDB
    participant Estimator
    participant UI

    Camera->>Detector: CameraX Frame
    Detector->>Detector: YOLOv5s Inference
    Detector-->>UI: DetectionResult [餐具位置, 菜品区域]

    UI->>FeatureExtractor: 裁剪菜品区域图片
    FeatureExtractor->>FeatureExtractor: MobileNetV3 特征提取
    FeatureExtractor-->>UI: 1280维特征向量

    UI->>VectorDB: 查询相似菜品
    VectorDB-->>UI: 匹配结果 (dishId, similarity)

    UI->>Estimator: 餐具类型
    Estimator-->>UI: 估算重量

    UI->>UI: 叠加显示 + 营养计算
```

### 6.2 添加新菜品流程

```mermaid
flowchart TD
    A[多角度拍摄 3-5张] --> B[选择最佳图片]
    B --> C[图像预处理 224x224]
    C --> D[特征提取 1280维向量]
    D --> E[输入菜品名称]
    E --> F[输入营养成分 / 每100g]
    F --> G[保存到数据库<br/>向量 + 营养 + 图片路径]
```

### 6.3 向量匹配算法

```kotlin
fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
    val dotProduct = vecA.zip(vecB).sumOf { (a, b) -> (a * b).toDouble() }
    val normA = sqrt(vecA.map { it * it }.sumOf { it.toDouble() })
    val normB = sqrt(vecB.map { it * it }.sumOf { it.toDouble() })
    return (dotProduct / (normA * normB)).toFloat()
}

// 匹配逻辑
val results = dishVectors.map { dish ->
    dish to cosineSimilarity(inputVector, dish.featureVector)
}.filter { (_, similarity) -> similarity >= MATCHING_THRESHOLD }
    .sortedByDescending { (_, similarity) -> similarity }
```

### 6.4 营养计算流程

```
营养估算 = 菜品单位营养(每100g) × 估算重量

示例：
- 宫保鸡丁 (每100g): 热量 118kcal, 脂肪 7g, 蛋白质 11g
- 餐具类型: 中碗 (250g)
- 估算结果: 热量 295kcal, 脂肪 17.5g, 蛋白质 27.5g
```

---

## 7. 正确性属性

### 7.1 功能正确性

| 功能 | 预期行为 | 验证方式 |
|------|----------|----------|
| 餐具检测 | 准确识别8种餐具类型 | 公开数据集测试 |
| 菜品识别 | Top-1 准确率 > 85% | 1000张测试集 |
| 营养计算 | 误差 < 10% | 对比标准数据 |
| 多角度添加 | 成功保存3-5张图片 | 功能测试 |

### 7.2 性能指标

| 指标 | 目标值 |
|------|--------|
| 检测帧率 | ≥ 15 FPS |
| 冷启动时间 | < 3s |
| 内存占用 | < 500MB |
| 模型文件大小 | < 100MB |

### 7.3 离线可用性

- 所有功能在飞行模式下正常工作
- 无需任何网络权限
- 用户数据仅存储在本地

---

## 8. 错误处理

### 8.1 异常分类

| 异常类型 | 处理策略 |
|----------|----------|
| 模型加载失败 | 显示错误页面，提示重新安装 |
| 相机权限缺失 | 引导用户授权 |
| 图片保存失败 | Toast提示，重试机制 |
| 数据库损坏 | 自动重建，提示用户 |
| 检测置信度过低 | 显示"未识别"状态 |

### 8.2 用户反馈

- 识别结果可手动纠正
- 支持用户反馈修正
- 营养数据可手动编辑

---

## 9. 技术栈汇总

| 层级 | 技术选型 |
|------|----------|
| 语言 | Kotlin 1.9+ |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM + Clean Architecture |
| DI | Hilt |
| 相机 | CameraX |
| ML | TensorFlow Lite 2.14 |
| 数据库 | Room 2.6 |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose |
| 最小SDK | 26 (Android 8.0) |
| 目标SDK | 34 (Android 14) |

---

## 10. 项目结构

```
app/src/main/
├── java/com/dishrecognition/
│   ├── App.kt                          # Application类
│   ├── di/                             # Hilt模块
│   │   ├── AppModule.kt
│   │   ├── MLModule.kt
│   │   └── DatabaseModule.kt
│   │
│   ├── domain/                         # 领域层
│   │   ├── model/
│   │   │   ├── Dish.kt
│   │   │   ├── NutritionInfo.kt
│   │   │   ├── DetectionResult.kt
│   │   │   └── TablewareType.kt
│   │   ├── repository/
│   │   │   ├── DishRepository.kt
│   │   │   ├── NutritionRepository.kt
│   │   │   └── RecognitionRepository.kt
│   │   └── usecase/
│   │       ├── RecognitionUseCase.kt
│   │       ├── AddDishUseCase.kt
│   │       └── NutritionUseCase.kt
│   │
│   ├── data/                           # 数据层
│   │   ├── local/
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── DishDao.kt
│   │   │   │   ├── NutritionDao.kt
│   │   │   │   └── TablewareMappingDao.kt
│   │   │   └── entity/
│   │   │       ├── DishEntity.kt
│   │   │       ├── NutritionEntity.kt
│   │   │       └── TablewareMappingEntity.kt
│   │   └── ml/
│   │       ├── ObjectDetectionModel.kt
│   │       ├── FeatureExtractorModel.kt
│   │       └── VectorSearchEngine.kt
│   │
│   ├── ui/                             # UI层
│   │   ├── theme/
│   │   ├── navigation/
│   │   ├── camera/
│   │   │   ├── CameraScreen.kt
│   │   │   ├── CameraViewModel.kt
│   │   │   └── RecognitionOverlay.kt
│   │   ├── dishlist/
│   │   │   ├── DishListScreen.kt
│   │   │   └── DishListViewModel.kt
│   │   └── nutrition/
│   │       ├── NutritionScreen.kt
│   │       └── NutritionViewModel.kt
│   │
│   └── util/
│       ├── ImageUtils.kt
│       └── WeightEstimator.kt
│
├── assets/
│   └── models/
│       ├── yolov5s.tflite
│       ├── mobilenetv3_dish.tflite
│       └── nutrition_data.db
│
└── res/
```

---

## 11. 实施计划建议

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| 1 | 项目初始化、架构搭建、相机预览 | P0 |
| 2 | 目标检测模型集成、餐具识别 | P0 |
| 3 | 特征提取模型集成 | P0 |
| 4 | 数据库设计、Room集成、向量存储 | P0 |
| 5 | 向量搜索匹配引擎 | P0 |
| 6 | 多角度拍照 + 表单添加菜品 | P1 |
| 7 | 营养成分计算与展示 | P1 |
| 8 | UI优化、模型调优 | P2 |

---

## 12. 添加新菜品 UI 流程

### 12.1 添加菜品表单

```
┌─────────────────────────────────────────┐
│  添加新菜品                    [完成]    │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐       │
│  │  +  │ │ img │ │ img │ │ img │ ...   │
│  └─────┘ └─────┘ └─────┘ └─────┘       │
│   添加照片                                │
│                                         │
│  菜品名称: [________________]           │
│                                         │
│  ─── 营养成分 (每100g) ───               │
│  热量(kcal): [____]                     │
│  脂肪(g): [____]                        │
│  蛋白质(g): [____]                      │
│  碳水化合物(g): [____]                   │
│  维生素(mg): [____]                      │
│  钠(mg): [____]                          │
│                                         │
└─────────────────────────────────────────┘
```

### 12.2 多角度拍照指引

```
┌─────────────────────────────────────────┐
│           拍摄菜品照片                    │
│                                         │
│     ┌───────────────────────┐          │
│     │                       │          │
│     │      [  菜品  ]       │          │
│     │                       │          │
│     └───────────────────────┘          │
│                                         │
│   建议拍摄 3-5 张不同角度               │
│   • 正面全景                             │
│   • 侧面 45°                            │
│   • 顶部俯视                             │
│                                         │
│   [  拍照  ]      [ 完成 ]              │
│                                         │
└─────────────────────────────────────────┘
```

# 小碗菜识别模型指南

本项目使用两个深度学习模型：
1. **YOLOv8n** - 目标检测（检测菜品和餐具位置）
2. **EfficientNet-B0** - 特征提取（提取菜品特征向量）

---

## 目录

1. [快速下载预训练模型](#1-快速下载预训练模型)
2. [YOLOv8n 模型获取与转换](#2-yolov8n-模型获取与转换)
3. [EfficientNet-B0 模型获取与转换](#3-efficientnetb0-模型获取与转换)
4. [模型文件放置](#4-模型文件放置)
5. [自定义数据集准备](#5-自定义数据集准备)
6. [YOLOv8 自定义训练](#6-yolov8-自定义训练)
7. [模型性能对比](#7-模型性能对比)

---

## 1. 快速下载预训练模型

### YOLOv8n (推荐)

```bash
# 使用 wget 直接下载
wget https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.pt

# 或使用 curl
curl -L https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.pt -o yolov8n.pt
```

### EfficientNet-B0

直接下载 TensorFlow Lite 预训练模型：

```bash
# 下载 EfficientNet-B0 TFLite
wget https://storage.googleapis.com/tfhub-lite-colabs/efficientnet_lite0_feature_vector_2.tflite -O efficientnetb0_feature.tflite

# 备选地址 (需科学上网)
wget https://tfhub.dev/google/imagenet/efficientnet_b0/feature_vector/1?tf-lite=true -O efficientnetb0_feature.tflite
```

---

## 2. YOLOv8n 模型获取与转换

### 2.1 环境准备

```bash
# 创建虚拟环境 (推荐)
python -m venv yolo_env
source yolo_env/bin/activate  # Linux/Mac
# yolo_env\Scripts\activate   # Windows

# 安装 ultralytics
pip install ultralytics
```

### 2.2 下载预训练权重

```bash
# 下载 YOLOv8n 预训练权重 (3.2M 参数)
wget https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.pt

# 验证下载
ls -lh yolov8n.pt  # 应该显示 ~6MB
```

### 2.3 转换为 TFLite

创建转换脚本 `convert_yolo_to_tflite.py`:

```python
#!/usr/bin/env python3
"""
YOLOv8 PT → TFLite 转换脚本
"""

from ultralytics import YOLO
import tensorflow as tf

def convert_yolov8_to_tflite(weights_path='yolov8n.pt', output_path='yolov8n.tflite'):
    """
    将 YOLOv8 权重转换为 TFLite 格式
    
    Args:
        weights_path: YOLOv8 权重文件路径
        output_path: 输出 TFLite 文件路径
    """
    # 加载模型
    model = YOLO(weights_path)
    
    # 导出为 TFLite
    # imgsz=640 表示输入图像大小为 640x640
    # keras=True 会生成 TensorFlow SavedModel，然后可以转换为 TFLite
    model.export(format='tflite', imgsz=640)
    
    print(f"模型已导出为: yolov8n_saved_model/yolov8n.tflite")

def convert_savedmodel_to_tflite():
    """
    将 TensorFlow SavedModel 转换为 TFLite
    """
    # 加载 SavedModel
    converter = tf.lite.TFLiteConverter.from_saved_model('yolov8n_saved_model')
    
    # TFLite 转换选项
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]  # 可选: float16 进一步压缩
    
    # 执行转换
    tflite_model = converter.convert()
    
    # 保存
    with open('yolov8n.tflite', 'wb') as f:
        f.write(tflite_model)
    
    print("TFLite 模型已保存: yolov8n.tflite")

if __name__ == '__main__':
    # 方式1: 直接导出 TFLite (Ultralytics 7.0+)
    convert_yolov8_to_tflite()
    
    # 方式2: 分步转换
    # convert_savedmodel_to_tflite()
```

执行转换：

```bash
python convert_yolo_to_tflite.py

# 输出
# yolov8n_saved_model/yolov8n.tflite
```

### 2.4 验证 TFLite 模型

```python
import tensorflow as tf

# 加载模型
interpreter = tf.lite.Interpreter('yolov8n.tflite')

# 获取输入输出信息
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"输入形状: {input_details[0]['shape']}")
print(f"输出形状: {output_details[0]['shape']}")

# 测试推理
import numpy as np
input_data = np.random.randn(1, 640, 640, 3).astype(np.float32)
interpreter.set_tensor(input_details[0]['index'], input_data)
interpreter.invoke()
output = interpreter.get_tensor(output_details[0]['index'])
print(f"输出形状: {output.shape}")
```

---

## 3. EfficientNet-B0 模型获取与转换

### 3.1 使用 TensorFlow/Keras

```python
#!/usr/bin/env python3
"""
EfficientNet-B0 特征提取器转换脚本
"""

import tensorflow as tf
from tensorflow.keras.applications import EfficientNetB0

def create_feature_extractor():
    """
    创建 EfficientNet-B0 特征提取器
    
    输出: 1280 维特征向量
    """
    model = EfficientNetB0(
        include_top=False,          # 去掉分类头
        weights='imagenet',          # ImageNet 预训练权重
        input_shape=(224, 224, 3),   # 输入图像大小
        pooling='avg'                # 全局平均池化 → 1280 维输出
    )
    
    # 冻结所有层 (只用于特征提取)
    for layer in model.layers:
        layer.trainable = False
    
    return model

def export_to_tflite(model, output_path='efficientnetb0_feature.tflite'):
    """
    导出为 TFLite
    """
    # 创建转换器
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # 优化选项
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # 量化 (可选，可减小模型体积但可能影响精度)
    # converter.target_spec.supported_types = [tf.float16]
    
    # 执行转换
    tflite_model = converter.convert()
    
    # 保存
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"特征提取模型已保存: {output_path}")
    
    # 验证
    model_size = tf.io.gfile.GFile(output_path, 'rb').read()
    print(f"模型大小: {len(model_size) / (1024*1024):.2f} MB")

if __name__ == '__main__':
    print("创建 EfficientNet-B0 特征提取器...")
    model = create_feature_extractor()
    
    print("导出为 TFLite...")
    export_to_tflite(model)
    
    # 打印模型结构
    model.summary()
```

### 3.2 使用 Kaggle/Google Drive

```bash
# 安装 kaggle API
pip install kaggle

# 下载 EfficientNet 预训练模型
kaggle datasets download -dkeras/efficientnetb0

# 解压
unzip efficientnetb0.zip
```

### 3.3 备选方案: MobileNetV3

如果 EfficientNet-B0 转换有问题，可使用 MobileNetV3：

```python
from tensorflow.keras.applications import MobileNetV3Large

model = MobileNetV3Large(
    include_top=False,
    weights='imagenet',
    input_shape=(224, 224, 3),
    pooling='avg'  # 输出 1280 维
)

# 导出 TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
```

---

## 4. 模型文件放置

### 4.1 目录结构

```
app/src/main/assets/
└── models/
    ├── yolov8n.tflite               # 目标检测模型 (~12MB)
    └── efficientnetb0_feature.tflite # 特征提取模型 (~20MB)
```

### 4.2 创建目录并复制文件

```bash
# 创建目录
mkdir -p app/src/main/assets/models

# 复制模型文件
cp yolov8n.tflite app/src/main/assets/models/
cp efficientnetb0_feature.tflite app/src/main/assets/models/

# 验证
ls -lh app/src/main/assets/models/
```

### 4.3 Android 项目配置

确保 `build.gradle.kts` 包含 assets 目录：

```kotlin
android {
    // ...
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}
```

---

## 5. 自定义数据集准备

### 5.1 数据集目录结构

```
dataset/
├── images/
│   ├── train/          # 训练图像
│   │   ├── img001.jpg
│   │   ├── img002.jpg
│   │   └── ...
│   └── val/           # 验证图像
│       ├── img101.jpg
│       └── ...
├── labels/
│   ├── train/         # 训练标签 (YOLO 格式)
│   │   ├── img001.txt
│   │   ├── img002.txt
│   │   └── ...
│   └── val/           # 验证标签
│       ├── img101.txt
│       └── ...
└── dataset.yaml       # 数据集配置文件
```

### 5.2 标注格式 (YOLO TXT)

每行格式: `class_id x_center y_center width height`

```
# img001.txt 内容示例
# class_id: 0=dish, 1=small_bowl, 2=medium_bowl, ...
0 0.5 0.3 0.2 0.15      # 菜品
1 0.2 0.7 0.15 0.2     # 小碗
2 0.8 0.6 0.18 0.25    # 中碗
```

坐标是归一化的 (0-1)：
- `x_center`, `width`: 相对于图像宽度的比例
- `y_center`, `height`: 相对于图像高度的比例

### 5.3 dataset.yaml 配置

```yaml
# dataset.yaml
path: ./dataset           # 数据集根目录
train: images/train       # 训练图像目录
val: images/val           # 验证图像目录

# 类别数量和名称
nc: 8                     # 类别数量
names:
  0: dish                 # 菜品
  1: small_bowl          # 小碗
  2: medium_bowl         # 中碗
  3: large_bowl          # 大碗
  4: small_plate         # 小盘
  5: medium_plate        # 中盘
  6: large_plate         # 大盘
  7: long_plate          # 长盘
```

### 5.4 标注工具推荐

| 工具 | 平台 | 特点 |
|------|------|------|
| [LabelImg](https://github.com/tzutalin/labelImg) | 全平台 | 轻量，支持 YOLO 格式 |
| [CVAT](https://cvat.org) | Web | 在线，支持协作 |
| [Label Studio](https://labelstudio.io) | Web | 功能丰富 |
| [Roboflow](https://roboflow.com) | Web | 专用，支持数据增强 |

使用 LabelImg 标注：

```bash
# 安装
pip install labelImg

# 启动 (选择 YOLO 格式)
labelImg --preset dataset.yaml

# 或指定类别文件
labelImg --classes dish,small_bowl,medium_bowl,large_bowl,small_plate,medium_plate,large_plate,long_plate
```

---

## 6. YOLOv8 自定义训练

### 6.1 完整训练脚本

```python
#!/usr/bin/env python3
"""
YOLOv8 自定义训练脚本
"""

from ultralytics import YOLO
import torch

def train_yolov8(
    data_yaml='dataset.yaml',
    model_size='n',        # n/s/m/l/x
    epochs=100,
    batch_size=16,
    img_size=640,
    device='0'              # GPU 设备，或 'cpu'
):
    """
    训练 YOLOv8 自定义模型
    
    Args:
        data_yaml: 数据集配置文件路径
        model_size: 模型大小 (n=nano, s=small, m=medium, l=large, x=xlarge)
        epochs: 训练轮数
        batch_size: 批次大小 (根据 GPU 内存调整)
        img_size: 输入图像大小
        device: 训练设备
    """
    # 加载预训练模型
    model = YOLO(f'yolov8{model_size}.pt')
    
    # 开始训练
    results = model.train(
        data=data_yaml,           # 数据集配置
        epochs=epochs,             # 训练轮数
        batch=batch_size,         # 批次大小
        imgsz=img_size,           # 图像大小
        device=device,            # 设备
        project='runs/detect',    # 输出目录
        name='dish_detection',    # 实验名称
        exist_ok=True,            # 覆盖已有结果
        pretrained=True,           # 使用预训练权重
        optimizer='AdamW',         # 优化器
        lr0=0.001,                # 初始学习率
        lrf=0.01,                 # 最终学习率 = lr0 * lrf
        momentum=0.937,          # 动量
        weight_decay=0.0005,      # 权重衰减
        save=True,                # 保存模型
        save_period=10,          # 每10轮保存一次
        cache=True,               # 缓存图像加速训练
        workers=8,               # 数据加载线程数
        verbose=True              # 详细输出
    )
    
    print("训练完成!")
    print(f"最佳模型: runs/detect/dish_detection/weights/best.pt")
    
    return results

def export_best_model(model_path='runs/detect/dish_detection/weights/best.pt'):
    """
    导出训练好的模型为 TFLite
    """
    model = YOLO(model_path)
    
    # 导出为 TFLite
    model.export(format='tflite', imgsz=640)
    
    print(f"导出完成: {model_path.replace('.pt', '_saved_model')}/yolov8n.tflite")

if __name__ == '__main__':
    # 检查 GPU
    print(f"PyTorch 版本: {torch.__version__}")
    print(f"CUDA 可用: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"GPU: {torch.cuda.get_device_name(0)}")
    
    # 训练模型
    train_yolov8(
        data_yaml='dataset.yaml',
        model_size='n',      # 使用 nano 模型，推理最快
        epochs=100,         # 根据数据量调整
        batch_size=16,     # 根据 GPU 内存调整
    )
    
    # 导出为 TFLite
    export_best_model()
```

### 6.2 训练命令 (不使用脚本)

```bash
# 基本训练
yolo detect train data=dataset.yaml model=yolov8n.pt epochs=100 imgsz=640

# 指定 GPU
yolo detect train data=dataset.yaml model=yolov8n.pt epochs=100 device=0

# 小数据集，使用数据增强
yolo detect train data=dataset.yaml model=yolov8n.pt epochs=200 \
    model=yolov8n.pt \
    scale=0.5 \           # 小模型
    degrees=15 \          # 旋转增强
    translate=0.1 \       # 平移增强
    flipud=0.5 \          # 上下翻转
    fliplr=0.5 \         # 左右翻转
    mosaic=1.0 \         # 马赛克增强
    mixup=0.1            # mixup 增强
```

### 6.3 训练参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `epochs` | 100 | 训练轮数 |
| `batch` | 16 | 批次大小 |
| `imgsz` | 640 | 输入图像大小 |
| `lr0` | 0.01 | 初始学习率 |
| `lrf` | 0.01 | 最终学习率因子 |
| `momentum` | 0.937 | SGD 动量 |
| `weight_decay` | 0.0005 | 权重衰减 |
| `warmup_epochs` | 3 | 预热轮数 |
| `warmup_momentum` | 0.8 | 预热动量 |
| `warmup_bias_lr` | 0.1 | 预热偏置学习率 |
| `box` | 7.5 | 边框损失权重 |
| `cls` | 0.5 | 分类损失权重 |
| `dfl` | 1.5 | DFL 损失权重 |

### 6.4 训练输出

```
runs/detect/dish_detection/
├── weights/
│   ├── best.pt          # 最佳模型
│   ├── last.pt          # 最后一个模型
├── args.yaml            # 训练参数
├── results.csv          # 训练结果日志
├── results.png          # 训练曲线图
└── val_batch0_pred.jpg  # 验证预测可视化
```

### 6.5 验证模型

```python
from ultralytics import YOLO

# 加载训练好的模型
model = YOLO('runs/detect/dish_detection/weights/best.pt')

# 验证
metrics = model.val(data='dataset.yaml')
print(f"mAP50: {metrics.box.map50:.4f}")
print(f"mAP50-95: {metrics.box.map:.4f}")

# 测试推理
results = model.predict(source='test.jpg', save=True)
```

---

## 7. 模型性能对比

### 7.1 YOLO 系列对比

| 模型 | 参数量 | mAP@50 | 推理时间 | 模型大小 | 推荐场景 |
|------|--------|--------|----------|----------|----------|
| YOLOv8n | 3.2M | 37.3% | ~20ms | 12MB | **移动端首选** |
| YOLOv8s | 11.2M | 44.9% | ~30ms | 40MB | 桌面端 |
| YOLOv8m | 25.9M | 50.2% | ~50ms | 90MB | 服务器 |
| YOLOv5n | 1.9M | 35.5% | ~25ms | 7MB | 极致轻量 |
| YOLOv5s | 7.5M | 42.0% | ~40ms | 28MB | 平衡选择 |
| YOLOv10n | 2.3M | 39.5% | ~15ms | 8MB | 最新最轻 |

### 7.2 特征提取模型对比

| 模型 | 参数量 | ImageNet | 输出维度 | 推理时间 | 推荐场景 |
|------|--------|---------|----------|----------|----------|
| **EfficientNet-B0** | 5.3M | 77.1% | 1280 | ~30ms | **平衡推荐** |
| MobileNetV3-L | 5.4M | 75.2% | 1280 | ~25ms | 速度优先 |
| MobileOne-s0 | 4.1M | 79.1% | 1280 | ~20ms | 最佳速度 |
| ResNet50 | 25.6M | 76.1% | 2048 | ~50ms | 精度优先 |
| EfficientNetV2-S | 21.5M | 84.1% | 1280 | ~60ms | 最高精度 |

### 7.3 移动端推理基准 (骁龙 865)

| 模型组合 | 总模型大小 | 总推理时间 | FPS |
|----------|-----------|------------|-----|
| YOLOv8n + MobileNetV3 | 32MB | ~45ms | 22 |
| YOLOv8n + EfficientNet-B0 | 32MB | ~50ms | 20 |
| YOLOv10n + MobileOne-s0 | 25MB | ~35ms | 28 |
| YOLOv8n + MobileOne-s0 | 20MB | ~40ms | 25 |

---

## 8. 常见问题

### Q1: TFLite 模型推理结果不正确？

可能原因：
1. **输入预处理不一致** - 确保归一化方式相同
2. **输出格式解析错误** - 检查模型输出形状
3. **模型未正确加载** - 检查文件是否完整

解决方案：
```python
# 验证输入输出形状
interpreter = tf.lite.Interpreter('model.tflite')
print(interpreter.get_input_details())
print(interpreter.get_output_details())
```

### Q2: 训练时 GPU 显存不足？

解决方案：
1. 减小 `batch_size`
2. 使用更小的 `imgsz`
3. 使用更小的模型 (n → s → m)

```bash
# 低显存配置
yolo detect train data=dataset.yaml model=yolov8n.pt epochs=100 batch=4 imgsz=416
```

### Q3: 标注工具输出的格式不对？

确保设置正确的输出格式：
- LabelImg: `Menu → View → Auto Save Mode → Change Output Dir`
- 选择 YOLO 格式后，左侧会显示 `YOLO TXT`

### Q4: 如何提升小物体检测精度？

```python
# 使用更高分辨率
model.train(data='dataset.yaml', model='yolov8n.pt', imgsz=1280)

# 开启超大物体检测
model.train(data='dataset.yaml', model='yolov8n.pt', close_mosaic=10)
```

---

## 9. 快速启动清单

- [ ] 安装环境: `pip install ultralytics tensorflow`
- [ ] 下载 YOLOv8n: `wget https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.pt`
- [ ] 准备数据集 (参考第5节)
- [ ] 转换模型为 TFLite (参考第2-3节)
- [ ] 放置模型到 `app/src/main/assets/models/`
- [ ] 运行应用测试

---

如有问题，请检查 [Ultralytics 官方文档](https://docs.ultralytics.com) 或 [TensorFlow Lite 文档](https://www.tensorflow.org/lite)。

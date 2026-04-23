# `LightingManager` 类级分析

## 1. 类职责

`LightingManager` 负责游戏中的动态光照与探索迷雾。

它让场景不仅“可见”，还具有：

- 环境光
- 点光源
- 视野遮挡
- 探索过区域的保留

## 2. 核心状态

- `enabled`
- `explorationMode`
- `ambientLight`
- `intensityMultiplier`
- 光源列表
- 叠加缓冲
- 已探索缓存

## 3. 光源模型

### `LightSource`

这是一个轻量记录类型，包含：

- 位置
- 半径
- 强度

## 4. 主要方法

- `setEnabled()`
- `setExplorationMode()`
- `setAmbientLight()`
- `setIntensityMultiplier()`
- `addLight()`
- `clearLights()`
- `resetExploration()`
- `render()`

## 5. 渲染逻辑

渲染时，光照系统会：

1. 根据世界状态收集光源
2. 绘制环境亮度
3. 叠加局部光源
4. 处理遮挡与阴影
5. 更新探索区域缓存

## 6. 与游戏对象的关系

光照来源不只来自场景灯，还可能来自：

- 玩家
- 终点
- 投射物
- 特定道具

因此这个类其实在渲染层和对象层之间起到了桥梁作用。

## 7. 设计价值

这个系统让游戏画面更有层次感，也让“视野”和“地形探索”成为玩法的一部分。


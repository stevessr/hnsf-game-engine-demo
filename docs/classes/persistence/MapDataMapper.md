# `MapDataMapper` 类级分析

## 1. 类职责

`MapDataMapper` 是“运行时世界 / DTO / JSON”之间的桥梁。

它的目标是：让同一份地图数据能在不同存储层和运行层之间无损流动。

## 2. 转换链路

### 2.1 世界 → DTO

`fromWorld(GameWorld world, String name)`

把当前世界快照转成 `MapData`。

### 2.2 DTO → JSON

`exportToJson(MapData mapData)`

导出为可分享的 JSON 结构。

### 2.3 JSON → DTO

`importFromJson(JSONObject json)`

从外部文件恢复地图数据。

### 2.4 DTO → 世界

`toWorld(MapData mapData)`

创建新的 `GameWorld`。

### 2.5 DTO → 现有世界

`applyToWorld(GameWorld world, MapData mapData)`

把地图应用到现有世界，适合“重载关卡”。

## 3. 背景与图片

`MapDataMapper` 负责：

- 背景颜色
- 背景模式
- 背景预设
- 背景图片名
- 背景图片数据编码/解码

这使地图不只是“对象列表”，还包含视觉层信息。

## 4. 对象转换

对象转换使用 `GameObjectFactory`：

- 运行时对象 → `ObjectData`
- `ObjectData` → 运行时对象

它支持把对象的专属属性装进 `extraJson`，再在反序列化时恢复。

## 5. 关卡属性

映射时还会处理：

- 重力
- 胜利条件
- 目标击杀数
- 目标收集数

因此关卡不仅有布局，还能直接带规则。

## 6. 应用到世界时的策略

`applyToWorld()` 会：

1. 保留部分玩家设置
2. 重置世界尺寸和背景
3. 清空旧对象
4. 重建对象
5. 恢复玩家设置

这个设计让“切换关卡”比较平滑，不会把用户设置完全冲掉。

## 7. 设计价值

这个类把“保存格式”和“运行时格式”分离开了，所以：

- 数据文件更稳定
- 游戏对象更灵活
- 编辑器和游戏可以共用同一套地图数据


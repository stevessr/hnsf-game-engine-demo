# `GameWorld` 类级分析

## 1. 类职责

`GameWorld` 是游戏运行时的“世界容器”。

它把以下内容统一收纳起来：

- 场景尺寸
- 背景
- 关卡目标
- 实体集合
- 物理引擎
- 光照系统
- 声音系统
- 摄像机
- 状态机

可以把它理解成“**关卡运行的总数据结构**”。

## 2. 核心成员

### 2.1 运行组件

- `EntityManager`
- `PhysicsEngine`
- `LightingManager`
- `SoundManager`

### 2.2 世界外观

- 宽高
- 背景颜色
- 背景模式
- 背景预设
- 背景图片

### 2.3 关卡状态

- 胜利条件
- 目标击杀数
- 目标收集数
- 实际击杀数
- 实际收集数
- 失败原因
- 是否显示目标

### 2.4 运行反馈

- 复活点
- 屏幕震动
- 背景缓存

## 3. 对外方法分组

### 3.1 目标与进度

- `setWinCondition()`
- `setTargetKills()`
- `setTargetItems()`
- `recordKill()`
- `recordItemCollection()`
- `isComplete()`

### 3.2 复活系统

- `setRespawnPoint()`
- `clearRespawnPoint()`
- `refreshRespawnPointFromPlayers()`
- `respawnPlayer()`

### 3.3 世界配置

- `setSize()`
- `setBackgroundColor()`
- `setBackgroundMode()`
- `setBackgroundPreset()`
- `setBackgroundImage()`
- `clearBackgroundImage()`
- `setGravityEnabled()`
- `setGravityStrength()`

### 3.4 对象管理

- `addObject()`
- `removeObject()`
- `getObjects()`
- `getActiveObjects()`
- `getObjectsByType()`
- `getSolidObjects()`
- `findPlayer()`

### 3.5 移动与碰撞

- `moveObject()`
- `collidesWithSolid()`
- `getCollisions()`

### 3.6 更新与渲染

- `update()`
- `render()`
- `renderBackgroundLayer()`
- `renderWorldLayer()`
- `renderUiLayer()`

## 4. 更新流程

`update(deltaSeconds)` 的逻辑是：

1. 忽略非法帧时间
2. 如果当前状态不允许更新世界，则直接返回
3. 更新屏幕震动
4. 让 `EntityManager` 更新所有对象
5. 对失活对象调用 `updateInactive()`

这意味着 `GameWorld` 本身不写具体游戏规则，而是把规则委托给对象和管理器。

## 5. 渲染流程

### 5.1 背景层

`renderBackgroundLayer()` 会先使用缓存绘制背景。

### 5.2 世界层

`renderWorldLayer()` 会：

- 通过摄像机平移世界坐标
- 绘制场景与实体
- 绘制光照层

### 5.3 UI 层

`renderUiLayer()` 会把菜单、对话框等 UI 直接绘制到屏幕层。

## 6. 背景缓存

`GameWorld` 对背景采用缓存策略，避免每帧重新生成渐变或主题背景。

只有在以下情况才会重建缓存：

- 世界尺寸改变
- 背景配置变化
- 背景图变化

## 7. 设计价值

`GameWorld` 的价值不只是“装对象”，而是把“游戏运行所需的上下文”统一管理起来。

这让下层对象可以通过 `GameWorld` 访问：

- 物理
- 光照
- 声音
- 目标
- 状态

同时又不会直接依赖 Swing 窗口。


# `PlayerObject` 类级分析

## 1. 类职责

`PlayerObject` 是玩家角色的完整行为实现。

它继承自 `ActorObject`，除了血量和攻击力之外，还管理：

- 等级与经验
- 体力与冲刺
- 油门与减速度
- 朝向
- 弹种
- 射击冷却
- 炸弹蓄力
- 光照范围
- 治疗特效
- 受击无敌

## 2. 主要状态

- `level`
- `experience`
- `velocityX`
- `velocityY`（继承）
- `throttlePower`
- `deceleration`
- `stamina`
- `projectileType`
- `lastDirX` / `lastDirY`
- `bombChargeActive`

## 3. 成长系统

### 3.1 经验与升级

玩家有等级和经验条：

- `gainExperience()`
- `experienceNeededForNextLevel()`

经验达到阈值后会自动升级。

### 3.2 体力系统

冲刺会消耗体力，停止移动后体力逐渐恢复。

相关方法包括：

- `sprintAccelerate()`
- `recoverStamina()`
- `setMaxStamina()`
- `setStamina()`

## 4. 移动系统

### `accelerate()`

按当前油门施加普通加速度。

### `sprintAccelerate()`

冲刺加速版本，会额外扣除体力。

### `update()`

每帧会做：

1. 死亡动画
2. 怪物接触伤害
3. 互补色伤害
4. 重力影响
5. 光球效果倒计时
6. 治疗特效倒计时
7. 速度衰减
8. 位置移动与碰撞

## 5. 战斗系统

### 5.1 射击

`shoot()` 和 `shoot(world, targetX, targetY)` 支持：

- 按最后朝向开火
- 朝鼠标点开火

### 5.2 炸弹蓄力

炸弹类型会走蓄力流程：

- `beginBombCharge()`
- `updateBombCharge()`
- `releaseBombCharge()`
- `cancelBombCharge()`

### 5.3 切换弹种

`cycleProjectileType()` 可以在多种弹药之间循环。

## 6. 受击与保护

### 6.1 怪物碰撞

玩家与怪物接触会触发伤害和轻微击退。

### 6.2 互补色伤害

玩家碰到特定危险方块时，会根据颜色关系受到额外伤害。

### 6.3 无敌时间

受伤后会进入短暂无敌，避免连续碰撞瞬间掉血过快。

## 7. 渲染

玩家渲染分为：

- 本体
- 治疗特效
- 死亡特效
- 血条 / 体力条 / 名字

如果素材系统有图像，会优先使用素材；否则用几何方式绘制。

## 8. 复活

`respawnAt()` 会恢复：

- 位置
- 速度
- 血量
- 体力
- 射击状态
- 蓄力状态

这使玩家从“死亡/失败”恢复到可操作状态。


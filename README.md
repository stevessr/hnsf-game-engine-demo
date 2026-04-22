# HNSF Game Engine Demo

[![CI](https://github.com/stevessr/hnsf-game-engine-demo/actions/workflows/ci.yml/badge.svg)](https://github.com/stevessr/hnsf-game-engine-demo/actions/workflows/ci.yml)
[![Release](https://github.com/stevessr/hnsf-game-engine-demo/actions/workflows/release.yml/badge.svg)](https://github.com/stevessr/hnsf-game-engine-demo/actions/workflows/release.yml)

这是一个基于 Java Swing 开发的 2D 游戏引擎原型，旨在展示模块化的架构设计、物理引擎、光照系统以及灵活的关卡编辑能力。

## 核心特性

-   **摄像机系统 (Camera System)**: 摄像机平滑跟随主角，支持多倍于屏幕大小的大地图探索。
-   **人形主角 (Humanoid Character)**: 具有四向渲染、行走动画和动态属性。
-   **动态光照 (Dynamic Lighting)**: 
    -   支持环境光亮度调节和光源强度倍率。
    -   主角、出口和投影物均具有动态光源。
    -   视野增强道具（光球）可临时扩大探索范围。
-   **物理引擎 (Physics Engine)**: 
    -   支持重力、跳跃（带地面检测）和弹性碰撞。
    -   摩擦力/阻尼效果，确保操作手感丝滑。
-   **投影物武器 (Weapon System)**: 支持射击、伤害判定和击退效果。
-   **关卡编辑器 (Level Editor)**: 
    -   内置可视化编辑器，支持实时预览。
    -   支持体素（Voxel）建造与销毁。
    -   支持自定义材质（草地、木头、石砖）与纹理贴图。
-   **程序化生成 (Procedural Generation)**: 
    -   集成基于柏林噪音（Perlin Noise）的地形生成算法。
    -   自动生成具有随机起伏、森林植被和洞穴结构的无限可能关卡。
-   **持久化与分享**: 
    -   使用 SQLite (`maps.db`) 存储关卡。
    -   支持 JSON 格式的地图导入与导出。
    -   游戏设置（FPS、键位、音量等）自动保存。

## 操作指南 (Controls)

| 按键 | 功能 |
| :--- | :--- |
| **W/A/S/D** 或 **方向键** | 移动角色 |
| **空格 (Space)** | 跳跃 (需靠近地面) |
| **K** | 射击 / 攻击 |
| **T** | 切换移动动力倍率 (Throttle) |
| **C** | 循环切换角色颜色 |
| **P** 或 **Esc** | 暂停游戏 / 打开选项 |
| **鼠标左键** | (编辑器模式) 放置体素 |
| **鼠标右键** | (编辑器模式) 销毁体素 |

## 技术架构

-   **状态机 (State Machine)**: `DefaultGameStateMachine` 管理菜单、游戏、对话、结算等状态流转。
-   **实体管理 (EntityManager)**: 使用类型缓存优化大规模场景的渲染性能。
-   **解耦渲染**: 区分世界渲染层、光照叠加层和 UI 叠加层，确保 UI 不受光照影响。
-   **数据映射 (Mapper)**: `MapDataMapper` 实现领域模型与持久化模型（SQLite/JSON）的无损转换。

## 开发环境要求

-   JDK 17+
-   Gradle 8.0+

## 快速开始

```bash
# 编译并运行游戏
./gradlew run

# 运行地图编辑器
./gradlew run --args="editor"

# 执行测试
./gradlew test
```

## 游戏特点及玩法

### 开始游戏

启动游戏后，玩家将操控一个**人形角色**进入由程序化生成或预设的2D横版关卡。游戏目标根据关卡设定而定：**到达终点**、**消灭怪物**、**收集物品**或**清除地图上所有可收集物**。角色拥有**生命值（HP）**、**体力值（Stamina）** 和 **等级系统**，通过击败怪物获得经验值提升等级。

### 角色操控

- **移动系统**：使用 **W/A/S/D** 或 **方向键** 控制角色四向移动，配合物理引擎实现**丝滑的加速与减速**手感
- **跳跃机制**：按 **空格键** 跳跃，系统自动检测地面状态，只有**贴近地面时才可起跳**，跳跃力度固定为650单位
- **冲刺能力**：角色拥有**体力槽**，移动时会消耗体力实现加速冲刺，停止冲刺后体力自动恢复
- **颜色切换**：按 **C键** 循环切换角色颜色，但需注意**互补色危险机制**——与角色颜色互补的方块会造成伤害

### 战斗系统

- **射击攻击**：按 **K键** 向角色面向方向发射投射物，具有**0.3秒冷却时间**，命中敌人造成伤害的同时产生**击退效果**
- **投射物类型**：支持多种投射物类型，包括**标准弹**和**炸弹**，炸弹需要蓄力释放，蓄力时间影响威力和射程
- **怪物种类**：游戏包含7种特色怪物，各有独特行为模式：
  - **史莱姆**：绿色软体生物，移动缓慢但跳跃力强
  - **蜘蛛**：深褐色爬行怪物，速度最快，会主动追踪玩家
  - **蝙蝠**：紫色飞行单位，持续盘旋并从空中俯冲攻击
  - **幽灵**：半透明飘浮体，移动诡异难以预判
  - **石像鬼**：灰色坚硬单位，生命值较高
  - **飞龙**：橙红色精英怪物，攻击欲望强
  - **飞行器**：机械类敌人，高速移动
- **击杀奖励**：成功击杀怪物可获得**经验值奖励**，部分怪物死亡时会掉落**治疗道具**

### 道具系统

关卡中散落多种可拾取道具，接触即自动收集：

| 道具类型 | 外观颜色 | 效果说明 |
| :--- | :--- | :--- |
| **生命** | 绿色 | 立即恢复指定数值的生命值 |
| **速度** | 蓝色 | 永久提升移动加速度 |
| **光球** | 亮黄色 | **临时扩大视野范围**，持续15秒后恢复，可重生 |
| **护盾** | 紫色 | 提供额外防护效果 |
| **金币/宝石** | 金色/橙色 | 提供经验值加成，助力升级 |

### 光照与探索

游戏采用**动态光照系统**营造探索氛围：

- **环境光**：整体场景亮度可调节，昏暗环境增加探索紧张感
- **角色光源**：玩家自身作为光源照亮周围区域，**光照半径200单位**
- **目标光源**：关卡终点（金色传送门）发出脉动光芒，指引前进方向
- **光球道具**：拾取后临时扩大角色光照范围，便于探索暗区
- **怪物光源**：部分怪物和投射物也具有发光效果

### 关卡机制

- **胜利条件**：根据关卡配置，达成以下任一目标即可通关
  - 到达金色传送门终点
  - 消灭关卡中所有怪物
  - 消灭指定数量的怪物
  - 收集指定数量的物品
  - 清除地图上所有可收集物品
- **失败条件**：生命值归零，系统会记录失败原因（被特定怪物击败或接触互补色方块）
- **重生系统**：角色死亡后可在检查点重生，拥有短暂**1秒无敌时间**

### 物理引擎

游戏实现了完整的2D物理系统：

- **重力作用**：角色和部分怪物受重力影响自然下落
- **地面检测**：精确判断是否着地，决定跳跃是否可用
- **碰撞响应**：与墙壁、地板的碰撞会自动计算反弹与阻挡
- **摩擦阻尼**：移动具有惯性衰减，操作手感流畅自然
- **弹跳效果**：特定材质的表面支持弹性碰撞

### 关卡编辑器

使用 `./gradlew run --args="editor"` 启动关卡编辑器，支持：

- **体素建造**：鼠标左键放置，右键销毁
- **材质选择**：草地、木头、石砖三种材质
- **实时预览**：即时查看编辑效果
- **存档管理**：地图自动保存至 `maps.db` 数据库
- **导入导出**：支持JSON格式的地图文件分享

---

# HNSF Game Engine Demo (English)

A 2D game engine prototype built with Java Swing, showcasing modular architecture, physics, dynamic lighting, and level editing.

## Key Features

-   **Camera System**: Smooth player tracking with support for large, scrolling maps.
-   **Humanoid Character**: Procedural rendering with directional orientations and walking animations.
-   **Dynamic Lighting**: Adjustable ambient light and intensity. Player, goals, and projectiles act as light sources.
-   **Physics Engine**: Gravity, jumping with ground detection, and collision resolution.
-   **Weapon System**: Projectile firing with damage and knockback.
-   **Level Editor**: Visual editor with custom material support (Grass, Wood, Stone).
-   **Persistence**: SQLite-based map storage and JSON Import/Export for map sharing.

## Technical Highlights

-   **State Driven**: Clean game flow management using a robust state machine.
-   **Performance**: Cached entity rendering for large environments.
-   **Layered Pipeline**: World -> Lighting -> UI separation.

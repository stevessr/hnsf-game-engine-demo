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

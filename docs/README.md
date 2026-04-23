# primary_software 文档中心

本目录用于整理当前仓库 `primary_software` 的源码分析结果、架构说明和模块说明。

## 推荐阅读顺序

1. [项目实作报告](项目实作报告.md) —— 按“项目简介 / 前期准备 / 项目设计 / 系统实现 / 总结”的方式做整体概览。
2. [系统架构说明](architecture/系统架构.md) —— 了解程序启动、运行时流程和数据流。
3. [模块总览](modules/README.md) —— 先看各模块概览，再进入详细分析。
4. [参考索引](reference/README.md) —— 按包、枚举和测试快速查找源码文件。
5. [类级文档](classes/README.md) —— 按单个核心类继续下钻。

## 文档目录

```text
docs/
├── README.md
├── 项目实作报告.md
├── architecture/
│   └── 系统架构.md
├── modules/
│   ├── README.md
│   ├── 核心模块.md
│   ├── 编辑器与持久化.md
│   └── detail/
│       ├── README.md
│       ├── 输入系统.md
│       ├── 状态机.md
│       ├── 物理与碰撞.md
│       ├── 对象模型.md
│       ├── 渲染系统.md
│       ├── 编辑器.md
│       └── 持久化.md
├── reference/
│   ├── README.md
│   ├── 类索引.md
│   └── detail/
│       ├── README.md
│       ├── 枚举与DTO.md
│       └── 测试索引.md
└── classes/
    ├── README.md
    ├── runtime/
    │   ├── GameWorld.md
    │   ├── SwingGamePanel.md
    │   ├── DefaultGameStateMachine.md
    │   ├── Camera.md
    │   └── LightingManager.md
    ├── input/
    │   └── GameInputController.md
    ├── object/
    │   ├── PlayerObject.md
    │   ├── MonsterObject.md
    │   ├── ProjectileObject.md
    │   ├── SceneObject.md
    │   ├── MenuObject.md
    │   └── DialogObject.md
    ├── persistence/
    │   ├── MapRepository.md
    │   ├── MapDataMapper.md
    │   └── SettingsRepository.md
    └── editor/
        └── MapEditorController.md
```

## 说明

- 文档内容基于当前仓库的源码、测试代码和构建配置整理。
- 分析对象是 `src/main/java`、`src/test/java` 以及 `build.gradle.kts` 中定义的运行与构建方式。
- 如果后续源码结构变化，建议优先更新 `项目实作报告.md`、`modules/README.md` 和 `reference/README.md`，再同步其他模块文档。

## 详细子文档

如果你想按源码模块继续深入，请直接进入：

- [`modules/detail/输入系统.md`](modules/detail/输入系统.md)
- [`modules/detail/状态机.md`](modules/detail/状态机.md)
- [`modules/detail/物理与碰撞.md`](modules/detail/物理与碰撞.md)
- [`modules/detail/对象模型.md`](modules/detail/对象模型.md)
- [`modules/detail/渲染系统.md`](modules/detail/渲染系统.md)
- [`modules/detail/编辑器.md`](modules/detail/编辑器.md)
- [`modules/detail/持久化.md`](modules/detail/持久化.md)
- [`reference/detail/枚举与DTO.md`](reference/detail/枚举与DTO.md)
- [`reference/detail/测试索引.md`](reference/detail/测试索引.md)
- [`classes/README.md`](classes/README.md)

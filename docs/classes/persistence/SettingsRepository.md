# `SettingsRepository` 类级分析

## 1. 类职责

`SettingsRepository` 负责保存和读取游戏运行设置。

它把一组运行参数持久化到本地 JSON 文件中。

## 2. 默认文件

默认设置文件位于：

`~/.hnsfgame/settings.json`

它用于存储：

- 帧率
- 字体大小
- 分辨率
- 油门与减速度
- 重力
- 光照
- 音量
- 按键绑定

## 3. 主要方法

- `saveSettings(...)`
- `loadSettings()`

## 4. 存储内容

设置内容不仅包括显示和性能项，还包括输入系统相关信息。

这意味着下次启动时，玩家的按键映射可以继续沿用。

## 5. 设计价值

`SettingsRepository` 让游戏具有“配置持久化”能力，使其更接近成品程序而不是一次性演示。

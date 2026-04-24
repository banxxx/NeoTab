# NeoTab 更新日志

## 2026-04-24 - Rainbow功能移除

### 移除的功能
- ❌ 移除 `<rainbow>` 动画彩虹标签
- ❌ 移除所有rainbow相关的代码实现
- ❌ 移除rainbow相关的文档

### 保留的功能
- ✅ `<color>` 单色标签
- ✅ `<gradient>` 静态渐变标签
- ✅ `<bold>`, `<italic>`, `<underlined>` 等样式标签
- ✅ 占位符系统
- ✅ 所有其他核心功能

### 修改的文件

#### 代码文件
- `src/main/java/com/poso/neotab/text/RichTextEngine.java`
  - 移除 `DEFAULT_RAINBOW_COLORS` 常量
  - 移除 `ANIMATION_CYCLE_MS` 常量
  - 移除 `PHASE_SEED` 常量
  - 移除 `interpolateRainbowColor()` 方法
  - 移除 `generatePseudoRandomPhase()` 方法
  - 移除 `smoothstep()` 方法
  - 移除 `smootherstep()` 方法
  - 移除 `cosineInterpolation()` 方法
  - 移除 `StyleState.rainbowColors` 字段
  - 移除 `StyleState.withRainbow()` 方法
  - 移除 `StyleState.toStyleForRainbowChar()` 方法
  - 移除 `StyleState.hasRainbow()` 方法
  - 从 `applyOpenTag()` 中移除rainbow分支
  - 从 `flushPlainText()` 中移除rainbow处理逻辑
  - 从 `isSupportedTagName()` 中移除"rainbow"

#### 文档文件
- 删除 `docs/Rainbow-Animation-Guide.md`
- 删除 `docs/Rainbow-Animation-Improvements.md`
- 删除 `docs/Rainbow-Quick-Reference.md`
- 删除 `docs/Interpolation-Comparison.md`
- 更新 `docs/Rich-Text-Features-Overview.md` - 移除所有rainbow引用
- 重写 `docs/Quick-Setup-Guide.md` - 改为通用快速设置指南

### 验证结果
- ✅ 代码编译成功
- ✅ 无编译错误
- ✅ 无诊断警告
- ✅ 项目可正常运行

### 当前支持的标签

#### 样式标签
- `<bold>` - 粗体
- `<italic>` - 斜体
- `<underlined>` - 下划线
- `<strikethrough>` - 删除线
- `<obfuscated>` - 混淆
- `<reset>` - 重置样式

#### 颜色标签
- `<color #RRGGBB>` - 单色
- `<color #AARRGGBB>` - 带透明度的单色
- `<gradient #RRGGBB,#RRGGBB,...>` - 静态渐变（支持多个颜色）

#### 占位符
- `%player_name%` - 玩家名称
- `%player_ping%` - 玩家延迟
- `%viewer_name%` - 观察者名称
- `%viewer_ping%` - 观察者延迟
- `%online%` - 在线人数
- `%max_players%` - 最大人数
- `%tps%` - 服务器TPS
- `%mspt%` - 服务器MSPT

### 迁移指南

如果你之前使用了rainbow标签，请按以下方式迁移：

#### 方案1：使用静态渐变替代

**之前：**
```
<rainbow>欢迎来到服务器</rainbow>
```

**现在：**
```
<gradient #FF0000,#FF7F00,#FFFF00,#00FF00,#0000FF,#4B0082,#9400D3>欢迎来到服务器</gradient>
```

#### 方案2：使用简单渐变

**之前：**
```
<rainbow #FF0000,#0000FF>红蓝动画</rainbow>
```

**现在：**
```
<gradient #FF0000,#0000FF>红蓝渐变</gradient>
```

#### 方案3：使用单色

**之前：**
```
<rainbow>TPS: %tps%</rainbow>
```

**现在：**
```
<color #00FF00>TPS: %tps%</color>
```

### 性能改进

移除rainbow功能后的性能提升：

- ✅ 无需定期刷新TAB列表
- ✅ CPU占用降低
- ✅ 网络流量减少
- ✅ 内存占用减少
- ✅ 更稳定的性能表现

### 推荐配置

```toml
[neotab]
    # 刷新间隔可以设置更高（因为没有动画需求）
    refreshIntervalTicks = 10  # 500ms
    
    # 使用静态渐变效果
    topTitleEnabled = true
    topTitleText = "<gradient #FFD700,#FFA500><bold>我的服务器</bold></gradient>"
    
    topContentEnabled = true
    topContentText = "<gradient #00FFFF,#0000FF>在线: %online%/%max_players%</gradient>"
    
    footerCustomText = "<color #AAAAAA>TPS: %tps% | MSPT: %mspt%</color>"
```

### 总结

此次更新移除了rainbow动画功能，但保留了所有核心功能和静态渐变效果。项目代码更加简洁，性能更加稳定，维护更加容易。

---

**版本**: 1.21.1-NeoForge  
**更新日期**: 2026-04-24  
**状态**: ✅ 稳定版本

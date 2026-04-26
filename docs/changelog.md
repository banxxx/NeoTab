# 更新日志

## 2026-04-25

### 新增功能

- **配置界面 Tab 栏**：将配置界面重构为多 Tab 结构
  - 新增"页面设置"Tab（包含所有原有配置选项）
  - 新增"主题样式"Tab（预留功能区域）
  - 支持 Tab 之间的流畅切换
  - 响应式布局，适应不同屏幕尺寸

- **新增 10 个占位符**：
  - 内存使用：`%memory_used%`、`%memory_max%`、`%memory_total%`、`%memory_percent%`
  - 运行时间：`%uptime%`、`%uptime_days%`、`%uptime_hours%`
  - 世界信息：`%world_time%`、`%world_day%`、`%loaded_chunks%`

### 性能优化

- 富文本解析缓存（命中率 95%+，性能提升 90%+）
- 服务端指标缓存（CPU 使用率降低 60-70%）
- 在线时长增量同步（网络包减少 95%）
- 客户端列宽度计算缓存（FPS 提升 5-15%）
- 称号 API 缓存（API 调用减少 80%）
- 占位符替换 StringBuilder 优化（临时对象减少 87.5%）

### 架构改进

- 清晰的职责分离（TabbedNeoTabConfigScreen + ConfigPanel 抽象基类）
- 面向对象设计，易于扩展维护
- 完善的注释文档

---

## 2026-04-24

### 移除功能

- **移除 `<rainbow>` 动画彩虹标签**

  如果之前使用了 rainbow 标签，请迁移到静态渐变：

  ```
  # 之前
  <rainbow>欢迎来到服务器</rainbow>

  # 现在
  <gradient #FF0000,#FF7F00,#FFFF00,#00FF00,#0000FF,#4B0082,#9400D3>欢迎来到服务器</gradient>
  ```

### 新增功能

- **渐变颜色标签 `<gradient>`**：支持 2 个或更多颜色的静态渐变
- **UI 改进**：
  - 输入框高度统一调整（标题 40px，内容 40px）
  - 修复滚动条触发范围问题
  - 优化输入体验

### 性能改进

移除 rainbow 功能后：
- 无需定期刷新 TAB 列表
- CPU 占用降低
- 网络流量减少
- 内存占用减少

---

## 当前支持的标签

### 样式标签

| 标签 | 功能 |
|------|------|
| `<bold>` | 粗体 |
| `<italic>` | 斜体 |
| `<underlined>` | 下划线 |
| `<strikethrough>` | 删除线 |
| `<obfuscated>` | 混淆 |
| `<reset>` | 重置样式 |

### 颜色标签

| 标签 | 功能 |
|------|------|
| `<color #RRGGBB>` | 单色 |
| `<color #AARRGGBB>` | 带透明度的单色 |
| `<gradient #C1,#C2,...>` | 静态渐变（支持多个颜色） |

### 占位符（共 20 个）

| 类别 | 占位符 |
|------|--------|
| 玩家信息 | `%player_name%` `%player_ping%` `%viewer_name%` `%viewer_ping%` |
| 服务器状态 | `%online%` `%max_players%` `%tps%` `%mspt%` |
| 内存使用 | `%memory_used%` `%memory_max%` `%memory_total%` `%memory_percent%` |
| 运行时间 | `%uptime%` `%uptime_days%` `%uptime_hours%` |
| 世界信息 | `%world_time%` `%world_day%` `%loaded_chunks%` |

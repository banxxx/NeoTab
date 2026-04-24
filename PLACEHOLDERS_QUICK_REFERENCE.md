# 🚀 占位符快速参考

## 📋 所有可用占位符（20个）

### 👤 玩家信息
```
%player_name%    - 目标玩家名称
%player_ping%    - 目标玩家延迟（ms）
%viewer_name%    - 查看者名称
%viewer_ping%    - 查看者延迟（ms）
```

### 🖥️ 服务器状态
```
%online%         - 在线人数
%max_players%    - 最大人数
%tps%            - 服务器 TPS（如：19.85）
%mspt%           - 每 Tick 毫秒数（如：45.23）
```

### 💾 内存使用 ✨ 新增
```
%memory_used%    - 已使用内存 MB（如：2048）
%memory_max%     - 最大内存 MB（如：4096）
%memory_total%   - 内存使用情况（如：2048/4096MB）
%memory_percent% - 内存使用百分比（如：50.0%）
```

### ⏱️ 运行时间 ✨ 新增
```
%uptime%         - 运行时间（如：2d 5h 30m）
%uptime_days%    - 运行天数（如：2）
%uptime_hours%   - 运行总小时数（如：53）
```

### 🌍 世界信息 ✨ 新增
```
%world_time%     - 主世界时间（如：14:30）
%world_day%      - 主世界天数（如：127）
%loaded_chunks%  - 已加载区块数（如：1523）
```

---

## 🎨 常用配置模板

### 模板 1: 简洁状态栏
```
在线: %online%/%max_players% | TPS: %tps% | 内存: %memory_percent%
```

### 模板 2: 详细监控
```
<gradient #FFD700,#FFA500>服务器状态</gradient>
TPS: %tps%  MSPT: %mspt%
内存: %memory_total% (%memory_percent%)
运行: %uptime% | 在线: %online%/%max_players%
```

### 模板 3: 世界信息
```
主世界第 %world_day% 天  时间: %world_time%
已加载区块: %loaded_chunks%
```

### 模板 4: 性能面板
```
<bold>性能监控</bold>
TPS: %tps% | MSPT: %mspt%
内存: %memory_used%/%memory_max%MB
区块: %loaded_chunks%
```

---

## 💡 快速提示

✅ **所有占位符都是高效的** - 无性能开销  
✅ **可以组合使用** - 支持富文本标签  
✅ **自动格式化** - 数字自动保留小数位  
✅ **实时更新** - 根据配置的刷新间隔更新

---

## 📖 详细文档

- **NEW_PLACEHOLDERS_GUIDE.md** - 完整使用指南
- **PLACEHOLDERS_SUMMARY.md** - 功能总结
- **VANILLA_API_ANALYSIS.md** - 技术分析

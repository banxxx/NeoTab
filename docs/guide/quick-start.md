# 快速开始

## 步骤 1：安装模组

将 `neotab-x.x.x.jar` 放入服务器的 `mods/` 目录，重启服务器。

## 步骤 2：打开配置界面

管理员在游戏中执行：

```
/neotab config
```

## 步骤 3：配置 TAB 内容

在配置界面的**页面配置**标签页中调整各项设置，点击**完成**保存。

配置立即对所有在线玩家生效。

---

## 推荐配置方案

### 方案 A：简洁风格

```toml
topTitleEnabled = true
topTitleText = "<bold>我的服务器</bold>"

topContentEnabled = true
topContentText = "在线: %online%/%max_players%"

footerCustomText = "欢迎游玩！"
```

### 方案 B：渐变风格（推荐）

```toml
topTitleEnabled = true
topTitleText = "<gradient #FFD700,#FFA500><bold>我的服务器</bold></gradient>"

topContentEnabled = true
topContentText = "<gradient #00FFFF,#0000FF>在线: %online%/%max_players%</gradient>"

footerCustomText = "<color #AAAAAA>TPS: %tps% | MSPT: %mspt%</color>"
```

### 方案 C：详细监控面板

```toml
topTitleEnabled = true
topTitleText = "<gradient #FFD700,#FFA500><bold>服务器状态</bold></gradient>"

topContentEnabled = true
topContentText = """
TPS: %tps%  MSPT: %mspt%
内存: %memory_total% (%memory_percent%)
运行: %uptime% | 在线: %online%/%max_players%
"""

footerCustomText = "<color #AAAAAA>主世界第 %world_day% 天  %world_time%</color>"
```

---

## 常用命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/neotab config` | 打开图形化配置界面 | 管理员 |
| `/neotab reload` | 从磁盘重新加载配置并立即刷新 | 管理员 |

---

## 延迟颜色规则

启用"更好的延迟显示"后：

| 延迟范围 | 颜色 |
|---------|------|
| 0 - 99ms | 🟢 绿色 |
| 100 - 199ms | 🟡 黄色 |
| 200 - 349ms | 🟠 橙色 |
| 350ms 及以上 | 🔴 红色 |

## 在线时长规则

启用"在线时长显示"后：

- 不足 1 小时按 `1h` 显示
- 满 24 小时进位为天
- 示例：`1h`、`6h`、`1d1h`、`2d7h`

> 在线时长使用 Minecraft 原版 `minecraft:play_time` 统计项，数据持久化保存，**服务器重启后不会重置**。

---

## 故障排除

**配置不生效？**
1. 确认配置文件路径正确
2. 检查配置文件格式
3. 尝试 `/neotab reload` 重载配置
4. 检查服务器日志是否有错误

**标签显示为原文？**
1. 检查标签语法是否正确
2. 确认颜色格式为 `#RRGGBB`
3. 检查标签是否正确闭合

**占位符不显示？**
1. 确认占位符名称拼写正确
2. 查看服务器日志
3. 确认相关功能已启用

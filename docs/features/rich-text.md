# 富文本标签

NeoTab 支持在所有文本输入框中使用富文本标签，可以与占位符混合使用。

## 样式标签

```
<bold>粗体文字</bold>
<italic>斜体文字</italic>
<underlined>下划线文字</underlined>
<strikethrough>删除线文字</strikethrough>
<obfuscated>混淆文字</obfuscated>
<reset>
```

## 颜色标签

```
<color #FF0000>红色文字</color>
<color #00FF00>绿色文字</color>
<color #0000FF>蓝色文字</color>
```

颜色格式为 `#RRGGBB`（6位十六进制）或 `#AARRGGBB`（8位，含透明度）。

## 渐变标签

```
<gradient #FF0000,#0000FF>红蓝渐变</gradient>
<gradient #FF0000,#00FF00,#0000FF>RGB三色渐变</gradient>
```

支持 2 个或更多颜色值，用英文逗号分隔。详见[渐变颜色](./gradient)。

## 组合使用

标签可以嵌套组合：

```
<gradient #FFD700,#FFA500><bold>服务器名称</bold></gradient>
<color #00FF00><underlined>在线: %online%/%max_players%</underlined></color>
<color #FFFF55><italic>欢迎 %viewer_name% 加入！</italic></color>
```

## 支持的标签一览

| 标签 | 功能 | 示例 |
|------|------|------|
| `<color #RRGGBB>` | 单色文字 | `<color #FF0000>红色</color>` |
| `<gradient #C1,#C2,...>` | 渐变文字 | `<gradient #FF0000,#0000FF>渐变</gradient>` |
| `<bold>` | 粗体 | `<bold>粗体</bold>` |
| `<italic>` | 斜体 | `<italic>斜体</italic>` |
| `<underlined>` | 下划线 | `<underlined>下划线</underlined>` |
| `<strikethrough>` | 删除线 | `<strikethrough>删除线</strikethrough>` |
| `<obfuscated>` | 混淆 | `<obfuscated>混淆</obfuscated>` |
| `<reset>` | 重置样式 | `<reset>` |

## 完整 TAB 配置示例

**顶部标题：**
```
<gradient #FFD700,#FFA500><bold>我的 Minecraft 服务器</bold></gradient>
```

**顶部内容：**
```
<gradient #00FFFF,#0000FF>在线玩家: %online%/%max_players%</gradient>
<color #FFFF00>欢迎 %viewer_name% 加入游戏！</color>
```

**底部自定义信息：**
```
<color #AAAAAA>TPS: %tps% | MSPT: %mspt% | 内存: %memory_percent%</color>
```

## 最佳实践

**推荐做法：**
- 在标题和重要信息使用特效
- 合理设置刷新间隔（100-200ms）
- 保持整体风格统一
- 确保文字清晰可读，使用足够的对比度

**不推荐做法：**
- 过多的颜色混搭
- 过于刺眼的颜色
- 超长文本（>100字符）使用渐变
- 嵌套多层复杂标签

## 常见问题

**Q: 可以嵌套使用标签吗？**

A: 可以，但要注意：
- 样式标签（bold、italic 等）可以嵌套
- 颜色标签（color、gradient）会相互覆盖
- 建议不要过度嵌套

**Q: 占位符可以在任何标签中使用吗？**

A: 可以！占位符会先被替换，然后再应用富文本样式。

**Q: 标签不生效怎么办？**

1. 检查标签语法是否正确
2. 确认颜色格式为 `#RRGGBB`
3. 检查标签是否正确闭合

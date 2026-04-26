# 称号系统 API

其他模组可以通过 NeoTab 提供的 API 为玩家添加称号，称号支持所有富文本标签。

## 方式一：实现 TitleProvider 接口

### 步骤 1：实现接口

```java
package com.yourmod.integration;

import com.poso.neotab.api.TitleProvider;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class YourModTitleProvider implements TitleProvider {

    @Override
    @Nullable
    public String getTitle(ServerPlayer player) {
        String rank = YourMod.getPlayerRank(player);
        if (rank == null) return null;

        return switch (rank) {
            case "admin" -> "<color #FF0000><bold>『管理员』</bold></color>";
            case "vip"   -> "<color #FFD700>『VIP』</color>";
            case "member"-> "<color #00FF00>『会员』</color>";
            default      -> null;
        };
    }

    @Override
    public int getPriority() {
        return 100; // 数值越大优先级越高
    }

    @Override
    public String getProviderId() {
        return "yourmod:rank_system";
    }
}
```

### 步骤 2：注册提供者

```java
@Mod("yourmod")
public class YourMod {
    public YourMod() {
        NeoTabAPI.registerTitleProvider(new YourModTitleProvider());
    }
}
```

## 方式二：监听事件

```java
@EventBusSubscriber(modid = "yourmod")
public class YourModEvents {

    @SubscribeEvent
    public static void onGetPlayerTitle(GetPlayerTitleEvent event) {
        if (event.hasTitle()) return; // 让其他模组的称号优先

        String title = YourMod.calculateTitle(event.getPlayer());
        if (title != null) {
            event.setTitle(title);
        }
    }
}
```

## 称号文本格式

称号文本支持所有 NeoTab 富文本标签：

```
<color #FFD700>『黄金会员』</color>
<gradient #FF0000,#FF7F00><bold>『管理员』</bold></gradient>
<bold><color #9400D3>『紫晶学者』</color></bold>
```

## 优先级设置

| 称号类型 | 建议优先级 |
|---------|----------|
| 系统级（管理员、服主） | 1000+ |
| 等级称号（VIP） | 500-999 |
| 临时称号（活动奖励） | 100-499 |
| 默认称号 | 0-99 |

## 完整示例

```java
public class RankSystemIntegration {

    public static void init() {
        if (ModList.get().isLoaded("neotab")) {
            NeoTabAPI.registerTitleProvider(new RankTitleProvider());
        }
    }

    private static class RankTitleProvider implements TitleProvider {

        @Override
        @Nullable
        public String getTitle(ServerPlayer player) {
            String rank = YourMod.getPlayerRank(player);
            int level = YourMod.getPlayerLevel(player);

            if (rank != null) return formatRankTitle(rank);
            if (level > 0)    return formatLevelTitle(level);
            return null;
        }

        private String formatRankTitle(String rank) {
            return switch (rank.toLowerCase()) {
                case "owner" -> "<gradient #FF0000,#FFD700><bold>『服主』</bold></gradient>";
                case "admin" -> "<color #FF0000><bold>『管理员』</bold></color>";
                case "mod"   -> "<color #00FF00><bold>『版主』</bold></color>";
                case "vip"   -> "<color #FFD700>『VIP』</color>";
                default      -> "<color #CCCCCC>『" + rank + "』</color>";
            };
        }

        private String formatLevelTitle(int level) {
            if (level >= 100) return "<gradient #FFD700,#FF8C00>『Lv." + level + "』</gradient>";
            if (level >= 50)  return "<color #FFD700>『Lv." + level + "』</color>";
            if (level >= 20)  return "<color #00FF00>『Lv." + level + "』</color>";
            return "<color #CCCCCC>『Lv." + level + "』</color>";
        }

        @Override
        public int getPriority() { return 500; }

        @Override
        public String getProviderId() { return "yourmod:rank_system"; }
    }
}
```

## 兼容性处理

```java
// 检查 NeoTab 是否存在
if (ModList.get().isLoaded("neotab")) {
    try {
        NeoTabAPI.registerTitleProvider(new YourModTitleProvider());
    } catch (Exception e) {
        LOGGER.warn("Failed to register NeoTab title provider", e);
    }
}
```

## 最佳实践

- **缓存称号数据**，避免频繁计算
- **在玩家数据变化时**主动更新缓存
- **避免在 `getTitle` 方法中**进行耗时操作
- **使用 try-catch** 包裹 API 调用，避免影响主逻辑

## 常见问题

**Q: 我的称号没有显示？**

1. 确认 NeoTab 配置中的"称号功能"已开启
2. 确认 TitleProvider 已正确注册
3. 确认 `getTitle` 方法返回了非空字符串
4. 检查控制台是否有错误日志

**Q: 多个模组的称号冲突？**

通过优先级控制，或在事件处理中检查 `event.hasTitle()`，与其他模组作者协调优先级分配。

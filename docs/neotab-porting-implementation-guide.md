# NeoTab 移植实施指南：从 NeoForge 1.21.1 到 Forge 1.20.1

## 项目概述

本文档基于现有的 `porting-guide-forge-1.20.1.md` 理论指南，提供具体的移植实施步骤。当前 Forge 1.20.1 项目已完成基础框架搭建，需要将 NeoForge 1.21.1 版本的 NeoTab 功能移植过来。

## 当前项目状态分析

### ✅ 已完成的基础设施
- **构建系统**：Forge 1.20.1 构建环境已配置完成
- **项目结构**：标准 Forge 项目目录结构已建立
- **基础配置**：`build.gradle`、`gradle.properties`、`mods.toml` 已正确配置
- **示例代码**：包含基础的 Forge 模组示例代码

### 📋 当前配置信息
- **Minecraft 版本**：1.20.1
- **Forge 版本**：47.4.20
- **Java 版本**：17
- **映射表**：Parchment 2023.09.03-1.20.1
- **模组 ID**：neotab

## 📝 移植任务清单 (Checklist)

### 🚀 阶段一：准备工作 (预估 2-3 小时)

#### 1.1 项目准备
- [ ] 备份当前 Forge 项目
- [ ] 创建 Git 分支用于移植工作
- [ ] 准备 NeoForge 源码访问（如果需要参考）

#### 1.2 清理示例代码
- [ ] 删除 `NeoTab.java` 中的 `BLOCKS` 注册器
- [ ] 删除 `NeoTab.java` 中的 `ITEMS` 注册器  
- [ ] 删除 `NeoTab.java` 中的 `CREATIVE_MODE_TABS` 注册器
- [ ] 删除 `EXAMPLE_BLOCK`、`EXAMPLE_BLOCK_ITEM`、`EXAMPLE_ITEM`、`EXAMPLE_TAB` 定义
- [ ] 删除 `addCreative()` 方法
- [ ] 删除或重构 `ClientModEvents` 内部类
- [ ] 清理 `Config.java` 中的示例配置项

#### 1.3 目录结构准备
- [ ] 创建 `src/main/java/com/poso/neotab/network/` 目录
- [ ] 创建 `src/main/java/com/poso/neotab/client/` 目录
- [ ] 创建 `src/main/java/com/poso/neotab/server/` 目录
- [ ] 创建 `src/main/java/com/poso/neotab/config/` 目录
- [ ] 创建 `src/main/java/com/poso/neotab/mixin/` 目录（如果需要）

### 阶段一：清理和准备工作 (★☆☆☆☆)

#### 1.1 清理示例代码
**需要删除的文件/代码：**
```java
// 在 NeoTab.java 中删除以下示例代码：
- BLOCKS、ITEMS、CREATIVE_MODE_TABS 注册器
- EXAMPLE_BLOCK、EXAMPLE_BLOCK_ITEM、EXAMPLE_ITEM、EXAMPLE_TAB
- addCreative() 方法
- ClientModEvents 内部类（如果不需要）
```

**保留的基础结构：**
```java
@Mod(NeoTab.MODID)
public class NeoTab {
    public static final String MODID = "neotab";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public NeoTab(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        // 网络注册将在这里添加
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // NeoTab 初始化逻辑
    }
}
```

#### 1.2 更新 Config.java
**需要完全重写：**
- 删除示例配置项
- 准备 NeoTab 特定的配置结构

### 阶段二：核心业务逻辑移植 (★★☆☆☆)

#### 2.1 可以直接复用的组件
以下组件可以从 NeoForge 版本直接复制，**无需修改**：

**配置相关类：**
- `TabConfig.java` - TAB 配置数据结构
- `PlayerConfig.java` - 玩家配置数据结构
- `CustomizePolicy.java` - 自定义策略枚举
- 所有配置序列化/反序列化逻辑

**数据模型类：**
- `OnlineDuration.java` - 在线时长数据模型
- `PlayerHealth.java` - 玩家血量数据模型
- 其他纯数据类

**工具类：**
- `TimeUtils.java` - 时间处理工具
- `ColorUtils.java` - 颜色处理工具
- `StringUtils.java` - 字符串处理工具
- 其他不依赖 Minecraft API 的工具类

**客户端 UI 组件：**
- `NeoTabConfigScreen.java` - 配置界面（可能需要微调）
- `NeoTabCustomizeScreen.java` - 自定义界面
- `MultiLineEditBox.java` - 多行文本框组件
- 其他 GUI 组件（需要检查 API 兼容性）

#### 2.2 需要适配的组件

**权限系统适配：**
```java
// 原 NeoForge 代码
public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
    ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "configure"),
    PermissionTypes.BOOLEAN,
    (player, uuid, contexts) -> player != null && player.hasPermissions(2)
);

// 需要改为 Forge 代码
public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
    NeoTab.MODID, "configure", PermissionTypes.BOOLEAN,
    (player, uuid, contexts) -> player != null && player.hasPermissions(2)
);
```

**资源位置创建：**
```java
// 检查并替换所有 ResourceLocation.fromNamespaceAndPath() 调用
// 改为：new ResourceLocation(namespace, path)
```

### 阶段三：网络系统重构 (★★★★★)

这是最复杂的部分，需要完全重写网络系统。

#### 3.1 创建网络管理器
**新建文件：`src/main/java/com/poso/neotab/network/NeoTabNetwork.java`**

```java
package com.poso.neotab.network;

import com.poso.neotab.NeoTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NeoTabNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NeoTab.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int id = 0;
        
        // 注册所有网络包
        INSTANCE.registerMessage(id++, OpenConfigScreenPacket.class,
            OpenConfigScreenPacket::encode,
            OpenConfigScreenPacket::decode,
            OpenConfigScreenPacket::handle);
            
        INSTANCE.registerMessage(id++, OpenCustomizeScreenPacket.class,
            OpenCustomizeScreenPacket::encode,
            OpenCustomizeScreenPacket::decode,
            OpenCustomizeScreenPacket::handle);
            
        INSTANCE.registerMessage(id++, SaveConfigPacket.class,
            SaveConfigPacket::encode,
            SaveConfigPacket::decode,
            SaveConfigPacket::handle);
            
        INSTANCE.registerMessage(id++, SavePlayerConfigPacket.class,
            SavePlayerConfigPacket::encode,
            SavePlayerConfigPacket::decode,
            SavePlayerConfigPacket::handle);
            
        INSTANCE.registerMessage(id++, SyncTabConfigPacket.class,
            SyncTabConfigPacket::encode,
            SyncTabConfigPacket::decode,
            SyncTabConfigPacket::handle);
            
        INSTANCE.registerMessage(id++, SyncCustomizePolicyPacket.class,
            SyncCustomizePolicyPacket::encode,
            SyncCustomizePolicyPacket::decode,
            SyncCustomizePolicyPacket::handle);
            
        INSTANCE.registerMessage(id++, SyncOnlineDurationsPacket.class,
            SyncOnlineDurationsPacket::encode,
            SyncOnlineDurationsPacket::decode,
            SyncOnlineDurationsPacket::handle);
            
        INSTANCE.registerMessage(id++, SyncPlayerHealthPacket.class,
            SyncPlayerHealthPacket::encode,
            SyncPlayerHealthPacket::decode,
            SyncPlayerHealthPacket::handle);
    }
}
```

#### 3.2 网络包类重构模板
**每个 Payload 类都需要重写为 Packet 类，以下是模板：**

```java
// 原 NeoForge Payload 类
public record OpenConfigScreenPayload(TabConfig config) implements CustomPacketPayload {
    // ... NeoForge 实现
}

// 新 Forge Packet 类
public class OpenConfigScreenPacket {
    private final TabConfig config;
    
    public OpenConfigScreenPacket(TabConfig config) {
        this.config = config;
    }
    
    public static void encode(OpenConfigScreenPacket packet, FriendlyByteBuf buf) {
        packet.config.write(buf);
    }
    
    public static OpenConfigScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenConfigScreenPacket(TabConfig.read(buf));
    }
    
    public static void handle(OpenConfigScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 处理逻辑（从原 Payload 的 handle 方法复制）
        });
        ctx.get().setPacketHandled(true);
    }
}
```

#### 3.3 需要重构的网络包列表
**必须重写的包类：**
1. `OpenConfigScreenPayload` → `OpenConfigScreenPacket`
2. `OpenCustomizeScreenPayload` → `OpenCustomizeScreenPacket`
3. `SaveConfigPayload` → `SaveConfigPacket`
4. `SavePlayerConfigPayload` → `SavePlayerConfigPacket`
5. `SyncTabConfigPayload` → `SyncTabConfigPacket`
6. `SyncCustomizePolicyPayload` → `SyncCustomizePolicyPacket`
7. `SyncOnlineDurationsPayload` → `SyncOnlineDurationsPacket`
8. `SyncPlayerHealthPayload` → `SyncPlayerHealthPacket`

#### 3.4 包发送方式更新
**需要全局替换的发送方式：**
```java
// 原 NeoForge 方式
PacketDistributor.sendToPlayer(player, new SyncTabConfigPayload(config));
PacketDistributor.sendToAllPlayers(new SyncTabConfigPayload(config));

// 新 Forge 方式
NeoTabNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncTabConfigPacket(config));
NeoTabNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncTabConfigPacket(config));
```

### 阶段四：事件系统适配 (★★★☆☆)

#### 4.1 事件类名称映射
**需要更新的事件类：**
```java
// NeoForge → Forge 事件映射
ServerTickEvent.Post → TickEvent.ServerTickEvent (检查 phase == Phase.END)
ClientTickEvent.Post → TickEvent.ClientTickEvent (检查 phase == Phase.END)
ClientPlayerNetworkEvent.LoggingOut → ClientPlayerNetworkEvent.LoggedOutEvent
RegisterPayloadHandlersEvent → 删除，改用网络注册
```

#### 4.2 事件处理类适配
**服务端事件处理：**
```java
@Mod.EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 原来的处理逻辑
        }
    }
}
```

**客户端事件处理：**
```java
@Mod.EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)
public final class NeoTabClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 原来的处理逻辑
        }
    }
}
```

### 阶段五：Mixin 系统适配 (★★☆☆☆)

#### 5.1 Mixin 配置文件检查
**检查 `neotab.mixins.json`：**
```json
{
  "required": true,
  "package": "com.poso.neotab.mixin",
  "compatibilityLevel": "JAVA_17", // 从 JAVA_21 改为 JAVA_17
  "mixins": [],
  "client": [
    "client.GuiMixin",
    "client.PlayerTabOverlayMixin",
    "client.PlayerTabOverlayAccessor"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

#### 5.2 Mixin 目标类验证
**需要验证的 Mixin 类：**
- `PlayerTabOverlayMixin` - 检查 1.20.1 中的方法签名
- `GuiMixin` - 检查 `renderTabList` 方法
- `PlayerTabOverlayAccessor` - 检查访问器目标

**验证步骤：**
1. 检查目标类是否存在
2. 检查目标方法签名是否匹配
3. 检查字段名称是否正确

### 阶段六：客户端 UI 适配 (★★☆☆☆)

#### 6.1 需要检查的 API
**可能需要适配的渲染 API：**
```java
// 检查这些方法在 1.20.1 中的签名
GuiGraphics.drawString()
GuiGraphics.fill()
Font.width()
Component.translatable()
```

#### 6.2 屏幕类适配
**主要检查点：**
- `NeoTabConfigScreen` - 检查父类构造函数
- `NeoTabCustomizeScreen` - 检查渲染方法
- `MultiLineEditBox` - 检查是否需要适配

### 阶段七：配置和资源文件 (★☆☆☆☆)

#### 7.1 可以直接复用的文件
**无需修改的资源文件：**
- `assets/neotab/lang/*.json` - 语言文件
- `assets/neotab/textures/**` - 纹理文件
- `data/neotab/**` - 数据文件

#### 7.2 需要检查的配置
**验证配置项：**
- 检查语言键在 1.20.1 中是否存在
- 验证纹理路径是否正确

## 移植实施步骤

### 步骤 1：准备工作 (预估 2-3 小时)
1. 备份当前项目
2. 清理示例代码
3. 创建源码目录结构
4. 从 NeoForge 版本复制可直接复用的类

### 步骤 2：网络系统重构 (预估 15-20 小时)
1. 创建 `NeoTabNetwork.java`
2. 逐个重写网络包类
3. 更新所有包发送调用
4. 测试网络通信

### 步骤 3：事件系统适配 (预估 5-8 小时)
1. 更新事件处理类
2. 适配事件名称和参数
3. 测试事件触发

### 步骤 4：权限和 API 适配 (预估 3-5 小时)
1. 更新权限节点创建
2. 适配 ResourceLocation 创建
3. 检查其他 API 调用

### 步骤 5：Mixin 和 UI 适配 (预估 5-8 小时)
1. 验证 Mixin 目标类
2. 适配客户端 UI
3. 测试渲染功能

### 步骤 6：集成测试 (预估 8-10 小时)
1. 功能完整性测试
2. 多人游戏测试
3. 性能测试
4. 兼容性测试

## 风险点和注意事项

### 🔴 高风险项
1. **网络包序列化兼容性** - 确保数据序列化格式一致
2. **Mixin 目标类变化** - 1.20.1 和 1.21.1 之间可能有类结构差异
3. **客户端渲染 API** - GUI 渲染方法可能有签名变化

### 🟡 中风险项
1. **事件参数变化** - 事件类的参数可能不完全一致
2. **权限系统差异** - Forge 权限 API 可能有细微差别

### 🟢 低风险项
1. **业务逻辑** - 核心 TAB 功能逻辑基本不变
2. **配置系统** - JSON 配置格式保持兼容
3. **资源文件** - 纹理和语言文件直接复用

## 测试策略

### 单元测试
- 配置序列化/反序列化
- 网络包编码/解码
- 工具类功能

### 集成测试
- 客户端-服务端通信
- 权限系统功能
- UI 界面操作

### 兼容性测试
- 单人游戏模式
- 多人游戏模式
- 与其他模组的兼容性

## 预估工作量

| 阶段 | 预估时间 | 难度 | 优先级 |
|------|----------|------|--------|
| 准备工作 | 2-3 小时 | ★☆☆☆☆ | 高 |
| 网络系统重构 | 15-20 小时 | ★★★★★ | 高 |
| 事件系统适配 | 5-8 小时 | ★★★☆☆ | 高 |
| 权限和 API 适配 | 3-5 小时 | ★★☆☆☆ | 中 |
| Mixin 和 UI 适配 | 5-8 小时 | ★★☆☆☆ | 中 |
| 集成测试 | 8-10 小时 | ★★★☆☆ | 高 |

**总计：38-54 小时**

## 成功标准

### 功能完整性
- [ ] 所有 TAB 列表增强功能正常工作
- [ ] 配置界面可以正常打开和操作
- [ ] 自定义功能完全可用
- [ ] 权限系统正确工作

### 性能标准
- [ ] 客户端渲染性能无明显下降
- [ ] 网络通信延迟在可接受范围内
- [ ] 内存使用量合理

### 兼容性标准
- [ ] 与原版 Minecraft 1.20.1 兼容
- [ ] 与主流模组包兼容
- [ ] 支持单人和多人游戏模式

## 后续维护建议

1. **版本管理**：建议使用语义化版本号，如 `1.0.0-forge1.20.1`
2. **文档更新**：及时更新用户文档和开发文档
3. **社区反馈**：建立反馈渠道，及时处理用户问题
4. **持续集成**：建立自动化测试和构建流程

---

*本文档基于 `porting-guide-forge-1.20.1.md` 理论指南编写，提供具体的实施步骤和注意事项。在实际移植过程中，请根据具体情况调整实施计划。*
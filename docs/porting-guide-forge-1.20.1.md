# NeoTab: NeoForge 1.21.1 移植到 Forge 1.20.1 详细说明文档

## 项目概述

NeoTab 是一个功能丰富的 TAB 列表增强模组，当前版本基于 NeoForge 1.21.1。移植到 Forge 1.20.1 需要处理多个层面的 API 变更和版本差异。

## 移植难度评估：★★★★☆ (困难)

**主要挑战：**
1. NeoForge → Forge 的 API 差异较大
2. Minecraft 1.21.1 → 1.20.1 的版本回退
3. 网络系统完全重构
4. 事件系统和权限系统变更
5. Mixin 目标类可能有变化

## 详细移植清单

### 1. 构建系统重构 (★★★★★)

#### build.gradle 完全重写
```gradle
// 当前 NeoForge 配置
plugins {
    id 'net.neoforged.moddev' version '2.0.141'
}

neoForge {
    version = project.neo_version
    // ...
}

// 需要改为 Forge 配置
plugins {
    id 'net.minecraftforge.gradle' version '5.1.+'
}

minecraft {
    mappings channel: 'official', version: '1.20.1'
    
    runs {
        client {
            workingDirectory project.file('run')
            // Forge 特定配置
        }
    }
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0' // 或最新版本
}
```

#### gradle.properties 更新
```properties
# 版本降级
minecraft_version=1.20.1
minecraft_version_range=[1.20.1]
forge_version=47.2.0
loader_version_range=[47,)

# 映射表更新
parchment_minecraft_version=1.20.1
parchment_mappings_version=2023.06.26
```

### 2. 模组元数据文件重构 (★★★☆☆)

#### mods.toml 重写
```toml
# 从 neoforge.mods.toml 改为 mods.toml
modLoader="javafml"
loaderVersion="[47,)"

[[mods]]
modId="neotab"
version="1.0.0-forge"
displayName="NeoTab"

[[dependencies.neotab]]
modId="forge"
type="required"
versionRange="[47.2.0,)"
ordering="NONE"
side="BOTH"

[[dependencies.neotab]]
modId="minecraft"
type="required"
versionRange="[1.20.1]"
ordering="NONE"
side="BOTH"
```

### 3. 主入口类重构 (★★★☆☆)

#### NeoTab.java 主要变更
```java
// 当前 NeoForge 代码
@Mod(NeoTab.MODID)
public class NeoTab {
    public NeoTab(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoTabPayloads::register);
    }
}

// 需要改为 Forge 代码
@Mod(NeoTab.MODID)
public class NeoTab {
    public NeoTab() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // 网络包注册移到这里
        event.enqueueWork(() -> {
            NeoTabPayloads.register();
        });
    }
}
```

#### NeoTabClient.java 重构
```java
// 当前 NeoForge 代码
@Mod(value = NeoTab.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)

// 需要改为 Forge 代码
@Mod.EventBusSubscriber(modid = NeoTab.MODID, value = Dist.CLIENT)
public class NeoTabClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 客户端初始化
    }
}
```

### 4. 网络系统完全重构 (★★★★★)

这是最复杂的部分，需要完全重写。

#### 当前 NeoForge 网络系统
```java
// NeoTabPayloads.java - 当前实现
public static void register(RegisterPayloadHandlersEvent event) {
    var registrar = event.registrar("2");
    
    registrar.playToClient(
        OpenConfigScreenPayload.TYPE,
        OpenConfigScreenPayload.STREAM_CODEC,
        OpenConfigScreenPayload::handle);
}

// Payload 类 - 当前实现
public record OpenConfigScreenPayload(TabConfig config) implements CustomPacketPayload {
    public static final Type<OpenConfigScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "open_config_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigScreenPayload> STREAM_CODEC = // ...
}
```

#### 需要改为 Forge 网络系统
```java
// 新的网络管理器
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
        
        INSTANCE.registerMessage(id++, OpenConfigScreenPacket.class,
            OpenConfigScreenPacket::encode,
            OpenConfigScreenPacket::decode,
            OpenConfigScreenPacket::handle);
            
        INSTANCE.registerMessage(id++, SyncTabConfigPacket.class,
            SyncTabConfigPacket::encode,
            SyncTabConfigPacket::decode,
            SyncTabConfigPacket::handle);
            
        // 注册所有其他包...
    }
}

// 包类重写
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
            // 处理逻辑
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new NeoTabConfigScreen(minecraft.screen, packet.config));
        });
        ctx.get().setPacketHandled(true);
    }
}
```

#### 所有网络包需要重写
需要重写的包类：
- `OpenConfigScreenPayload` → `OpenConfigScreenPacket`
- `OpenCustomizeScreenPayload` → `OpenCustomizeScreenPacket`
- `SaveConfigPayload` → `SaveConfigPacket`
- `SavePlayerConfigPayload` → `SavePlayerConfigPacket`
- `SyncTabConfigPayload` → `SyncTabConfigPacket`
- `SyncCustomizePolicyPayload` → `SyncCustomizePolicyPacket`
- `SyncOnlineDurationsPayload` → `SyncOnlineDurationsPacket`
- `SyncPlayerHealthPayload` → `SyncPlayerHealthPacket`

#### 发送包的方式变更
```java
// 当前 NeoForge 方式
PacketDistributor.sendToPlayer(player, new SyncTabConfigPayload(config));
PacketDistributor.sendToAllPlayers(new SyncTabConfigPayload(config));

// 需要改为 Forge 方式
NeoTabNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncTabConfigPacket(config));
NeoTabNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncTabConfigPacket(config));
```

### 5. 事件系统重构 (★★★☆☆)

#### 事件注册方式变更
```java
// 当前 NeoForge 事件
@EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 处理逻辑
    }
}

// 需要改为 Forge 事件
@Mod.EventBusSubscriber(modid = NeoTab.MODID)
public final class NeoTabServerEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 处理逻辑
        }
    }
}
```

#### 主要事件类变更
- `ServerTickEvent.Post` → `TickEvent.ServerTickEvent`
- `ClientTickEvent.Post` → `TickEvent.ClientTickEvent`
- `ClientPlayerNetworkEvent.LoggingOut` → `ClientPlayerNetworkEvent.LoggedOutEvent`
- `RegisterPayloadHandlersEvent` → 移除，改用网络注册
- `PermissionGatherEvent.Nodes` → `PermissionGatherEvent.Nodes`

### 6. 权限系统适配 (★★★☆☆)

#### 权限节点创建方式变更
```java
// 当前 NeoForge 权限
public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
    ResourceLocation.fromNamespaceAndPath(NeoTab.MODID, "configure"),
    PermissionTypes.BOOLEAN,
    (player, uuid, contexts) -> player != null && player.hasPermissions(2)
);

// 需要改为 Forge 权限
public static final PermissionNode<Boolean> CONFIGURE = new PermissionNode<>(
    NeoTab.MODID, "configure", PermissionTypes.BOOLEAN,
    (player, uuid, contexts) -> player != null && player.hasPermissions(2)
);
```

### 7. Mixin 系统适配 (★★☆☆☆)

#### Mixin 配置文件更新
```json
// neotab.mixins.json 可能需要调整目标类
{
  "required": true,
  "package": "com.poso.neotab.mixin",
  "compatibilityLevel": "JAVA_17", // 从 JAVA_21 降级到 JAVA_17
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

#### Mixin 目标类可能的变更
需要检查 1.20.1 中的类结构：
- `PlayerTabOverlay` 类的方法签名
- `Gui` 类的 `renderTabList` 方法
- `MultiLineEditBox` 相关类

### 8. 资源和语言文件 (★☆☆☆☆)

基本不需要变更，但需要检查：
- 语言键是否在 1.20.1 中存在
- 纹理资源路径是否正确

### 9. API 兼容性检查 (★★★☆☆)

#### Minecraft API 变更检查
需要检查以下 API 在 1.20.1 中的可用性：
- `Component.translatable()` 方法
- `ServerPlayer` 相关方法
- `GuiGraphics` 渲染方法
- `Font` 字体渲染 API

#### 可能需要适配的 API
```java
// 检查这些方法在 1.20.1 中是否存在
ResourceLocation.fromNamespaceAndPath() // 可能需要改为 new ResourceLocation()
player.sendSystemMessage() // 检查方法签名
guiGraphics.drawString() // 检查参数
```

### 10. 依赖库检查 (★★☆☆☆)

检查项目使用的第三方库：
- Mixin 版本兼容性
- 其他可能的依赖库

## 移植步骤建议

### 第一阶段：基础框架移植
1. 创建新的 Forge 1.20.1 项目
2. 更新 `build.gradle` 和 `gradle.properties`
3. 重写 `mods.toml`
4. 移植主入口类

### 第二阶段：网络系统重构
1. 创建新的网络管理器
2. 重写所有网络包类
3. 更新所有发送包的代码

### 第三阶段：事件和权限系统
1. 适配事件系统
2. 更新权限节点创建
3. 测试权限功能

### 第四阶段：Mixin 和 UI
1. 检查 Mixin 目标类
2. 适配客户端 UI 代码
3. 测试渲染功能

### 第五阶段：测试和优化
1. 功能测试
2. 性能测试
3. 兼容性测试

## 预估工作量

- **总工作量：** 约 40-60 小时
- **核心开发：** 30-45 小时
- **测试调试：** 10-15 小时

## 风险评估

### 高风险项
1. **网络系统重构** - 可能遇到序列化兼容性问题
2. **Mixin 适配** - 目标类结构可能有变化
3. **API 兼容性** - 某些 1.21.1 的 API 在 1.20.1 中可能不存在

### 中风险项
1. **事件系统** - 事件名称和参数可能有变化
2. **权限系统** - Forge 权限 API 可能有差异

### 低风险项
1. **业务逻辑** - 核心功能逻辑基本不需要变更
2. **配置系统** - JSON 序列化部分基本兼容
3. **UI 渲染** - 大部分渲染代码可以复用

## 建议

1. **优先级排序：** 建议先完成网络系统重构，这是最复杂的部分
2. **版本选择：** 考虑是否真的需要回退到 1.20.1，或者可以选择更新的 Forge 版本
3. **测试策略：** 建议在每个阶段完成后进行充分测试
4. **代码复用：** 大部分业务逻辑代码可以直接复用，重点关注框架层面的适配

总的来说，这是一个中等偏难的移植项目，主要挑战在于网络系统的完全重构和 API 适配。建议分阶段进行，确保每个阶段的稳定性后再进行下一阶段。

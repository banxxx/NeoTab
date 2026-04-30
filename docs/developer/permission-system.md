# NeoTab 权限控制系统 — 详细实施方案

## 架构概览

```
权限判断优先级（从高到低）：
1. 玩家 OP 等级 ≥ 2 → 完全自由，跳过所有限制（兼容单人世界）
2. 玩家专属策略（UUID 级别）→ 覆盖全局策略
3. 全局策略 AND 权限节点 → 两者都允许才开放
```

### 设计原则

- **服务端是权威**：所有权限判断在服务端完成，客户端只负责展示。
- **null 即继承**：玩家个人配置中 `null` 表示"跟随服务器设置"，管理员修改服务器配置时未设置个人配置的玩家自动跟随。
- **双重校验**：客户端发来的配置包在服务端必须重新校验，防止绕过。
- **单人世界兼容**：`player.hasPermissions(2)` 在单人世界对房主返回 `true`，天然兼容，无需特殊处理。

---

## 受限 UI 视觉规范

受限组件**保留显示但禁用操作**，不隐藏：

| 状态 | 视觉表现 |
|------|---------|
| 可操作 | 正常颜色，可点击 |
| 策略锁定 | 灰色（`AbstractWidget.active = false` 自动处理），悬停显示 tooltip |
| 权限节点不足 | 同上，tooltip 文案不同 |

Tooltip 文案：
- 策略锁定：`"此选项由服务器管理员控制"`
- 权限节点不足：`"您没有修改此选项的权限"`

---

## 任务清单

### 阶段一：数据结构

- [x] **Task 1.1** — 新建 `PlayerCustomizePolicy`
- [x] **Task 1.2** — 扩展 `TabConfig` 加入策略字段
- [x] **Task 1.3** — 新建 `PlayerTabConfig`
- [x] **Task 1.4** — 新建 `PlayerTabConfigRepository`

### 阶段二：权限判断

- [x] **Task 2.1** — 扩展 `NeoTabPermissions`

### 阶段三：服务层

- [x] **Task 3.1** — 扩展 `NeoTabService`

### 阶段四：网络层

- [x] **Task 4.1** — 新增 `SyncCustomizePolicyPayload`
- [x] **Task 4.2** — 新增 `SavePlayerConfigPayload`
- [x] **Task 4.3** — 新增 `/neotab customize` 命令
- [x] **Task 4.4** — 注册新网络包

### 阶段五：客户端 UI

- [x] **Task 5.1** — 扩展 `NeoTabClientState`
- [x] **Task 5.2** — 改造 `NeoTabConfigScreen` 支持受限模式
- [x] **Task 5.3** — 受限 UI 视觉规范（随 5.2 一起实现）

### 阶段六：管理员界面

- [x] **Task 6.1** — 管理员配置界面新增策略面板

### 阶段七：收尾

- [x] **Task 7.1** — 注册新权限节点
- [x] **Task 7.2** — 添加语言文件条目
- [x] **Task 7.3** — 更新 README

---

## 阶段一：数据结构设计

### Task 1.1 — 新建 `PlayerCustomizePolicy`

**文件**：`src/main/java/com/poso/neotab/permission/PlayerCustomizePolicy.java`

细粒度策略字段，对照配置界面分区：

| 字段 | 控制范围 |
|------|---------|
| `allowTopTitleToggle` | 顶部标题 开关 |
| `allowTopTitleEdit` | 顶部标题 文本内容 |
| `allowTopContentToggle` | 顶部内容 开关 |
| `allowTopContentEdit` | 顶部内容 文本内容 |
| `allowPingDisplayToggle` | 更好的延迟显示 开关 |
| `allowDurationToggle` | 在线时长显示 开关 |
| `allowTitleToggle` | 称号功能 开关 |
| `allowHealthDisplayToggle` | 血量显示 开关 |
| `allowHealthModeChange` | 血量显示模式（心/数字/百分比） |
| `allowFooterCustomEdit` | 底部自定义文字 |
| `allowFooterTpsToggle` | 底部 TPS 开关 |
| `allowFooterMsptToggle` | 底部 MSPT 开关 |
| `allowFooterOnlineToggle` | 底部在线人数 开关 |
| `allowThemeChange` | 主题切换 |
| `allowRefreshIntervalChange` | 刷新间隔调整 |

提供两个静态工厂方法：
- `locked()` — 全部字段为 `false`，服务器默认值
- `unlocked()` — 全部字段为 `true`，管理员使用

---

### Task 1.2 — 扩展 `TabConfig`

**文件**：`src/main/java/com/poso/neotab/config/TabConfig.java`

在 record 中新增两个字段：

- `globalPolicy` — 全局玩家策略，类型 `PlayerCustomizePolicy`，默认 `locked()`
- `playerPolicies` — 个人专属策略，类型 `Map<UUID, PlayerCustomizePolicy>`，默认空 Map

同步更新：`defaults()`、`sanitized()`、`toJson()`、`fromJson()`、`write()`、`read()`

---

### Task 1.3 — 新建 `PlayerTabConfig`

**文件**：`src/main/java/com/poso/neotab/config/PlayerTabConfig.java`

存储玩家个人偏好，所有字段均为可空（`@Nullable`），`null` 表示跟随服务器设置。

字段与 `TabConfig` 中的可自定义字段一一对应（排除策略字段本身）。

提供 `mergeInto(TabConfig server, PlayerCustomizePolicy policy)` 方法，将个人配置叠加到服务器配置上，生成最终有效配置。合并规则：策略允许 AND 个人有设置 → 用个人值，否则用服务器值。

---

### Task 1.4 — 新建 `PlayerTabConfigRepository`

**文件**：`src/main/java/com/poso/neotab/config/PlayerTabConfigRepository.java`

存储路径：`<世界>/playerdata/neotab/<uuid>.json`

提供 `load(MinecraftServer, UUID)` 和 `save(MinecraftServer, PlayerTabConfig)` 方法。

---

## 阶段二：权限判断逻辑

### Task 2.1 — 扩展 `NeoTabPermissions`

**文件**：`src/main/java/com/poso/neotab/permission/NeoTabPermissions.java`

**新增权限节点**（与 `PlayerCustomizePolicy` 字段一一对应）：

| 权限节点 | 默认值 | 说明 |
|---------|--------|------|
| `neotab:customize` | `true` | 总开关，允许打开个人自定义界面 |
| `neotab:customize.top_title.toggle` | `false` | 顶部标题开关 |
| `neotab:customize.top_title.edit` | `false` | 顶部标题内容 |
| `neotab:customize.top_content.toggle` | `false` | 顶部内容开关 |
| `neotab:customize.top_content.edit` | `false` | 顶部内容文字 |
| `neotab:customize.ping` | `false` | 延迟显示开关 |
| `neotab:customize.duration` | `false` | 在线时长开关 |
| `neotab:customize.title` | `false` | 称号功能开关 |
| `neotab:customize.health.toggle` | `false` | 血量显示开关 |
| `neotab:customize.health.mode` | `false` | 血量显示模式 |
| `neotab:customize.footer.custom` | `false` | 底部自定义文字 |
| `neotab:customize.footer.tps` | `false` | 底部 TPS 开关 |
| `neotab:customize.footer.mspt` | `false` | 底部 MSPT 开关 |
| `neotab:customize.footer.online` | `false` | 底部在线人数开关 |
| `neotab:customize.theme` | `false` | 主题切换 |

**新增核心方法** `resolvePolicy(ServerPlayer, TabConfig)`：

1. 玩家 OP ≥ 2 → 直接返回 `unlocked()`
2. 查找 `playerPolicies` 中是否有该玩家的专属策略 → 有则直接返回
3. 取全局策略每个字段 AND 对应权限节点 → 返回合并结果
4. 没有权限管理模组时，权限节点回退为 `false`，全局策略单独生效

---

## 阶段三：服务层扩展

### Task 3.1 — 扩展 `NeoTabService`

**文件**：`src/main/java/com/poso/neotab/service/NeoTabService.java`

新增内容：

- 玩家个人配置缓存：`Map<UUID, PlayerTabConfig>`（`ConcurrentHashMap`）
- `PlayerTabConfigRepository` 实例
- `getEffectiveConfig(ServerPlayer)` — 调用 `resolvePolicy` + `mergeInto` 生成有效配置
- `savePlayerConfig(ServerPlayer, PlayerTabConfig)` — 服务端二次校验后保存并重新同步
- 修改 `onPlayerJoined()` — 加载个人配置，同步有效配置和策略
- 修改 `onPlayerLeft()` — 清理缓存
- 修改 `reload()` — 清空个人配置缓存

---

## 阶段四：网络层扩展

### Task 4.1 — 新增 `SyncCustomizePolicyPayload`

**文件**：`src/main/java/com/poso/neotab/network/payload/SyncCustomizePolicyPayload.java`

- 方向：服务端 → 客户端
- 携带：已经过 `resolvePolicy` 计算的有效策略（`PlayerCustomizePolicy`）
- 客户端处理：存入 `NeoTabClientState.currentPolicy`

---

### Task 4.2 — 新增 `SavePlayerConfigPayload`

**文件**：`src/main/java/com/poso/neotab/network/payload/SavePlayerConfigPayload.java`

- 方向：客户端 → 服务端
- 携带：玩家修改后的个人配置（`PlayerTabConfig`）
- 服务端处理：调用 `NeoTabService.savePlayerConfig()`，内部进行二次校验

---

### Task 4.3 — 新增 `/neotab customize` 命令

**文件**：`src/main/java/com/poso/neotab/command/NeoTabCommands.java`

- 权限要求：`neotab:customize`（默认所有玩家可用）
- 执行效果：服务端向玩家发送 `OpenCustomizeScreenPayload`，携带当前个人配置和有效策略
- 仅限玩家执行，控制台不可用

---

### Task 4.4 — 注册新网络包

**文件**：`src/main/java/com/poso/neotab/network/NeoTabPayloads.java`

注册以下新增包：
- `SyncCustomizePolicyPayload`（服务端 → 客户端）
- `SavePlayerConfigPayload`（客户端 → 服务端）
- `OpenCustomizeScreenPayload`（服务端 → 客户端，复用现有模式新增）

---

## 阶段五：客户端 UI

### Task 5.1 — 扩展 `NeoTabClientState`

**文件**：`src/main/java/com/poso/neotab/client/NeoTabClientState.java`

新增字段：
- `currentPolicy`：当前玩家的有效策略，默认 `locked()`
- `personalConfig`：当前玩家的个人配置，默认 `null`

`reset()` 中同步清空这两个字段。

---

### Task 5.2 — 改造 `NeoTabConfigScreen` 支持受限模式

**文件**：`src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java`

**新增 `ScreenMode` 枚举**：

| 模式 | 说明 |
|------|------|
| `ADMIN` | 管理员模式，完整配置 + 策略设置面板 |
| `PLAYER` | 玩家模式，个人自定义，受策略约束 |

**改动要点**：

1. 构造函数新增 `ScreenMode` 和 `PlayerCustomizePolicy` 参数
2. 每个组件创建后根据策略设置 `active` 状态
3. 受限组件添加 tooltip 说明原因
4. 保存按钮行为区分：`ADMIN` 发 `SaveConfigPayload`，`PLAYER` 发 `SavePlayerConfigPayload`
5. `ADMIN` 模式下在 PAGE_CONFIG 标签页末尾新增策略配置分区（见 Task 6.1）

---

### Task 5.3 — 受限 UI 视觉规范

随 Task 5.2 一起实现，无需单独文件。

规范见本文档顶部"受限 UI 视觉规范"章节。

---

## 阶段六：管理员界面扩展

### Task 6.1 — 管理员配置界面新增策略面板

**文件**：`src/main/java/com/poso/neotab/client/screen/NeoTabConfigScreen.java`（ADMIN 模式）

在 PAGE_CONFIG 标签页末尾新增"玩家自定义权限"分区，布局如下：

```
┌─ 玩家自定义权限 ──────────────────────────────────┐
│  全局策略                                          │
│  顶部标题开关    [开] [关]                          │
│  顶部标题内容    [开] [关]                          │
│  顶部内容开关    [开] [关]                          │
│  顶部内容文字    [开] [关]                          │
│  延迟显示        [开] [关]                          │
│  在线时长        [开] [关]                          │
│  称号功能        [开] [关]                          │
│  血量显示开关    [开] [关]                          │
│  血量显示模式    [开] [关]                          │
│  底部自定义文字  [开] [关]                          │
│  底部 TPS        [开] [关]                          │
│  底部 MSPT       [开] [关]                          │
│  底部在线人数    [开] [关]                          │
│  主题切换        [开] [关]                          │
│                                                    │
│  个人专属策略                                       │
│  玩家名: [____________] [查找]                      │
│  （找到玩家后展开同样的开关列表）                    │
└────────────────────────────────────────────────────┘
```

---

## 阶段七：收尾

### Task 7.1 — 注册新权限节点

**文件**：`src/main/java/com/poso/neotab/event/NeoTabServerEvents.java`

在 `onPermissionNodesGathered()` 中注册 Task 2.1 中新增的所有权限节点。

---

### Task 7.2 — 添加语言文件条目

**文件**：
- `src/main/resources/assets/neotab/lang/zh_cn.json`
- `src/main/resources/assets/neotab/lang/en_us.json`

新增条目：

| Key | 中文值 |
|-----|--------|
| `screen.neotab.locked_by_server` | 此选项由服务器管理员控制 |
| `screen.neotab.no_permission` | 您没有修改此选项的权限 |
| `screen.neotab.section.player_policy` | 玩家自定义权限 |
| `screen.neotab.policy.global` | 全局策略 |
| `screen.neotab.policy.personal` | 个人专属策略 |
| `command.neotab.customize_opened` | 已打开个人自定义界面 |
| `command.neotab.customize_no_permission` | 您没有使用个人自定义功能的权限 |

---

### Task 7.3 — 更新 README

**文件**：`README.md`

更新"权限系统"章节，补充：
- 所有新增权限节点及其默认值
- 玩家自定义功能说明（`/neotab customize` 命令）
- 管理员策略配置说明
- 个人专属策略说明

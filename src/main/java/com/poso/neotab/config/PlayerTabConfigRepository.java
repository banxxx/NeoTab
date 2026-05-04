package com.poso.neotab.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poso.neotab.NeoTab;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 玩家个人配置持久化仓库。
 *
 * <p>文件位置：{@code <世界>/playerdata/neotab/<uuid>.json}</p>
 *
 * <p>职责单一：只负责读写，不参与业务逻辑。</p>
 */
public final class PlayerTabConfigRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * 加载指定玩家的个人配置。
     * 如果文件不存在，返回全部跟随服务器的默认配置（不写入文件）。
     */
    public PlayerTabConfig load(MinecraftServer server, UUID playerId) {
        Path path = resolve(server, playerId);
        if (!Files.exists(path)) {
            return PlayerTabConfig.defaults(playerId);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return PlayerTabConfig.fromJson(playerId, json);
        } catch (Exception exception) {
            NeoTab.LOGGER.error("Failed to load player tab config for {} from {}", playerId, path, exception);
            return PlayerTabConfig.defaults(playerId);
        }
    }

    /**
     * 保存玩家个人配置到磁盘。
     * 如果配置全部为 null（即全部跟随服务器），则删除文件以节省空间。
     */
    public void save(MinecraftServer server, PlayerTabConfig config) {
        Path path = resolve(server, config.playerId());

        // 如果是全默认配置，删除文件即可
        if (isAllDefaults(config)) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                NeoTab.LOGGER.error("Failed to delete player tab config for {}", config.playerId(), exception);
            }
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config.toJson(), writer);
            }
        } catch (IOException exception) {
            NeoTab.LOGGER.error("Failed to save player tab config for {} to {}", config.playerId(), path, exception);
        }
    }

    /**
     * 删除指定玩家的个人配置文件。
     */
    public void delete(MinecraftServer server, UUID playerId) {
        Path path = resolve(server, playerId);
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            NeoTab.LOGGER.error("Failed to delete player tab config for {}", playerId, exception);
        }
    }

    /** 解析配置文件实际路径。 */
    private Path resolve(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(LevelResource.ROOT)
            .resolve("playerdata")
            .resolve("neotab")
            .resolve(playerId + ".json");
    }

    /** 判断配置是否全部为 null（即全部跟随服务器）。 */
    private boolean isAllDefaults(PlayerTabConfig config) {
        return config.topTitleEnabled()      == null
            && config.topTitleText()         == null
            && config.topContentEnabled()    == null
            && config.topContentText()       == null
            && config.betterPingEnabled()    == null
            && config.onlineDurationEnabled() == null
            && config.titleEnabled()         == null
            && config.healthDisplayEnabled() == null
            && config.healthDisplayMode()    == null
            && config.footerCustomText()     == null
            && config.footerTpsEnabled()     == null
            && config.footerMsptEnabled()    == null
            && config.footerOnlineEnabled()  == null
            && config.tabTheme()             == null;
    }
}
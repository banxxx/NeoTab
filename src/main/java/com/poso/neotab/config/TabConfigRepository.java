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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 配置仓库。
 *
 * <p>它的职责非常单一：负责配置文件的读写，不参与业务逻辑。</p>
 *
 * <p>文件位置：世界目录 / serverconfig / neotab-tab.json</p>
 */
public final class TabConfigRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "neotab-tab.json";

    /**
     * 读取配置；如果文件不存在，则写入一份默认配置并返回。
     */
    public TabConfig load(MinecraftServer server) {
        Path path = resolve(server);
        if (!Files.exists(path)) {
            TabConfig defaults = TabConfig.defaults();
            save(server, defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return TabConfig.fromJson(json);
        } catch (Exception exception) {
            NeoTab.LOGGER.error("Failed to load NeoTab config from {}", path, exception);
            return TabConfig.defaults();
        }
    }

    /**
     * 保存配置到磁盘。
     */
    public void save(MinecraftServer server, TabConfig config) {
        Path path = resolve(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config.sanitized().toJson(), writer);
            }
        } catch (IOException exception) {
            NeoTab.LOGGER.error("Failed to save NeoTab config to {}", path, exception);
        }
    }

    /**
     * 解析配置文件实际路径。
     */
    private Path resolve(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
    }
}
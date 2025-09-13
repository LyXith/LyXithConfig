package org.lyxith.lyxithconfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class LyxithConfig implements ModInitializer {
    public static final String modId = "LyXithConfig";
    public static final Logger logger = Logger.getLogger(modId);
    private static final Path minecraftPath = FabricLoader.getInstance().getGameDir();
    public static final Path configPath = minecraftPath.resolve("LyXithConfig");
    @Override
    public void onInitialize() {
        createConfigDir();
    }
    public void createConfigDir() {
        if (!Files.exists(configPath) && !Files.isDirectory(configPath)) {
            try {
                // 创建配置目录（如果不存在）
                Files.createDirectories(configPath);
                logger.info("配置目录创建成功: " + configPath);
            } catch (IOException e) {
                logger.warning("无法创建配置目录: " + e.getMessage());
            }
        }
    }
}

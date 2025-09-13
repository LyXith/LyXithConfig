package org.lyxith.lyxithconfig.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.lyxith.lyxithconfig.LyxithConfig.configPath;
import static org.lyxith.lyxithconfig.LyxithConfig.logger;
public class LyXithConfigAPIImpl implements LyXithConfigAPI{
    private final Map<String, LyXithConfigNode> configs = new ConcurrentHashMap<>();

    @Override
    public Path getConfigRootPath() {
        return configPath;
    }

    @Override
    public boolean modConfigDirExist(String modId) {
        return Files.exists(configPath.resolve(modId));
    }

    @Override
    public boolean modConfigExist(String modId, String configName) {
        Path modConfigPath = configPath.resolve(modId).resolve(configName);
        return Files.exists(modConfigPath) && !Files.isDirectory(modConfigPath);
    }

    @Override
    public void createModConfigDir(String modConfigPath) {
        Path modConfigDir = configPath.resolve(modConfigPath);
        if (!Files.exists(modConfigDir) && !Files.isDirectory(modConfigDir)) {
            try {
                Files.createDirectories(modConfigDir);
            } catch (IOException e) {
                logger.warning("无法创建配置目录: " + e.getMessage());
            }
        }
    }
    @Override
    public void createModConfig(String modConfigPath, String modConfigFile) {
        Path modConfigDir = configPath.resolve(modConfigPath);
        // 确保文件扩展名是.json
        String fileName = modConfigFile.endsWith(".json") ? modConfigFile : modConfigFile + ".json";
        Path modConfigFilePath = modConfigDir.resolve(fileName);

        try {
            // 确保目录存在
            Files.createDirectories(modConfigDir);

            // 如果文件不存在，则创建空文件
            if (!Files.exists(modConfigFilePath)) {
                Files.writeString(modConfigFilePath,"{}");
                logger.info("配置文件创建成功: " + modConfigFilePath);
            }
            // 如果文件已存在，不做任何操作

        } catch (IOException e) {
            logger.warning("无法创建配置文件 '" + modConfigFilePath + "': " + e.getMessage());
        }
    }

    @Override
    public LyXithConfigNode getConfigRootNode(String modId) {
        return getConfigRootNode(modId,"config");
    }

    @Override
    public LyXithConfigNode getConfigRootNode(String modId, String configName) {
        Path modConfigFile = configPath.resolve(modId).resolve(configName + ".json");
        if (Files.exists(modConfigFile) && !Files.isDirectory(modConfigFile)) {
            LyXithConfigNode loadedNode = loadConfigFromJson(modConfigFile);
            if (loadedNode != null) {
                // 转换为实现类并获取根节点
                return loadedNode.getRoot();
            }
        }
        return null;
    }

    @Override
    public void saveConfig(String modId) {
        saveConfig(modId,"config");
    }
    @Override
    public void saveConfig(String modId, String configName) {
        Path path = configPath.resolve(modId).resolve(configName + ".json");
        saveConfigToFile(configs.get(modId+"_"+configName), path);
    }

    @Override
    public void saveConfig(String modId, String configName, LyXithConfigNode configNode) {
        configs.put(modId+"_"+configName,configNode);
        saveConfig(modId, configName);
    }

    @Override
    public void loadConfig(String modId) {
        loadConfig(modId,"config");
    }
    @Override
    public void loadConfig(String modId, String configName) {
        Path path = configPath.resolve(modId).resolve(configName + ".json");
        LyXithConfigNode configNode = loadConfigFromJson(path);
        configs.put(modId+"_"+configName,configNode);
    }

    @Override
    public void setValue(String modId, String nodePath, Object value) {
        setValue(modId, "config",nodePath,value);
    }
    @Override
    public void setValue(String modId, String configName, String nodePath, Object value) {
        LyXithConfigNode configNode = configs.get(modId+"_"+configName);
        Optional<LyXithConfigNodeImpl> selNode = configNode.getNode(nodePath);
        selNode.ifPresent(lyXithConfigNode -> lyXithConfigNode.setValue(value));
        if (selNode.isEmpty()) {
            logger.warning("ConfigNode doesn't exist.");
        }
    }

    @Override
    public  <T> Optional<T> getValue(String modId, String nodePath, Class<T> type) {
        return getValue(modId, "config",nodePath,type);
    }

    @Override
    public  <T> Optional<T> getValue(String modId, String configName, String nodePath, Class<T> type) {
        LyXithConfigNode configNode = configs.get(modId+"_"+configName);
        Optional<LyXithConfigNodeImpl> selNode = configNode.getNode(nodePath);
        return selNode.flatMap(node -> node.getValue(type));
    }
    private LyXithConfigNode loadConfigFromJson(Path configPath) {
        LyXithConfigNode instance = new LyXithConfigNodeImpl();
        try {
            String config = Files.readString(configPath);
            return instance.fromString(config);
        } catch (IOException e) {
            logger.warning("Can't find config:" + configPath + "error" + e.getMessage());
        }
        return new LyXithConfigNodeImpl();
    }

    private void saveConfigToFile(LyXithConfigNode configNode, Path configPath) {
        try {
            Files.writeString(configPath,configNode.toString());
        } catch (IOException e) {
            logger.warning("Save config to:" + configPath + "error:" + e.getMessage());
        }
    }
}

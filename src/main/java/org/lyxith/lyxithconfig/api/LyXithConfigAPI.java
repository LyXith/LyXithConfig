package org.lyxith.lyxithconfig.api;

import java.nio.file.Path;
import java.util.Optional;

public interface LyXithConfigAPI {
    Path getConfigRootPath();

    boolean modConfigDirExist(String modId);

    boolean modConfigExist(String modId, String configName);

    void createModConfigDir(String modConfigPath);

    void createModConfig(String modConfigPath, String modConfigFile);

    LyXithConfigNode getConfigRootNode(String modId);

    LyXithConfigNode getConfigRootNode(String modId, String configName);

    void saveConfig(String modId);

    void saveConfig(String modId, String configName);

    void loadConfig(String modId);

    void loadConfig(String modId, String configName);

    void saveConfig(String modId, String configName, LyXithConfigNode configNode);

    // 便捷方法
    void setValue(String modId, String nodePath, Object value);

    void setValue(String modId, String configName, String nodePath, Object value);

    <T> Optional<T> getValue(String modId, String nodePath, Class<T> type);

    <T> Optional<T> getValue(String modId, String configName, String nodePath, Class<T> type);
}

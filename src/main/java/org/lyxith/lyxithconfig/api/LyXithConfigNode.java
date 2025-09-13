package org.lyxith.lyxithconfig.api;

import java.util.Optional;

public interface LyXithConfigNode {
    // 基本操作
    String getPath();
    void addNode(String path);
    void delNode(String path);

    // 值操作
    boolean hasValue();
    void setValue(Object value);
    <T> Optional<T> getValue(Class<T> type);

    // 列表操作
    int length();


    // 序列化方法
    String toString();
    LyXithConfigNode fromString(String jsonString);

    // 便捷方法
    default Optional<String> getString() {
        return getValue(String.class);
    }

    default Optional<Integer> getInt() {
        return getValue(Integer.class);
    }

    default Optional<Boolean> getBoolean() {
        return getValue(Boolean.class);
    }

    default Optional<Double> getDouble() {
        return getValue(Double.class);
    }

    default void set(String value) {
        setValue(value);
    }

    default void set(int value) {
        setValue(value);
    }

    default void set(boolean value) {
        setValue(value);
    }

    default void set(double value) {
        setValue(value);
    }

    // 辅助方法：根据路径获取节点
    Optional<LyXithConfigNodeImpl> getNode(String path);

    LyXithConfigNodeImpl getRoot();

    void addNode(String path, Boolean Overwrite);

    void addElement(Object element);

    void delElement(int index);

    void setElement(Object element, int index);

    Object getElement(int index);
}

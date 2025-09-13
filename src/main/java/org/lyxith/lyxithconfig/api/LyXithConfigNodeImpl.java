package org.lyxith.lyxithconfig.api;

import com.google.gson.*;

import java.util.*;

public class LyXithConfigNodeImpl implements LyXithConfigNode {
    private final LyXithConfigNodeImpl parent;
    private final String name;
    private Object value;
    private final Map<String, LyXithConfigNodeImpl> children = new HashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting() // 美化输出，便于阅读
            .serializeNulls()    // 序列化null值
            .create();

    // 构造方法：用于创建根节点
    public LyXithConfigNodeImpl() {
        this.parent = null;
        this.name = "";
    }

    // 构造方法：用于创建子节点
    public LyXithConfigNodeImpl(LyXithConfigNodeImpl parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    // 序列化当前节点为JSON字符串
    @Override
    public String toString() {
        return GSON.toJson(toJsonObject());
    }

    // 从JSON字符串反序列化为配置节点
    public LyXithConfigNode fromString(String jsonString) {
        JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
        return fromJsonObject(jsonObject, null, "");
    }

    // 将节点转换为JsonObject用于序列化
    private JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        if (hasValue()) {
            // 如果节点有值，根据节点名称决定序列化方式
            if (name.isEmpty()) {
                // 对于匿名节点（如根节点的值），使用空键名
                switch (value) {
                    case String s -> jsonObject.addProperty("", s);
                    case Number number -> jsonObject.addProperty("", number);
                    case Boolean b -> jsonObject.addProperty("", b);
                    case null -> jsonObject.add("", JsonNull.INSTANCE);
                    default -> jsonObject.add("", GSON.toJsonTree(value));
                }
            } else {
                // 对于有名节点，使用节点名称作为键名
                switch (value) {
                    case String s -> jsonObject.addProperty(name, s);
                    case Number number -> jsonObject.addProperty(name, number);
                    case Boolean b -> jsonObject.addProperty(name, b);
                    case null -> jsonObject.add(name, JsonNull.INSTANCE);
                    default -> jsonObject.add(name, GSON.toJsonTree(value));
                }
            }
        } else {
            // 如果节点有子节点，递归序列化子节点
            for (Map.Entry<String, LyXithConfigNodeImpl> entry : children.entrySet()) {
                jsonObject.add(entry.getKey(), entry.getValue().toJsonObject());
            }
        }
        return jsonObject;
    }

    // 从JsonObject反序列化节点
    private static LyXithConfigNodeImpl fromJsonObject(JsonObject jsonObject, LyXithConfigNodeImpl parent, String name) {
        LyXithConfigNodeImpl node = new LyXithConfigNodeImpl(parent, name);

        // 处理空JSON对象 {}
        if (jsonObject == null || jsonObject.entrySet().isEmpty()) {
            return node; // 返回空的容器节点
        }

        // 检查是否是值节点（包含空键名）
        boolean hasValue = jsonObject.has("");

        if (hasValue) {
            // 处理有值的节点
            JsonElement valueElement = jsonObject.get("");
            if (valueElement.isJsonPrimitive()) {
                // 处理基本数据类型...
                if (valueElement.getAsJsonPrimitive().isString()) {
                    node.setValue(valueElement.getAsString());
                } else if (valueElement.getAsJsonPrimitive().isNumber()) {
                    String numberStr = valueElement.getAsString();
                    if (numberStr.contains(".")) {
                        node.setValue(valueElement.getAsDouble());
                    } else {
                        try {
                            node.setValue(valueElement.getAsInt());
                        } catch (NumberFormatException e) {
                            node.setValue(valueElement.getAsLong());
                        }
                    }
                } else if (valueElement.getAsJsonPrimitive().isBoolean()) {
                    node.setValue(valueElement.getAsBoolean());
                }
            } else if (valueElement.isJsonNull()) {
                node.setValue(null);
            } else {
                // 处理复杂对象或数组
                node.setValue(GSON.fromJson(valueElement, Object.class));
            }
        } else {
            // 处理有子节点的节点 - 所有其他键都是子节点
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    LyXithConfigNodeImpl childNode = fromJsonObject(
                            entry.getValue().getAsJsonObject(), node, entry.getKey());
                    node.children.put(entry.getKey(), childNode);
                }
            }
        }

        return node;
    }

    @Override
    public boolean hasValue() {
        return value != null && children.isEmpty();
    }

    @Override
    public String getPath() {
        Deque<String> pathStack = new ArrayDeque<>();
        LyXithConfigNodeImpl currentNode = this;

        while (currentNode != null) {
            if (!currentNode.name.isEmpty()) {
                pathStack.push(currentNode.name);
            }
            currentNode = currentNode.parent;
        }

        return String.join(".", pathStack);
    }

    @Override
    public void addNode(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String[] pathParts = path.split("\\.");
        LyXithConfigNodeImpl currentNode = this;

        for (String part : pathParts) {
            currentNode.children.putIfAbsent(part, new LyXithConfigNodeImpl(currentNode, part));
            currentNode = currentNode.children.get(part);
        }
    }

    @Override
    public void delNode(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        String[] pathParts = path.split("\\.");
        LyXithConfigNodeImpl currentNode = this;

        for (int i = 0; i < pathParts.length - 1; i++) {
            currentNode = currentNode.children.get(pathParts[i]);
            if (currentNode == null) {
                return;
            }
        }

        String targetNodeName = pathParts[pathParts.length - 1];
        currentNode.children.remove(targetNodeName);
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
        // 设置值时清空子节点，确保hasValue()逻辑正确
        this.children.clear();
    }

    @Override
    public <T> Optional<T> getValue(Class<T> type) {
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    // 辅助方法：根据路径获取节点
    @Override
    public Optional<LyXithConfigNodeImpl> getNode(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.of(this);
        }

        String[] pathParts = path.split("\\.");
        LyXithConfigNodeImpl currentNode = this;

        for (String part : pathParts) {
            currentNode = currentNode.children.get(part);
            if (currentNode == null) {
                return Optional.empty();
            }
        }
        return Optional.of(currentNode);
    }

    // Getter方法，用于序列化和测试
    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, LyXithConfigNodeImpl> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    @Override
    public LyXithConfigNodeImpl getRoot() {
        LyXithConfigNodeImpl currentNode = this;
        while (currentNode.parent != null) {
            currentNode = currentNode.parent;
        }
        return currentNode;
    }

    @Override
    public void addNode(String path, Boolean Overwrite) {
        if (!Overwrite && getNode(path).isEmpty()) {
            addNode(path);
        }
    }
}
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
            // 如果节点有值，直接序列化值到当前对象
            if (value instanceof List<?> list) {
                // 处理列表节点
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    switch (item) {
                        case String s -> jsonObject.addProperty(String.valueOf(i), s);
                        case Number number -> jsonObject.addProperty(String.valueOf(i), number);
                        case Boolean b -> jsonObject.addProperty(String.valueOf(i), b);
                        case null -> jsonObject.add(String.valueOf(i), JsonNull.INSTANCE);
                        default -> jsonObject.add(String.valueOf(i), GSON.toJsonTree(item));
                    }
                }
            } else {
                // 处理单个值节点
                switch (value) {
                    case String s -> jsonObject.addProperty("0", s);
                    case Number number -> jsonObject.addProperty("0", number);
                    case Boolean b -> jsonObject.addProperty("0", b);
                    case null -> jsonObject.add("0", JsonNull.INSTANCE);
                    default -> jsonObject.add("0", GSON.toJsonTree(value));
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

        if (jsonObject == null || jsonObject.entrySet().isEmpty()) {
            return node;
        }

        // 检查是否是值节点：所有键都必须是纯数字
        boolean isValueNode = true;
        boolean hasNumericKey = false;

        for (String key : jsonObject.keySet()) {
            if (key.matches("\\d+")) {
                hasNumericKey = true;
            } else {
                isValueNode = false;
                break;
            }
        }

        isValueNode = isValueNode && hasNumericKey;

        if (isValueNode) {
            // 处理列表节点
            List<Object> list = new ArrayList<>();

            // 按数字键顺序收集所有值
            List<String> numericKeys = new ArrayList<>(jsonObject.keySet());
            numericKeys.sort(Comparator.comparingInt(Integer::parseInt));

            for (String key : numericKeys) {
                JsonElement valueElement = jsonObject.get(key);
                if (valueElement.isJsonPrimitive()) {
                    if (valueElement.getAsJsonPrimitive().isString()) {
                        list.add(valueElement.getAsString());
                    } else if (valueElement.getAsJsonPrimitive().isNumber()) {
                        String numberStr = valueElement.getAsString();
                        if (numberStr.contains(".")) {
                            list.add(valueElement.getAsDouble());
                        } else {
                            try {
                                list.add(valueElement.getAsInt());
                            } catch (NumberFormatException e) {
                                list.add(valueElement.getAsLong());
                            }
                        }
                    } else if (valueElement.getAsJsonPrimitive().isBoolean()) {
                        list.add(valueElement.getAsBoolean());
                    }
                } else if (valueElement.isJsonNull()) {
                    list.add(null);
                } else {
                    list.add(GSON.fromJson(valueElement, Object.class));
                }
            }
            node.setValue(list);
        } else {
            // 处理有子节点的节点
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
        if (value == null) {
            return Optional.empty();
        }

        // 如果是列表，返回第一个元素
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object firstElement = list.getFirst();
            if (type.isInstance(firstElement)) {
                return Optional.of(type.cast(firstElement));
            }
            return Optional.empty();
        }

        // 如果是单个值
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

    //列表操作

    @Override
    public int length() {
        if (hasValue()) {
            // 值节点：如果是列表，返回列表大小；如果是单个值，返回1
            if (value instanceof List<?> list) {
                return list.size();
            } else {
                return 1;
            }
        } else {
            // 容器节点：返回子节点数量
            return children.size();
        }
    }

    @Override
    public void addElement(Object element) {
        if (hasValue()) {
            if (value instanceof List<?>) {
                // 安全地转换为List<Object>
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                list.add(element);
            } else {
                // 将单个值转换为列表
                List<Object> newList = new ArrayList<>();
                newList.add(value);
                newList.add(element);
                setValue(newList);
            }
        } else {
            setValue(element);
        }
    }

    @Override
    public void delElement(int index) {
        if(hasValue() && value instanceof List<?> list) {
            list.remove(index);
        }
    }

    @Override
    public void setElement(Object element, int index) {
        if(hasValue() && value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            list.set(index,element);
        }
    }

    @Override
    public Object getElement(int index) {
        if(hasValue() && value instanceof List<?> list) {
            return list.get(index);
        }
        return null;
    }
}
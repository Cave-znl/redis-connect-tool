package com.caven.redistool.entity;

public class RedisKey {
    private final String fullKeyName;   // 完整的 Key 名称, e.g., "USER:1"
    private final String displayPart;   // 用于在 TreeView 中显示的部分, e.g., "USER" 或 "1"
    private final String keyType;       // "string", "hash", or "folder" for virtual nodes
    private final boolean isLeaf;       // true 代表是真实 key, false 代表是虚拟父节点

    public RedisKey(String fullKeyName, String displayPart, String keyType, boolean isLeaf) {
        this.fullKeyName = fullKeyName;
        this.displayPart = displayPart;
        this.keyType = keyType;
        this.isLeaf = isLeaf;
    }

    public String getFullKeyName() {
        return fullKeyName;
    }

    public String getDisplayPart() {
        return displayPart;
    }

    public String getKeyType() {
        return keyType;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    @Override
    public String toString() {
        // This will be used by the CellFactory for display
        if ("folder".equalsIgnoreCase(keyType)) {
            return displayPart;
        }
        return String.format("[%s] %s", keyType.toUpperCase(), displayPart);
    }
}
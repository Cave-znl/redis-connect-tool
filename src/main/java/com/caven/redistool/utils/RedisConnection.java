package com.caven.redistool.utils;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RedisConnection {
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisAsyncCommands<String, String> async;

    // ... 您已有的 connect, getAsync, disconnect 等方法保持不变 ...
    public void connect(String host, int port, String password) {
        // 构建 URI：如果有密码，使用 "redis://default:password@host:port"
        // 无密码： "redis://host:port"
        String username = "default";
        String uri;
        if (password != null && !password.isEmpty()) {
            uri = String.format("redis://%s:%s@%s:%d", username, password, host, port);
        } else {
            uri = String.format("redis://%s:%d", host, port);
        }
        client = RedisClient.create(uri);
        connection = client.connect();
        async = connection.async();
    }

    /**
     * 异步获取 String 类型 key 的值
     */
    public CompletableFuture<String> getAsync(String key) {
        return async.get(key).toCompletableFuture();
    }

    /**
     * 异步分页扫描 Hash 的字段和值
     */
    public CompletableFuture<MapScanCursor<String, String>> hscanAsync(String key, ScanCursor cursor, long count) {
        return async.hscan(key, cursor, ScanArgs.Builder.limit(count)).toCompletableFuture();
    }

    /**
     * 异步删除 Hash 中的一个或多个字段
     */
    public CompletableFuture<Long> hdelAsync(String key, String... fields) {
        return async.hdel(key, fields).toCompletableFuture();
    }

    /**
     * 异步分页获取 List 的元素
     */
    public CompletableFuture<List<String>> lrangeAsync(String key, long start, long stop) {
        return async.lrange(key, start, stop).toCompletableFuture();
    }

    public RedisAsyncCommands<String, String> getAsyncCommands() {
        return async;
    }

    public void disconnect() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }


    /**
     * [新增] 使用 SCAN 命令异步分页扫描 keys
     * @param cursor 扫描的游标, 初始应为 ScanCursor.INITIAL
     * @param pattern 匹配的 key 模式
     * @param count 每次扫描的数量
     * @return 包含 keys 列表和下一个游标的 CompletableFuture
     */
    public CompletableFuture<KeyScanCursor<String>> scanAsync(ScanCursor cursor, String pattern, long count) {
        if (async == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Redis connection is not initialized."));
        }
        ScanArgs scanArgs = ScanArgs.Builder.limit(count).match(pattern);
        return async.scan(cursor, scanArgs).toCompletableFuture();
    }

    /**
     * 异步获取指定 key 的数据类型
     * @param key a key
     * @return a CompletableFuture with the key type (e.g., "string", "list", "hash")
     */
    public CompletableFuture<String> typeAsync(String key) {
        if (async == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Redis connection is not initialized."));
        }
        return async.type(key).toCompletableFuture();
    }

    /**
     * 异步删除一个或多个 keys
     * @param keys 要删除的 key 列表
     * @return a CompletableFuture with the number of keys that were removed.
     */
    public CompletableFuture<Long> delAsync(String... keys) {
        if (async == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Redis connection is not initialized."));
        }
        return async.del(keys).toCompletableFuture();
    }
}
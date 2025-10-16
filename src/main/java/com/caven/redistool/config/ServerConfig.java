package com.caven.redistool.config;

/**
 *
 * @author : WangXiYao
 * @date 2025/10/16 17:02
 */
public class ServerConfig {
    private String host;
    private int port;
    private String encryptedPassword;  // 加密后的密码

    public ServerConfig(String host, int port, String encryptedPassword) {
        this.host = host;
        this.port = port;
        this.encryptedPassword = encryptedPassword;
    }

    // Getters and Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @Override
    public String toString() {
        return host + ":" + port;  // ListView 显示格式
    }
}

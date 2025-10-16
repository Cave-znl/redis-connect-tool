# Redis Connect Tools

**Redis Connect Tools** 是一个使用 JavaFX 构建的、简洁而强大的 Redis® 数据库图形用户界面（GUI）。它提供了一个直观的方式来连接、浏览和管理您的 Redis 服务器中的数据。

![应用主界面截图](https://i.imgur.com/uTmoXP0.png)


## ✨ 主要功能

*   **多服务器管理**: 保存多个 Redis 服务器的连接配置，方便快速切换。
*   **分层级 Key 视图**: 自动将使用 `:` 分隔的 Key（如 `user:1`, `user:2`）组织成可折叠的树状结构，使浏览大量 Key 变得轻而易举。
*   **安全高效的 Key 浏览**: 使用 `SCAN` 命令进行分页加载 Key，避免因 Key 数量过多而阻塞服务器。支持“加载更多”功能。
*   **按类型查看内容**: 在右侧面板中，可以直观地查看 String、List、Hash 等不同类型 Key 的内容。
*   **内容分页加载**: 对于 List 和 Hash 等大型数据结构，其内部元素也支持分页加载，轻松处理海量数据。
*   **强大的搜索功能**: 支持使用 `*` 等通配符模式来搜索和过滤 Key。
*   **数据管理操作**:
    *   支持多选并批量删除 Key。
    *   支持刷新 Key 列表和独立的 Key 内容刷新。
    *   支持删除 Hash 中的特定字段。
*   **响应式布局**: 采用可拖动的 `SplitPane` 布局，用户可以自由调整 Key 列表和内容视图的宽度。

## 🛠️ 技术栈

*   **语言**: Java 17+
*   **UI 框架**: JavaFX 21
*   **Redis 客户端**: Lettuce Core
*   **JSON 解析**: Google Gson
*   **日志**: SLF4J
*   **构建工具**: Apache Maven

## 🚀 快速开始

### 先决条件

在开始之前，请确保您的开发环境中已安装以下软件：
1.  **JDK (Java Development Kit)**: 版本 17 或更高版本。
2.  **Apache Maven**: 用于项目构建和依赖管理。
3.  **Redis Server**: 一个正在运行且可以从本地访问的 Redis 服务器实例。

### 安装与运行

1.  **克隆仓库**
    ```bash
    git clone https://github.com/your-username/redis-connect-tools.git
    cd redis-connect-tools
    ```
    <!-- 请将上面的 URL 替换为您自己的 Git 仓库地址 -->

2.  **使用 Maven 构建项目**
    在项目根目录下运行以下命令，Maven 将会自动下载所有依赖并打包成一个 JAR 文件。
    ```bash
    mvn clean package
    ```
    构建成功后，您会在 `target` 目录下找到 `redis-connect-tools-1.0.0.jar` (版本号可能不同)。

3.  **运行应用程序**
    由于本项目使用了 Java 模块化系统 (JPMS)，需要使用特定的命令来启动。请在项目**根目录**下运行：

    ```bash
    java --module-path "C:\path\to\your\javafx-jmods-21" --add-modules javafx.controls,javafx.fxml -jar target/redis-connect-tools-1.0.0.jar
    ```
    **请注意替换以下部分**:
    *   `C:\path\to\your\javafx-jmods-21`: 替换为您在本地存放 JavaFX JMods 文件的**真实路径**。您可以从 [GluonHQ 官网](https://gluonhq.com/products/javafx/) 下载对应您操作系统和 JDK 版本的 JavaFX SDK。
    *   `redis-connect-tools-1.0.0.jar`: 确保 JAR 文件名与 `target` 目录下的文件名一致。

## 📖 使用指南

1.  **添加新连接**: 启动软件后，点击左上角的 `+` 按钮，在弹出的对话框中输入 Redis 服务器的主机、端口和密码（如有）。
2.  **连接服务器**: 在左侧列表中，**双击**您想要连接的服务器配置。
3.  **浏览 Keys**: 连接成功后，左侧将以树状结构展示该服务器上的所有 Key。
    *   使用顶部的**搜索框**和**搜索按钮**来过滤 Key。
    *   点击**刷新按钮**以重新加载 Key 列表。
    *   点击**加载更多按钮**以获取下一批 Key。
4.  **查看内容**: 在左侧 Key 列表中，**双击**一个具体的 Key（非文件夹节点），其内容将在右侧面板中显示。
5.  **管理内容**:
    *   使用右侧面板顶部的**刷新按钮**来重新加载当前 Key 的内容。
    *   对于 Hash 或 List 类型，如果内容过多，可以点击底部的**加载更多内容按钮**。
    *   在 Hash 视图中，选中一个字段后，可以点击**删除选中项按钮**来删除该字段。
6.  **删除 Keys**: 在左侧 Key 列表中，可以按住 `Ctrl` (Windows/Linux) 或 `Cmd` (macOS) 进行多选，然后点击**删除 Key 按钮**来批量删除。

## 🗺️ 未来规划 (Roadmap)

*   [ ] 直接在 UI 中编辑 Key 的值。
*   [ ] 支持新增 Key、以及在 Hash/List 中新增元素。
*   [ ] 支持更多 Redis 数据类型 (Set, ZSet, Stream)。
*   [ ] 提供一个简单的命令行界面 (CLI) 或终端窗口。
*   [ ] 连接状态指示和服务器信息展示面板。

## 📄 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT) 许可证。

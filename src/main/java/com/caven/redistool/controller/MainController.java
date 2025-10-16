package com.caven.redistool.controller;

import com.caven.redistool.config.ServerConfig;
import com.caven.redistool.entity.HashEntry;
import com.caven.redistool.entity.RedisKey;
import com.caven.redistool.utils.EncryptionUtil;
import com.caven.redistool.utils.RedisConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanCursor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // --- 左侧 FXML ---
    @FXML private ListView<ServerConfig> serverListView;
    @FXML private SplitPane mainSplitPane;
    @FXML private TreeView<RedisKey> keyTreeView;
    @FXML private TextField searchField;
    @FXML private Button loadMoreButton;
    @FXML private VBox welcomeCenter;
    @FXML private VBox redisKeyView;

    // --- 右侧 FXML ---
    @FXML private VBox keyContentView;
    @FXML private Label welcomeLabel;
    @FXML private TextArea stringContentView;
    @FXML private ListView<String> listContentView;
    @FXML private TableView<HashEntry> hashContentView;
    @FXML private Button contentLoadMoreButton;

    // --- 业务逻辑属性 ---
    // --- 业务逻辑 ---
    private RedisConnection redisConn;
    private RedisKey activeKey; // 当前在右侧显示内容的Key
    private ScanCursor scanCursor; // 左侧Key列表的游标
    private ScanCursor contentScanCursor; // 右侧内容(Hash)的游标
    private long listContentOffset; // 右侧List内容的偏移量
    private static final int SCAN_COUNT = 20;
    private ObservableList<ServerConfig> serverConfigs = FXCollections.observableArrayList();
    private static final String DATA_DIR = "data";
    private static final String CONFIG_FILE = DATA_DIR + "/servers.json";
    private final Gson gson = new Gson();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateUI(false);
        setupKeyTreeView();
        keyContentView.setVisible(false); // 初始隐藏右侧面板

        // 加载配置
        loadServerConfigs();
        serverListView.setItems(serverConfigs);
        updateServerListUI();

        // 双击连接
        serverListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ServerConfig selected = serverListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    connectToServer(selected);
                }
            }
        });
    }

    /**
     * 设置 TreeView 的单元格工厂和多选模式
     */
    private void setupKeyTreeView() {
        keyTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // [修改] 双击事件，只对叶子节点响应
        keyTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<RedisKey> item = keyTreeView.getSelectionModel().getSelectedItem();
                // 核心改动：只有当节点是叶子节点 (isLeaf() == true) 时才加载内容
                if (item != null && item.getValue() != null && item.getValue().isLeaf()) {
                    loadKeyContent(item.getValue());
                }
            }
        });

        // [修改] CellFactory 使用新的 RedisKey 属性
        keyTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(RedisKey item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 直接使用 RedisKey 的 toString() 方法，它已经包含了我们想要的逻辑
                    setText(item.toString());
                }
            }
        });
    }

    private void connectToServer(ServerConfig config) {
        try {
            String password = EncryptionUtil.decrypt(config.getEncryptedPassword());
            // 如果已有连接，先断开
            if (redisConn != null) {
                redisConn.disconnect();
            }
            redisConn = new RedisConnection();
            redisConn.connect(config.getHost(), config.getPort(), password);
            log.info("连接成功到 " + config.getHost() + ":" + config.getPort() + "！\n");


            // 切换服务器时先隐藏右侧,再初始化 Key 视图
            updateUI(true);
            initializeKeyView();
        } catch (Exception e) {
            log.error("连接失败: ", e);
            // 可以在此弹窗提示用户连接失败
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("连接错误");
                alert.setHeaderText("无法连接到 Redis 服务器");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
            updateUI(false); // 失败则切回欢迎页
        }
    }

    /**
     * 加载 Key 的内容
     * @param key 要加载的 Key
     */
    private void loadKeyContent(RedisKey key) {
        this.activeKey = key;
        keyContentView.setVisible(true); // 显示右侧面板
        switch (key.getKeyType().toLowerCase()) {
            case "string":
                loadStringContent();
                break;
            case "list":
                loadListContent(false);
                break;
            case "hash":
                loadHashContent(false);
                break;
            default:
                showContentPlaceholder("不支持的类型: " + key.getKeyType());
                break;
        }
    }

    private void showContentPlaceholder(String message) {
        Stream.of(stringContentView, listContentView, hashContentView).forEach(node -> node.setVisible(false));
        welcomeLabel.setText(message);
        welcomeLabel.setVisible(true);
        contentLoadMoreButton.setVisible(false);
    }

    private void setVisibleContentNode(Node node) {
        welcomeLabel.setVisible(false);
        stringContentView.setVisible(node == stringContentView);
        listContentView.setVisible(node == listContentView);
        hashContentView.setVisible(node == hashContentView);
    }

    private void loadStringContent() {
        setVisibleContentNode(stringContentView);
        contentLoadMoreButton.setVisible(false);
        redisConn.getAsync(activeKey.getFullKeyName()).thenAcceptAsync(value ->
                Platform.runLater(() -> stringContentView.setText(value)), Platform::runLater);
    }

    private void loadListContent(boolean isLoadMore) {
        setVisibleContentNode(listContentView);
        if (!isLoadMore) {
            listContentView.getItems().clear();
            listContentOffset = 0;
        }

        long start = listContentOffset;
        long end = start + SCAN_COUNT - 1;

        redisConn.lrangeAsync(activeKey.getFullKeyName(), start, end).thenAcceptAsync(values -> {
            Platform.runLater(() -> {
                listContentView.getItems().addAll(values);
                listContentOffset += values.size();
                contentLoadMoreButton.setVisible(!values.isEmpty() && values.size() == SCAN_COUNT);
            });
        }, Platform::runLater);
    }

    private void loadHashContent(boolean isLoadMore) {
        setVisibleContentNode(hashContentView);
        if (!isLoadMore) {
            hashContentView.getItems().clear();
            contentScanCursor = ScanCursor.INITIAL;
        }

        if (contentScanCursor == null || contentScanCursor.isFinished()) {
            contentLoadMoreButton.setVisible(false);
            return;
        }

        redisConn.hscanAsync(activeKey.getFullKeyName(), contentScanCursor, SCAN_COUNT)
                .thenAcceptAsync(cursor -> {
                    contentScanCursor = cursor;
                    Platform.runLater(() -> {
                        for (Map.Entry<String, String> entry : cursor.getMap().entrySet()) {
                            hashContentView.getItems().add(new HashEntry(entry.getKey(), entry.getValue()));
                        }
                        contentLoadMoreButton.setVisible(!cursor.isFinished());
                    });
                }, Platform::runLater);
    }

    /**
     * 初始化 Key 视图，并加载第一批数据
     */
    private void initializeKeyView() {
        updateUI(true);
        searchField.setText(""); // 清空搜索框

        TreeItem<RedisKey> rootItem = new TreeItem<>(); // 使用我们新的数据模型
        keyTreeView.setRoot(rootItem);
        keyTreeView.setShowRoot(false);

        scanCursor = ScanCursor.INITIAL;
        loadMoreButton.setDisable(false);
        loadKeys(false); // 初始加载
    }

    /**
     * loadKeys 方法现在能处理追加和清空加载
     * @param isLoadMore true 表示追加加载, false 表示清空后重新加载
     */
    private void loadKeys(boolean isLoadMore) {
        if (redisConn == null || scanCursor == null || scanCursor.isFinished()) {
            return;
        }

        String pattern = searchField.getText().trim();
        if (pattern.isEmpty()) {
            pattern = "*"; // 默认为 *
        }

        // 异步扫描 keys
        redisConn.scanAsync(scanCursor, pattern, SCAN_COUNT).thenComposeAsync(keyScanCursor -> {
            scanCursor = keyScanCursor;
            List<String> keys = keyScanCursor.getKeys();

            if (keys.isEmpty()) {
                return CompletableFuture.completedFuture(new ArrayList<RedisKey>());
            }

            List<CompletableFuture<RedisKey>> typeFutures = keys.stream()
                    .map(key -> redisConn.typeAsync(key)
                            // 注意：这里只创建了叶子节点的模型，displayPart 暂时用 full name
                            .thenApply(type -> new RedisKey(key, key, type, true)))
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(typeFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> typeFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));

        }).thenAcceptAsync(leafKeys -> {
            // 在 JavaFX 线程更新 UI
            Platform.runLater(() -> {
                TreeItem<RedisKey> root = keyTreeView.getRoot();
                if (!isLoadMore) {
                    root.getChildren().clear(); // 如果不是追加，先清空
                }

                // [核心改动] 使用新的方法来构建树
                for (RedisKey leafKey : leafKeys) {
                    insertKeyIntoTree(root, leafKey);
                }

                if (scanCursor.isFinished()) {
                    loadMoreButton.setDisable(true);
                }
            });
        }).exceptionally(ex -> {
            log.error("扫描 keys 失败: ", ex);
            // ... (错误处理) ...
            return null;
        });
    }

    @FXML
    private void handleLoadMoreKeys() {
        loadKeys(true); // 追加加载
    }

    /**
     * 更新中心区域的UI显示
     * @param connected true 显示 key 视图, false 显示欢迎页
     */
    private void updateUI(boolean connected) {
        welcomeCenter.setVisible(!connected);
        mainSplitPane.setVisible(connected);
    }

    // --- 您已有的、无需修改的方法 ---

    private void loadServerConfigs() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type listType = new TypeToken<ArrayList<ServerConfig>>() {}.getType();
                List<ServerConfig> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    serverConfigs.addAll(loaded);
                }
            } catch (Exception e) {
                log.error("加载配置失败: ", e);
                serverConfigs.clear();
            }
        }
    }

    private void saveServerConfigs() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(serverConfigs, writer);
        } catch (Exception e) {
            log.error("保存配置失败: ", e);
        }
    }

    private void updateServerListUI() {
        serverListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getHost() + ":" + item.getPort());
            }
        });
    }

    @FXML
    private void handleAddServer() {
        Stage dialog = new Stage();
        dialog.setTitle("新建连接");
        VBox dialogVBox = new VBox(10);
        dialogVBox.setPadding(new Insets(10));
        TextField hostInput = new TextField();
        hostInput.setPromptText("主机");
        TextField portInput = new TextField();
        portInput.setPromptText("端口");
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("密码");
        HBox buttonBox = new HBox(10);
        Button okButton = new Button("确定");
        Button cancelButton = new Button("取消");
        buttonBox.getChildren().addAll(okButton, cancelButton);
        dialogVBox.getChildren().addAll(new Label("主机:"), hostInput, new Label("端口:"), portInput, new Label("密码:"), passwordInput, buttonBox);
        Scene dialogScene = new Scene(dialogVBox, 300, 200);
        dialog.setScene(dialogScene);
        okButton.setOnAction(event -> {
            try {
                String host = hostInput.getText().trim();
                int port = Integer.parseInt(portInput.getText().trim());
                String password = passwordInput.getText().trim();
                String encryptedPw = EncryptionUtil.encrypt(password);
                ServerConfig newConfig = new ServerConfig(host, port, encryptedPw);
                serverConfigs.add(newConfig);
                saveServerConfigs();
                connectToServer(newConfig);
                dialog.close();
            } catch (Exception e) {
                log.error("新建连接失败: ", e);
            }
        });
        cancelButton.setOnAction(event -> dialog.close());
        dialog.show();
    }

    /**
     * 处理刷新按钮点击事件
     * 它的作用是清空当前的 TreeView 并从头开始重新加载 keys
     */
    @FXML
    private void handleRefreshKeys() {
        keyContentView.setVisible(false);
        initializeKeyView();
    }

    /**
     * 处理搜索按钮点击事件
     */
    @FXML
    private void handleSearchKeys() {
        if (keyTreeView.getRoot() == null) return;
        log.info("按模式 '{}' 搜索 keys...", searchField.getText());
        scanCursor = ScanCursor.INITIAL; // 重置游标从头开始搜索
        loadMoreButton.setDisable(false); // 重新启用加载按钮
        loadKeys(false); // 清空并加载
    }

    /**
     * 处理删除按钮点击事件
     */
    @FXML
    private void handleDeleteKeys() {
        ObservableList<TreeItem<RedisKey>> selectedItems = keyTreeView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            log.warn("没有选择任何 key 进行删除。");
            return;
        }

        // 确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("您确定要删除选中的 " + selectedItems.size() + " 个 key 吗？");
        alert.setContentText("此操作不可恢复！");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String[] keysToDelete = selectedItems.stream()
                    .map(item -> item.getValue().getFullKeyName())
                    .toArray(String[]::new);

            // 异步删除
            redisConn.delAsync(keysToDelete).thenAcceptAsync(deletedCount -> {
                log.info("成功删除了 {} 个 keys。", deletedCount);
                // 在 UI 线程移除对应节点
                Platform.runLater(() -> keyTreeView.getRoot().getChildren().removeAll(selectedItems));
            }).exceptionally(ex -> {
                log.error("删除 keys 失败: ", ex);
                // ... (错误处理) ...
                return null;
            });
        }
    }


    // --- 右侧按钮事件 ---
    @FXML
    private void handleContentRefresh() {
        if (activeKey != null) loadKeyContent(activeKey);
    }

    @FXML
    private void handleContentLoadMore() {
        if (activeKey == null) return;
        String type = activeKey.getKeyType().toLowerCase();
        if ("list".equals(type)) {
            loadListContent(true);
        } else if ("hash".equals(type)) {
            loadHashContent(true);
        }
    }

    @FXML
    private void handleContentDelete() {
        if (activeKey == null) return;
        String type = activeKey.getKeyType().toLowerCase();

        if ("hash".equals(type)) {
            HashEntry selected = hashContentView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // ... (此处可加确认对话框) ...
                redisConn.hdelAsync(activeKey.getFullKeyName(), selected.getField()).thenAccept(res -> {
                    if (res > 0) Platform.runLater(() -> hashContentView.getItems().remove(selected));
                });
            }
        }
        // ... 此处可扩展 List, Set 等类型的删除逻辑 (e.g., LREM, SREM) ...
    }

    /**
     * [新增] 核心方法：将一个完整的 key 插入到 TreeView 的正确位置
     * @param root TreeView 的根节点
     * @param leafKey 代表一个真实 Redis Key 的对象
     */
    private void insertKeyIntoTree(TreeItem<RedisKey> root, RedisKey leafKey) {
        String fullKeyName = leafKey.getFullKeyName();
        String[] parts = fullKeyName.split(":");

        // 如果 key 不含 ':', 直接加到 root 下
        if (parts.length <= 1) {
            root.getChildren().add(new TreeItem<>(leafKey));
            return;
        }

        TreeItem<RedisKey> currentNode = root;
        String currentPath = "";

        // 遍历所有父路径部分 (除了最后一部分)
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            currentPath = currentPath.isEmpty() ? part : currentPath + ":" + part;

            // 查找是否已存在该父节点
            TreeItem<RedisKey> foundNode = null;
            for (TreeItem<RedisKey> child : currentNode.getChildren()) {
                if (child.getValue().getDisplayPart().equals(part)) {
                    foundNode = child;
                    break;
                }
            }

            // 如果不存在，创建一个新的虚拟父节点
            if (foundNode == null) {
                // 父节点也带上类型，这样显示为 "[HASH] USER"
                RedisKey folderKey = new RedisKey(currentPath, part, leafKey.getKeyType(), false);
                TreeItem<RedisKey> newFolderNode = new TreeItem<>(folderKey);
                currentNode.getChildren().add(newFolderNode);
                currentNode = newFolderNode;
            } else {
                currentNode = foundNode;
            }
        }

        // 添加最后的叶子节点
        String lastPart = parts[parts.length - 1];
        RedisKey finalLeafKey = new RedisKey(fullKeyName, lastPart, leafKey.getKeyType(), true);
        currentNode.getChildren().add(new TreeItem<>(finalLeafKey));
    }

}
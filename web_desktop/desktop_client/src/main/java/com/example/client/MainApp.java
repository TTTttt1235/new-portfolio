package com.example.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;
//import java.util.function.ToDoubleBiFunction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;

public class MainApp extends Application {
    private Label statusLabel = new Label("🖱 情報待機中...");

    private TrayIcon trayIcon;
    Dotenv dotenv = Dotenv.load(); 
    String wsUrl = dotenv.get("WS_SERVER_URL");

    @Override
    public void start(Stage stage) throws Exception {



        // JavaFXウィンドウ作成
        StackPane root = new StackPane();
        //root.getChildren().add(new Label("マウス共有クライアント（常駐版）"));
        root.getChildren().add(statusLabel);
        Scene scene = new Scene(root, 400, 300);

        stage.setTitle("Mouse Sharing Client");
        stage.setScene(scene);

        try {//WebSocket接続用のコンテナ（管理役）を取得、WebSocket接続を作ったり、コントロールできる特別なオブジェクト
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            //取得したコンテナを使ってサーバーに接続する
            //container.connectToServer(MouseClient.class, new URI("ws://localhost:8080/ws/mouse"));
            //container.connectToServer(MouseClient.class, new URI("ws://192.168.xx.xxx:8080/ws/mouse"));//ToDo
            container.connectToServer(MouseClient.class, new URI(wsUrl));//ToDowin
            System.out.println("✅ 接続先: " + wsUrl);

        } catch (Exception e) {
            System.err.println("❌ 接続エラー: " + e.getMessage());
        }

        // マウス情報を0.5秒ごとに更新
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(0.5),
                e -> updateCursorInfo()
            )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();

        // トレイアイコンセットアップ
        setupSystemTray(stage);

        // ウィンドウを閉じようとしたら最小化
        stage.setOnCloseRequest(event -> {
            event.consume(); // デフォルトの終了処理をキャンセル
            hideToSystemTray(stage);
        });

        stage.show();
    }

    private void setupSystemTray(Stage stage) {
        //システムトレイが使えない場合スキップ
        if (!SystemTray.isSupported()) {
            System.out.println("❌ システムトレイ未対応");
            return;
        }
    
        SystemTray tray = SystemTray.getSystemTray();
        try {
            Image image = ImageIO.read(getClass().getResourceAsStream("/trayicon.png"));
    
            ActionListener exitListener = e -> {
                System.out.println("🚪 アプリ終了");
                System.exit(0);
            };
    
            //終了ボタン付きポップアップメニュー
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("終了");
            exitItem.addActionListener(exitListener);
            popup.add(exitItem);
    
            trayIcon = new TrayIcon(image, "Mouse Sharing Client", popup);
            trayIcon.setImageAutoSize(true);
    
            tray.add(trayIcon);
    
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }

    //ウィンドウを隠しトレイから通知を出す
    private void hideToSystemTray(Stage stage) {
        if (trayIcon != null) {
            stage.hide();
            trayIcon.displayMessage("最小化されました", "ここから終了できます", TrayIcon.MessageType.INFO);
        }
    }    

    public static void main(String[] args) {
        launch();//start()が呼ばれる
    }

    private void updateCursorInfo() {
    PointerInfo pointerInfo = MouseInfo.getPointerInfo();
    Point location = pointerInfo.getLocation();
    int x = (int) location.getX();
    int y = (int) location.getY();

    String screenInfo = "未検出";
    GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    for (GraphicsDevice screen : screens) {
        Rectangle bounds = screen.getDefaultConfiguration().getBounds();
        if (bounds.contains(x, y)) {
            screenInfo = screen.getIDstring() + " / " + bounds;
            break;
        }
    }

    statusLabel.setText(String.format("🖱 座標: (%d, %d)\n🖥 画面: %s", x, y, screenInfo));
}


    
}

/*
 * JavaFXアプリケーションの起動 ＋ WebSocketサーバーへの接続
 * launch() 
 * _JavaFX内部で、必要なリソース（UIスレッド、イベントキューなど）を用意
 * _MainApp クラスが new
 * _start(Stage stage) を呼び出す
 * getWebSocketContainer()
 * _実装に依存しないで、WebSocketコンテナを探す
 * 
 */
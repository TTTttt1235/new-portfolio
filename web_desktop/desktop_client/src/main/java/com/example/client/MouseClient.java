package com.example.client;

import jakarta.websocket.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.awt.*;
//import java.awt.event.InputEvent;
import java.util.Map;

/*デバッグ用
    System.out.printf("🧪 仮想座標: (%d, %d), 現在スクリーン: %s [%d ~ %d]\n",
    currentX, currentY, currentScreen.id, currentScreen.x, currentScreen.x + currentScreen.width);
 */

@ClientEndpoint
public class MouseClient {
    private long lastWarpTime = 0;
    private static final long WARP_COOLDOWN_MS = 500;// 0.5秒クールダウン
    private int currentX = 3000;// 初期仮想座標
    private int currentY = 2500;
    private VirtualLayoutManager layoutManager;// 仮想レイアウトマネージャー
    private static final ObjectMapper objectMapper = new ObjectMapper();//ObjectMapper:Jacksonライブラリ,JSONを変換
    private boolean suppressWarpCheck = false;


    @OnOpen
    public void onOpen(Session session) {
        System.out.println("✅ WebSocket 接続成功");
        SessionManager.add(session);
        try {
            String registerMessage = objectMapper.writeValueAsString(Map.of(
                "type", "register",
                "from", "PC-B",
                "to", "PC-A",
                "dummy", true
            ));
            session.getBasicRemote().sendText(registerMessage);
        } catch (Exception e) {
            System.err.println("❌ 登録メッセージ送信失敗: " + e.getMessage());
        }

        layoutManager = new VirtualLayoutManager("PC-A");
        layoutManager.addScreen(new VirtualLayoutManager.ScreenInfo("PC-B", -3840, -555, 4480, 1440));
        int macWidth = 1920;// PC-A（Mac）を PC-B の真下中央に配置
        //int macWidth = 3000;
        int macHeight = 1200;
        int centerX = -3840 + (4480 - macWidth) / 2;// PC-B の中心から Mac を左右中央に揃える
        //int centerX = -3840;//左端
        int macY = -555 + 1440;
        layoutManager.addScreen(new VirtualLayoutManager.ScreenInfo("PC-A", centerX, macY, macWidth, macHeight));
        System.out.println("🧭 PC-A 配置: x=" + centerX + ", y=" + macY);

        VirtualLayoutManager.ScreenInfo pcA = layoutManager.getScreenById("PC-A");// ✅ 初期座標を PC-A の中央にする
        if (pcA != null) {
            currentX = pcA.x + pcA.width / 2;
            currentY = pcA.y + pcA.height / 2;
            warpToVirtual(currentX, currentY);
        }
        startMouseMonitor(); // ⬇️ PC-B のマウス監視スレッド起動
    }

    @OnMessage
    public void onMessage(String message) {
      //System.out.println("📩 受信: " + message);TODO
        try {
          // 受信したJSON文字列をパースする
            //Map<String, Object> data = objectMapper.readValue(message, Map.class);
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});
            String type = (String) data.get("type");
            if ("move".equals(type)) {
                int deltaX = (int) data.get("deltaX");
                int deltaY = (int) data.get("deltaY");
              //System.out.println("📥 移動指示受信: dx=" + deltaX + ", dy=" + deltaY);todo
                currentX += deltaX;// 仮想座標更新
                currentY += deltaY;
                checkAndWarpIfNeeded();// 画面端チェックしてワープ判定
                VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
                //Point physical = CoordinateConverter.virtualToPhysical(currentX, currentY, layoutManager);
                if (currentScreen != null && "PC-B".equals(currentScreen.id)) {
                    Point physical = CoordinateConverter.virtualToPhysical(currentX, currentY, layoutManager);
                    if (physical != null) {
                        Point currentPhysical = MouseInfo.getPointerInfo().getLocation();
                        if (!currentPhysical.equals(physical)) {
                            System.out.printf("🎯 仮想座標: (%d, %d), 現在スクリーン: %s [%d ~ %d]\n",
                                currentX, currentY, currentScreen.id, currentScreen.x, currentScreen.x + currentScreen.width);
                            MouseController.warpTo(physical.x, physical.y);
                        }
                    }
                }
            }
            if ("click".equals(type)) {
                int button = (int) data.get("button");
                VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
                if (currentScreen != null && "PC-B".equals(currentScreen.id)) {
                    MouseController.click(button);
                }
            }
            if ("warp".equals(type)) {
                String direction = (String) data.get("direction");

                if (direction != null) {
                    VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
                    System.out.println("📡 warp指示受信: direction=" + direction);
                    System.out.printf("🌍 現在仮想座標: (%d, %d)\n", currentX, currentY);
                    if (currentScreen != null) {
                        System.out.println("✅ warp先スクリーン: " + currentScreen.id);
                        warpToNextScreen(direction, currentScreen); // ここに breakPoint や log を置くと良い
                    } else {
                        System.out.println("❌ currentScreen が null（座標不一致）");
                    }
                    if (currentScreen != null) {
                        warpToNextScreen(direction, currentScreen);
                    }
                }else {
                    System.out.println("⚠️ direction が null");
                }
            }
            if ("scroll".equals(type)) {
                double deltaY = Double.parseDouble(data.get("deltaY").toString());
                VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
                if (currentScreen != null && "PC-B".equals(currentScreen.id)) {
                    MouseController.scroll(deltaY);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ パースエラー: " + e.getMessage());
        }
    }

    private void checkAndWarpIfNeeded() {
        
        if (suppressWarpCheck) return; // ✅ warp直後なら無視
        VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
        
        if (currentScreen == null) return;
        // 🧪 デバッグログ出力（仮想座標 & スクリーン範囲）
        System.out.printf("🧪 ワープチェック時: (%d,%d), 条件: y >= %d\n",
            currentX, currentY, currentScreen.y + currentScreen.height - 10);
        System.out.printf("🧪 仮想座標: (%d, %d), 現在スクリーン: %s [%d ~ %d, %d ~ %d]\n",
            currentX, currentY, currentScreen.id,
            currentScreen.x, currentScreen.x + currentScreen.width,
            currentScreen.y, currentScreen.y + currentScreen.height);
        if ("PC-A".equals(currentScreen.id)) {
            if (currentX <= currentScreen.x && canWarpNow()) {
                warpToNextScreen("left", currentScreen);
            } else if (currentX >= currentScreen.x + currentScreen.width - 1 && canWarpNow()) {
                warpToNextScreen("right", currentScreen);
            }
        } else if ("PC-B".equals(currentScreen.id)) {
            if (currentY >= currentScreen.y + currentScreen.height - 10 && canWarpNow()) {
                warpToNextScreen("down", currentScreen);
            }
        }
    }

    private void warpToNextScreen(String direction, VirtualLayoutManager.ScreenInfo fromScreen) {
        VirtualLayoutManager.ScreenInfo currentScreen = layoutManager.findScreenByPoint(currentX, currentY);
        System.out.println("🚀 warpToNextScreen() 呼び出し direction=" + direction + ", currentScreen=" + currentScreen.id);
        
        if ("PC-A".equals(fromScreen.id)) {
            VirtualLayoutManager.ScreenInfo pcB = layoutManager.getScreenById("PC-B");
            if (pcB != null) {
                if ("left".equals(direction)) {
                    currentX = pcB.x + pcB.width / 4;
                } else if ("right".equals(direction)) {
                    currentX = pcB.x + (pcB.width * 3 / 4);
                }
                currentY = pcB.y + pcB.height / 2;
                warpToVirtual(currentX, currentY);
            }
        }
        if ("PC-B".equals(fromScreen.id) && "down".equals(direction)) {
            VirtualLayoutManager.ScreenInfo pcA = layoutManager.getScreenById("PC-A");
            if (pcA != null) {
                currentX = pcA.x + pcA.width / 2;
                currentY = pcA.y + pcA.height / 2;
                warpToVirtual(currentX, currentY);
            }
        }
    }

    private Point lastWarpedVirtual = null;
    private void warpToVirtual(int virtualX, int virtualY) {
        System.out.printf("🧲 warpToVirtual() called with (%d, %d)\n", virtualX, virtualY);
        new Exception("🔍 warpToVirtual 呼び出しスタックトレース").printStackTrace();
        if (lastWarpedVirtual != null &&
            lastWarpedVirtual.x == virtualX &&
            lastWarpedVirtual.y == virtualY) {
            return; // 同じ仮想座標 → 無視
        }
        lastWarpedVirtual = new Point(virtualX, virtualY);
        
        Point physical = CoordinateConverter.virtualToPhysical(virtualX, virtualY, layoutManager);
        System.out.printf("🧲 仮想→物理: (%d,%d) → (%d,%d)\n", virtualX, virtualY, physical.x, physical.y);
        Point backToVirtual = CoordinateConverter.physicalToVirtual(physical.x, physical.y, layoutManager);
        if (backToVirtual != null) {
            System.out.printf("🔄 物理→仮想: (%d,%d) → (%d,%d)\n", physical.x, physical.y, backToVirtual.x, backToVirtual.y);
        } else {
            System.out.println("⚠️ 逆変換できませんでした");
        }
        if (physical != null) {
            suppressWarpCheck = true; // ✅ warp後の監視抑制
            System.out.printf("🧲 warpTo(): 仮想→実 (%d,%d) → (%d,%d)\n", virtualX, virtualY, physical.x, physical.y);
            MouseController.warpTo(physical.x, physical.y);
            // ✅ 0.5秒後に抑制解除（Timerで非同期）
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() {
                    suppressWarpCheck = false;
                }
        }, 500);
        }
    }

    private boolean canWarpNow() {
        long now = System.currentTimeMillis();
        if (now - lastWarpTime >= WARP_COOLDOWN_MS) {
            lastWarpTime = now;
            return true;
        }
        return false;
    }

    private void startMouseMonitor() {
        new Thread(() -> {
            try {
                while (true) {
                    PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                    if (pointerInfo == null) continue;
                    Point real = pointerInfo.getLocation();
                    Point virtual = CoordinateConverter.physicalToVirtual(real.x, real.y, layoutManager);
                    if (virtual == null) continue;
                    VirtualLayoutManager.ScreenInfo screen = layoutManager.findScreenByPoint(virtual.x, virtual.y);
                    if (screen != null && "PC-B".equals(screen.id)) {
                        int bottom = screen.y + screen.height;
                        if (virtual.y >= bottom - 5 && canWarpNow()) {
                            sendWarpMessage("down");
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                System.err.println("❌ モニタースレッドエラー: " + e.getMessage());
            }
        }, "MouseMonitorThread").start();
    }

    private void sendWarpMessage(String direction) {
        if (!canWarpNow()) return;
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "type", "warp",
                "from", "PC-B",
                "to", "PC-A",
                "direction", direction
            ));
            for (Session session : SessionManager.getSessions()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            System.err.println("❌ warp送信エラー: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("❌ 切断: " + reason);
        SessionManager.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("🔥 エラー: " + throwable.getMessage());
    }
}

package com.example.client;

//import com.sun.jna.Native;
//import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.POINT;

//import javafx.scene.effect.Light.Point;

//import javafx.scene.shape.Rectangle;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;

import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Point;


/**
 * マウスを制御するユーティリティクラス
 */

public class MouseController {

    private static double getWindowsScaleFactor() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        AffineTransform transform = gc.getDefaultTransform();
        return transform.getScaleX(); // 通常 1.0, 1.5, 2.0 など
    }
    

    public static void moveBy(int deltaX, int deltaY) {
        // Windowsのスケーリング係数（例：150% = 1.5）
        double scaleFactor = getWindowsScaleFactor()*4.0;//todoマジックナンバー
        POINT point = new POINT();
        User32.INSTANCE.GetCursorPos(point); // 現在位置を取得
        //int newX = point.x + deltaX;
        //int newY = point.y + deltaY;
        int newX = point.x + (int)(deltaX * scaleFactor);
        int newY = point.y + (int)(deltaY * scaleFactor);
        User32.INSTANCE.SetCursorPos(newX, newY); // 新しい座標に移動
    }
    public static void click(int button) {
        try {
            // JNAのRobotクラスを使ったクリック方法
            java.awt.Robot robot = new java.awt.Robot();
            int awtButton = java.awt.event.InputEvent.BUTTON1_DOWN_MASK; // デフォルト左クリック
    
            if (button == 2) {
                awtButton = java.awt.event.InputEvent.BUTTON3_DOWN_MASK; // 右クリック
            } else if (button == 1) {
                awtButton = java.awt.event.InputEvent.BUTTON2_DOWN_MASK; // 中クリック
            }
    
            robot.mousePress(awtButton);
            robot.mouseRelease(awtButton);
            System.out.println("✅ クリック実行完了");
        } catch (Exception e) {
            System.err.println("❌ クリックエラー: " + e.getMessage());
        }
    }
    public static void warpToEdge(String direction) {
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            GraphicsDevice targetScreen = screens[0]; // シンプルに左側画面を対象
    
            Rectangle bounds = targetScreen.getDefaultConfiguration().getBounds();
            int targetX = bounds.x;
            int targetY = bounds.y + bounds.height / 2;
    
            if ("right".equals(direction)) {
                targetX = bounds.x + bounds.width - 1;
            } else if ("up".equals(direction)) {
                targetX = bounds.x + bounds.width / 2;
                targetY = bounds.y;
            } else if ("down".equals(direction)) {
                targetX = bounds.x + bounds.width / 2;
                targetY = bounds.y + bounds.height - 1;
            }
    
            java.awt.Robot robot = new java.awt.Robot();
            robot.mouseMove(targetX, targetY);
    
            System.out.println("✅ ワープ実行完了: (" + targetX + "," + targetY + ")");
        } catch (Exception e) {
            System.err.println("❌ ワープエラー: " + e.getMessage());
        }
        }
    public static void scroll(double deltaY) {
        try {
            java.awt.Robot robot = new java.awt.Robot();
    
            //int amount = (int) Math.round(deltaY / 100.0); // スクロール単位に変換
            int amount = (int) Math.signum(deltaY) * Math.max(1, (int) Math.abs(deltaY / 40.0));
            robot.mouseWheel(amount);
    
            System.out.println("✅ スクロール実行: " + amount);
        } catch (Exception e) {
            System.err.println("❌ スクロールエラー: " + e.getMessage());
        }
    }

    
    public static void warpTo(int x, int y) {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;
        Point current = pointerInfo.getLocation();

        // すでにほぼ同じ座標ならワープしない（1px以内の誤差は無視）
        if (Math.abs(current.x - x) <= 1 && Math.abs(current.y - y) <= 1) {
            return;
        }

        System.out.println("🖱️ warpTo(): 実行 " + x + ", " + y);
        try {
            java.awt.Robot robot = new java.awt.Robot();
            robot.mouseMove(x, y);
            System.out.println("✅ マウスを座標 (" + x + ", " + y + ") にワープしました");
        } catch (Exception e) {
            System.err.println("❌ マウスワープエラー: " + e.getMessage());
        }
    }

    
    

    
}


/* 
 * Windows OS専用
 * Windows APIのSetCursorPos
 * JavaFX（GUI用）
 * AWT（マウス制御用）
*/
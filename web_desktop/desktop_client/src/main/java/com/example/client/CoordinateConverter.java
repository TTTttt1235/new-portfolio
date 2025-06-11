package com.example.client;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

public class CoordinateConverter {

    /**
     * 仮想座標を物理座標に変換
     */
    public static Point virtualToPhysical(int virtualX, int virtualY, VirtualLayoutManager layoutManager) {
        VirtualLayoutManager.ScreenInfo screen = layoutManager.findScreenByPoint(virtualX, virtualY);
        if (screen == null) return null;

        int offsetX = virtualX - screen.x;
        int offsetY = virtualY - screen.y;

        if (screen.id.equals("PC-B")) {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice device : screens) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                // PC-B はこの物理スクリーンに対応していると仮定して bounds だけ使う
                return new Point(bounds.x + offsetX, bounds.y + offsetY);
            }
        }

        // PC-Aなど他のスクリーンは仮に中央(0,0)原点と仮定する
        return new Point(offsetX, offsetY);
    }

    /**
     * 物理座標を仮想座標に変換
     */
    public static Point physicalToVirtual(int physicalX, int physicalY, VirtualLayoutManager layoutManager) {
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice device : screens) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            if (bounds.contains(physicalX, physicalY)) {
                // 仮想レイアウト上の PC-B に対応するスクリーンを検索
                VirtualLayoutManager.ScreenInfo pcB = layoutManager.getScreenById("PC-B");
                if (pcB != null) {
                    int offsetX = physicalX - bounds.x;
                    int offsetY = physicalY - bounds.y;
                    return new Point(pcB.x + offsetX, pcB.y + offsetY);
                }
            }
        }
        return null;
    }
}

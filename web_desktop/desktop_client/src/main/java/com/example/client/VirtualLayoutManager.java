package com.example.client;

import java.util.ArrayList;
import java.util.List;

public class VirtualLayoutManager {

    public ScreenInfo getScreenById(String id) {
        for (ScreenInfo screen : screens) {
            if (screen.id.equals(id)) {
                return screen;
            }
        }
        return null;
    }
    

    public static class ScreenInfo {
        public String id;    // 端末/画面ID (例: PC-A, PC-B-1)
        public int x;        // 仮想座標X
        public int y;        // 仮想座標Y
        public int width;
        public int height;

        public ScreenInfo(String id, int x, int y, int width, int height) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        //(px,py)がスクリーン内にあるか判定
        public boolean contains(int px, int py) {
            return (px >= x && px <= x + width && py >= y && py <= y + height);
        }
    }

    private List<ScreenInfo> screens = new ArrayList<>();
    private String selfId;//マネージャを生成した自身の端末のスクリーンID

    //インスタンス生成時に自身のスクリーンIDを設定
    public VirtualLayoutManager(String selfId) {
        this.selfId = selfId;
    }

    public void addScreen(ScreenInfo screen) {
        screens.add(screen);
    }

    //登録済みのすべてのスクリーンをループ。マウスポインタの座標から現在どのスクリーンに居るか判定
    public ScreenInfo findScreenByPoint(int px, int py) {
        for (ScreenInfo screen : screens) {
            if (screen.contains(px, py)) {
                return screen;
            }
        }
        System.out.println("🕵️ 座標 (" + px + "," + py + ") はどのスクリーンにも該当しない");
        return null;
    }

    // ワープ先
    public ScreenInfo findNextScreen(String direction, int px, int py) {
        ScreenInfo currentScreen = findScreenByPoint(px, py);
        if (currentScreen == null) return null;
    
        // PC-A から出るとき
        if ("PC-A".equals(currentScreen.id)) {
            if ("left".equals(direction)) {
                // 左に出たら 4Kモニター（PC-B-1）へ
                return getScreenById("PC-B-1");
            } else if ("right".equals(direction)) {
                // 右に出たら FHDモニター（PC-B-2）へ
                return getScreenById("PC-B-2");
            }
        }
    
        // PC-B 側から Mac へ戻る
        if ("PC-B-1".equals(currentScreen.id) || "PC-B-2".equals(currentScreen.id)) {
            if ("down".equals(direction)) {
                return getScreenById("PC-A");
            }
        }
    
        return null;
    }
    

    public String getSelfId() {
        return selfId;
    }
}

/*
 *  想定
 *  +--------------------+ +------------------+
 *  | PC-B-1 (4K)        | | PC-B-2 (FHD)     |
 *  | 3840x2160          | | 1920x1080        |
 *  +--------------------+ +------------------+
 *                  ↑
 *                  ワープ
 *                  ↓
 *  +--------------------------+
 *  | PC-A (Mac) 1710x1112     |
 *  +--------------------------+
 *
 * 
 * 
 */
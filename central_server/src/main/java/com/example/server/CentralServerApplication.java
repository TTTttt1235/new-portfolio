package com.example.server;

import org.glassfish.tyrus.server.Server;

import io.github.cdimascio.dotenv.Dotenv;

public class CentralServerApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load(); // .envを読み込む
        String host = dotenv.get("SERVER_HOST", "0.0.0.0"); // デフォルト値あり
        int port = Integer.parseInt(dotenv.get("SERVER_PORT", "8080"));
        String wsUrl = String.format("ws://%s:%d/ws/mouse", host, port);
        Server server = new Server(host, port, "/ws", null, MouseRelayServer.class);
        System.out.println("🌐 中央サーバー起動中: " + wsUrl);
        try {
            server.start();
            Thread.currentThread().join(); // 永続化
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//mvn exec:java -Dexec.mainClass=com.example.server.CentralServerApplication

package com.example.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/mouse")
public class MouseRelayServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final Map<Session, String> reverseMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("🟢 接続開始: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session sender) {
        System.out.println("📨 受信: " + message);  // デバッグ用
        try {
            Map<String, Object> data = mapper.readValue(message, new TypeReference<>() {});
            String from = (String) data.get("from");
            String to = (String) data.get("to");
            
            if (from != null) {
                sessionMap.put(from, sender);
                reverseMap.put(sender, from);
            }
            System.out.println("🗺 登録状態: " + sessionMap.keySet());//todo
            if (to != null && sessionMap.containsKey(to)) {
                sessionMap.get(to).getBasicRemote().sendText(message);
                System.out.printf("📤 中継: %s → %s\n", from, to);
            } else if (to != null) {
                System.out.println("⚠️ 宛先未接続: " + to);
            }
            /*
            if (!"PC-B".equals(to)) return;
            if (from != null) {
                sessionMap.put(from, sender);
                reverseMap.put(sender, from);
            }

            if (to != null && sessionMap.containsKey(to)) {
                sessionMap.get(to).getBasicRemote().sendText(message);
                System.out.printf("📤 中継: %s → %s\n", from, to);
            } else {
                System.out.println("⚠️ 宛先未接続: " + to);
            } */

        } catch (Exception e) {
            System.err.println("❌ JSON処理エラー: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        String id = reverseMap.remove(session);
        if (id != null) {
            sessionMap.remove(id);
            System.out.println("🔴 切断: " + id);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("🔥 エラー: " + throwable.getMessage());
    }
}

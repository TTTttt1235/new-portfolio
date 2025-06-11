package com.example.backend.websocket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class MouseWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    //WebSocket経由でクライアントからテキストメッセージを受け取った時に呼び出し
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("📩 受信メッセージ: " + message.getPayload());//ログ

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {//接続中か確認
                s.sendMessage(message); // 全接続クライアントに中継（ブロードキャスト）
            }
        }
    }

    //接続したとき
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("✅ クライアント接続: " + session.getId());//ログ
    }

    //切断されたとき
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
        System.out.println("❌ クライアント切断: " + session.getId());//ログ
    }
}
/*
 * List：接続中のセッションを管理
 * CopyOnWriteArrayList：スレッドセーフなリスト
 * →リストに対する変更操作が内部リストのコピーに行われる
 * →ConcurrentModificationExceptionをスローしない
 * 
 * 
 * 
 */
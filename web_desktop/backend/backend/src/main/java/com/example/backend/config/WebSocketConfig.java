package com.example.backend.config;

import com.example.backend.websocket.MouseWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration //springの設定クラス、springboot起動時に読み込み
@EnableWebSocket //websocket機能有効化
public class WebSocketConfig implements WebSocketConfigurer {

    //websocketHandlerをエンドポイントに紐付け
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MouseWebSocketHandler(), "/ws/mouse")
                .setAllowedOrigins("*"); //ローカル以外なら制限
    }
}

/* registry：SpringのWebsocketエンドポイント登録用オブジェクト
 * 
 */
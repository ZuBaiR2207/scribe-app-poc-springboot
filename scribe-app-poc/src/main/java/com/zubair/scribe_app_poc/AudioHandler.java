package com.zubair.scribe_app_poc;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * AudioHandler
 */
public class AudioHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("{\"type\":\"hello\"}"));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        System.out.println("received bytes: " + message.getPayloadLength());
    }
}

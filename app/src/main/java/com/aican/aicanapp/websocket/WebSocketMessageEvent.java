package com.aican.aicanapp.websocket;

public class WebSocketMessageEvent {
    private final String message;

    public WebSocketMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
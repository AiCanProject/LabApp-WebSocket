package com.aican.aicanapp.websocket

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

object WebSocketManager {
    private var webSocketClient: WebSocketClient? = null
    private var messageListener: ((String) -> Unit)? = null
    private var errorListener: ((Exception) -> Unit)? = null

    fun initializeWebSocket(uri: URI) {
        if (webSocketClient == null) {
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    // WebSocket connection opened
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    // WebSocket connection closed
                }

                override fun onMessage(message: String?) {
                    // Handle incoming message
                    message?.let {
                        messageListener?.invoke(it)
                    }
                }

                override fun onError(ex: Exception?) {
                    // Handle error
                    ex?.let {
                        errorListener?.invoke(it)
                    }
                }
            }
            webSocketClient?.connect()
        }
    }

    fun setMessageListener(listener: (String) -> Unit) {
        messageListener = listener
    }

    fun sendMessage(message: String) {
        if (webSocketClient?.isOpen == true) {
            webSocketClient?.send(message)
        } else {
            // Handle the case when WebSocket is not connected
            errorListener?.invoke(Exception("WebSocket is not connected"))
        }
    }

    fun setErrorListener(listener: (Exception) -> Unit) {
        errorListener = listener
    }


    fun disconnect() {
        webSocketClient?.close()
    }
}

